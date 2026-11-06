# Step 16 — Profile selector (Version 2 begins)

## Goal

After choosing a destination and before the route is requested, let the user pick a **navigation profile**:

- **Focus** — direct navigation, no POI suggestions (the V1 behaviour).
- **Tour** — exploration; POI discovery arrives in steps 17–20, so for now Tour **starts navigation exactly like
  Focus**, but the profile is stored in the session and exposed so later modules can branch on it.

## New state, carried in the session

A `ProfileSelection` state is inserted between `DestinationSelected` and `RouteLoading`. The chosen
`NavigationProfile` (`:core:model`, `FOCUS` / `TOUR`) then rides in every trip state from `RouteLoading` on, so
the Navigation Engine and the Tour modules read it **from the session, never from the UI**. See
[ARCHITECTURE.md → Navigation session state machine](../../ARCHITECTURE.md#navigation-session-state-machine).

Transitions (pure `reduce`, unit-tested):

| From | Event | To |
|---|---|---|
| `DestinationSelected` | `ChooseProfile` | `ProfileSelection(destination)` |
| `ProfileSelection` | `SelectProfile(FOCUS\|TOUR)` | `RouteLoading(destination, profile)` |
| `ProfileSelection` | `StartSearch` (back) | `Searching` |
| `DestinationSelected` | `RequestRoute` (retry / restore) | `RouteLoading(destination, profile)` — profile kept |

`RequestRoute` keeps its V1 meaning ("load the route") for retries and restored routes, so the whole V1 flow and
its tests are untouched; the new forward path is `ChooseProfile` → `SelectProfile`. The profile field defaults to
`FOCUS`, so every state built without one (V1 code, the route cache, tests) stays Focus.

## Folder tree (new / changed)

```
core/model/
  HapticEvent.kt              + PROFILE_SELECTED
  HapticPattern.kt            + PROFILE_SELECTED pattern ("tk-TUM" confirm)
  NavigationProfile.kt        (ya existía: FOCUS, TOUR)
core/navigation/
  NavigationSession.kt        + ProfileSelection; + profile field on the trip states; + navigationProfile
  NavigationEvent.kt          + ChooseProfile, SelectProfile; + profile on RestoreRoute
  NavigationReducer.kt        new transitions + profile threaded through
  NavigationSessionStore.kt   label for ProfileSelection
  ProfilePreference.kt        NEW — persistence contract (pure Kotlin)
core/designsystem/
  molecules/ProfileOption.kt                 NEW — the selectable option (atomic: molecule)
  res/drawable/ic_profile_focus.xml          NEW — target icon
  res/drawable/ic_profile_tour.xml           NEW — compass icon
  res/values{,-es}/strings.xml               + sample strings
  screenshotTest/.../ProfileScreenshots.kt   NEW
feature/navigation-ui/
  ProfileSelectorScreen.kt                   NEW — screen (screens level)
  ProfileSelectorViewModel.kt                NEW
  DataStoreProfilePreference.kt              NEW — DataStore impl + Hilt @Binds
  RouteSummaryViewModel.kt                   handles the RouteLoading entry after profile selection
  res/values{,-es}/strings.xml               + selector strings
  screenshotTest/.../ProfileSelectorScreenshots.kt  NEW
app/
  presentation/navigation/Screen.kt          + ProfileSelection route
  presentation/navigation/HaptiMapNavHost.kt destino → selector → resumen
  presentation/debug/*                        new states/events in the debug tool
  res/values{,-es}/strings.xml               + debug labels
gradle/libs.versions.toml                     + androidx.datastore:datastore-preferences
feature/navigation-ui/build.gradle.kts        + datastore dependency
```

## Design decisions

- **Profile in the session state, not a parallel flow.** Each trip state gains a `profile` field (default `FOCUS`,
  appended last) rather than a side `StateFlow`. It is literally carried through the session, and because it
  defaults to Focus, no V1 construction or test had to change — only the new forward transitions were added.
- **Reuse before building.** The screen is a `RoundScreenTemplate` + `HeadingText`; the only new component is the
  `ProfileOption` molecule, built on the same Wear M3 `Button` as `PlaceRow`. Colours, typography, spacing, shapes
  and icon sizes are the shared tokens, so a change propagates everywhere. See [DESIGN_SYSTEM §21](../../DESIGN_SYSTEM.md).
- **One tap.** The last profile (DataStore) is preselected and highlighted; tapping any option chooses it, so the
  common case is a single tap, and the crown reaches either option.
- **Haptic through a semantic event.** The confirmation is `HapticEvent.PROFILE_SELECTED` played via `HapticEngine`
  (the app-scope `follow` loop plays it, as the tutorial does); the UI never touches the vibrator. The pattern is a
  short "tk-TUM", unlike any navigation cue, lowest priority (it never shares the cue stream).
- **Persistence split.** The `ProfilePreference` contract is pure Kotlin (`:core:navigation`) so ViewModels and
  tests depend on it; the DataStore implementation is Android (`:feature:navigation-ui`), bound with Hilt, mirroring
  the `TutorialStore` pattern. A corrupt/unknown stored value falls back to `FOCUS` without throwing.
- **Keys unchanged.** No key is touched: Maps still comes from `local.properties`/BuildConfig, DeepSeek stays empty
  in release (step 15A). Nothing in this step uses AI yet.

## Strings (new)

`:feature:navigation-ui` — `profile_title`, `profile_focus_title`, `profile_focus_subtitle`, `profile_tour_title`,
`profile_tour_subtitle` (EN + ES). `:core:designsystem` — `sample_profile_*`. `:feature:onboarding` —
`haptic_profile_selected`. `:app` — `event_choose_profile`, `event_select_profile`, `session_profile_selection`.
All present in `values/` and `values-es/`.

## How this prepares steps 17–20 (Tour mode)

- The POI states the model foresees (`PoiDetected`, `PoiSuggestion`, `Detour`) will branch from `NavigationActive`
  and carry the `Route`; now they can also read `navigationProfile` from the session to exist **only in Tour**.
- The engine/service already see the profile in the session they follow (no UI dependency), so POI detection can be
  gated on `TOUR` without new plumbing.
- `HapticEvent.POI_SUGGESTION` already exists; `PROFILE_SELECTED` adds the confirmation vocabulary.

## Verification (2026-11-06)

`./gradlew testDebugUnitTest validateDebugScreenshotTest lintDebug :core:model:test :core:navigation:test` →
**BUILD SUCCESSFUL**.

- **Unit tests:** 290 pass (up from 284). New: `NavigationReducerTest` — the `ChooseProfile`/`SelectProfile`/back
  transitions, the profile travelling the whole trip, a restored route keeping its profile, and retry keeping it
  (10 tests); `ProfileSelectorViewModelTest` — entering moves the session to `ProfileSelection`, the last profile is
  preselected, and choosing Tour saves it, fires `PROFILE_SELECTED` and loads the route with the profile (3 tests).
- **Screenshots:** `validateDebugScreenshotTest` green; +36 references (`ProfileOptions` 12, `ProfileSelectorFocus` /
  `ProfileSelectorTour` 24). No existing reference changed.
- **Lint:** 0 errors in every module; 0 warnings outside `:app`; `:app` 3 (the step 15A baseline — DataStore pinned
  to 1.3.0 so it adds no version notice).

## Emulator run (2026-11-06)

On the windowed `Wear_OS_Large_Round` AVD (debug build, fake APIs), logs under `HaptiSession` / `HaptiHaptics`:

- **Selector appears in place.** Search → select "Store Calle de las Huertas" → log `DestinationSelected +
  ChooseProfile -> ProfileSelection`; the screen shows "How do you want to go?" with Focus and Tour, both reachable.
- **Choice carried in the session.** Tapping Tour logged `ProfileSelection + SelectProfile(profile=TOUR) ->
  RouteLoading(…, TOUR)`, then `RouteLoaded -> RouteReady`; the route summary loaded normally (Tour == Focus for
  now), so Tour reaches navigation exactly like Focus.
- **Haptic via the semantic event.** `HaptiHaptics: play event=PROFILE_SELECTED pattern=40/50/120ms amp=120/0/255`
  fired on the tap — through the engine, not the vibrator directly.
- **Persistence.** The choice is written to DataStore and the selector is shown again on the next search; the
  preselection of the saved profile is covered by `ProfileSelectorViewModelTest`.

TalkBack/rotary: the options are standard Wear `Button`s in a `RoundScreenTemplate`, so crown scroll and TalkBack
work as on the other list screens; the selected option carries a `selected` semantic.

## Pending

- Tour behaviour itself (POI discovery): steps 17–20.
- A restored route (process death) currently defaults to `FOCUS`; storing the profile with the cached route is a
  small future refinement (no functional effect while Tour == Focus).
- Physical-watch feel of the `PROFILE_SELECTED` confirmation (step 14 checklist).
