# HaptiMap Watch — Design System

The design system lives in its own Gradle module, **`:core:designsystem`**. Any feature module
can depend on it, and it depends on no domain module (see §1, "What a component may accept"). Every color,
type style, spacing, shape, size and piece of text the user sees is defined here or in
`res/values*`. The haptic language is the one exception: it lives in `:core:model` (see §2). **Nothing else in the app holds raw design values.**

## 1. Folder structure (Atomic Design)

```
core/designsystem/src/main/
├── java/com/xanderjs/haptimap/designsystem/
│   ├── tokens/                     ← raw design decisions (the ONLY place with literal values)
│   │   ├── ColorTokens.kt          BasePalette (internal) + HaptiColors (semantic roles)
│   │   ├── Typography.kt           HaptiTypography: distance, distanceCompact (step 13), title, body, label, caption
│   │   ├── Spacing.kt              HaptiSpacing: xs 2 · s 4 · m 8 · l 12 · xl 16 dp
│   │   ├── Shape.kt                HaptiShapes: small 8 · medium 16 · large 24 dp · full (pill)
│   │   └── Dimens.kt               HaptiDimens: minTouchTarget 48 dp, icon sizes, borderWidth, waveform sizes (24 dp; compact 36 × 14 dp), clockBandSafeTop 8 dp, centeredContentWidthFraction 0.8, stepDot 6 dp (current 14 dp)
│   ├── theme/                      ← turns tokens into a theme (+ AppLanguage.kt: Context.appLanguage())
│   │   ├── ThemeMode.kt            SYSTEM · LIGHT · DARK
│   │   ├── ColorSchemes.kt         LightHaptiColors, DarkHaptiColors, mapping to Wear M3 ColorScheme
│   │   └── HaptiMapTheme.kt        HaptiMapTheme(themeMode) { } + accessor object HaptiMapTheme.*
│   ├── atoms/                      ← smallest UI pieces; only use tokens
│   │   ├── HaptiButton.kt          primary / secondary; full width, label centered; optional icon and maxLines (step 11)
│   │   ├── HaptiIcon.kt            Small / Medium / Large from dimens
│   │   ├── HaptiTimeText.kt        curved clock in onBackground; offline label + LocalOffline; fixed 10:10 in previews
│   │   ├── HeadingText.kt          screen heading: title style, centered, 2 lines, centeredContentWidthFraction wide, TalkBack heading
│   │   ├── BodyText.kt             explanatory paragraph: body style, centered, no line limit, centeredContentWidthFraction wide (step 13)
│   │   ├── StepIndicator.kt        step n of total in a linear flow: dots, the current one wider; TalkBack "Step 2 of 6" (step 13)
│   │   ├── HaptiEdgeButton.kt      the screen's primary action on the bottom edge (Wear M3 EdgeButton), for GlanceScreenTemplate(edgeButton) (step 13)
│   │   ├── DirectionArrow.kt       LEFT / RIGHT / STRAIGHT / ARRIVE, tinted navigationArrow
│   │   ├── DistanceText.kt         formatDistance(): "850 m", "1.2 km" / "1,2 km"; DistanceText (large)
│   │   ├── CategoryChip.kt         selectable search category: icon + label, full-width pill button
│   │   ├── VoiceSearchButton.kt    primary HaptiButton with the microphone (step 11)
│   │   ├── ActiveRouteButton.kt    back to the route in progress: secondary, one line, arrow or pin (step 11)
│   │   ├── RouteStatsText.kt       "1.2 km · 15 min"; formatDuration()
│   │   ├── StatusTone.kt           Default / Muted / Warning / Error + StatusTone.color
│   │   ├── NoticeLabel.kt          short in-content notice: text + StatusTone in a surface pill
│   │   ├── InstructionText.kt      the maneuver instruction while navigating: body, centered, up to 3 lines
│   │   └── HapticWaveform.kt       a haptic pattern drawn: width = time, height = strength, pill per pulse; WaveformSegment
│   ├── molecules/                  ← a few atoms working as one unit
│   │   ├── ManeuverIndicator.kt    DirectionArrow + DistanceText, read as one phrase by TalkBack
│   │   ├── PlaceRow.kt             PlaceRowItem: name + "category · distance", one touch target
│   │   ├── HapticPatternCard.kt    name + HapticWaveform + summary; the whole card plays it (step 09 bench, step 13 tutorial)
│   │   └── HapticCueBadge.kt       the pattern just played: small waveform + name in a pill (debug indicator)
│   ├── organisms/                  ← full sections of a screen
│   │   ├── PlaceResultsList.kt     search results: a PlaceRow per place
│   │   ├── StatusMessage.kt        text + StatusTone + optional spinner
│   │   └── ConfirmDialog.kt        full-screen question with two text buttons; ConfirmDialogContent for previews
│   ├── templates/                  ← screen skeletons, no real data
│   │   ├── RoundScreenTemplate.kt  ScreenScaffold + TimeText + safe round padding + scrolling list (optional listState)
│   │   ├── GlanceScreenTemplate.kt glance + details by priority in a measured first view; actions below the fold; optional edgeButton pinned at the bottom (step 13)
│   │   └── CenteredScreenTemplate.kt  ScreenScaffold + TimeText + one short block centered, no scroll
│   └── preview/                    ← preview tooling (not part of the hierarchy)
│       ├── HaptiPreviews.kt        multipreview: {small round, large round, square} × {light, dark} × {en, es}
│       └── PreviewSurface.kt       theme + background wrapper for previews
└── res/
    ├── values/strings.xml          English (default)
    ├── values-es/strings.xml       Spanish
    └── drawable/                   ic_turn_left, ic_turn_right, ic_straight, ic_arrive, ic_off_route, ic_play, ic_mic, ic_category_* (7)
```

Screens built from templates live in the feature modules (`:feature:search`,
`:feature:navigation-ui`, `:feature:onboarding`). The temporary catalog lives in
`app/.../presentation/catalog/DesignSystemCatalog.kt`.

### What a component may accept

**Design-system components accept only UI values: strings already resolved, numbers, durations,
design-system enums and data classes (`ArrowDirection`, `StatusTone`, `PlaceRowItem`,
`GuidanceUi` in `:feature:navigation-ui`) and callbacks. Never domain types (`Place`, `Route`, `Maneuver`,
`PlaceSearchError`…).** The mapping from the domain to these values lives in the screen or feature
layer that owns the domain, next to the screen that uses it:

| Mapping | Where |
|---|---|
| `PlaceCategory.label` / `.icon`, `Place.toRowItem()`, `SearchStatus` → `SearchStatusMessage`, `SearchQuery.title()` | `:feature:search` `SearchUi.kt` (in `:app` until step 11) |
| `HapticPattern.toSegments()`, `HapticEvent.label` / `.tint`, `hapticScaleMs`, `playbackProgress()` | `:app` `presentation/haptics/HapticUi.kt` |
| `Maneuver` → `ArrowDirection`, `Route.toSummary()`, `RouteStatus` → `RouteStatusMessage`, offline route → `RouteOfflineNotice`, `NavigationProgress.toGuidance()` → `GuidanceUi`, `navigationNotice()` → `NavigationNoticeLabel` | `:feature:navigation-ui` `RouteUi.kt` |

The build enforces it: `:core:designsystem` does not depend on `:core:model`, so a domain import
in a component does not compile. Why: the same component serves any data that looks the same
(POI suggestions in version 2 can reuse `PlaceRow`), a new error type needs no new component variant,
logic such as choosing the first maneuver is tested as plain Kotlin (`Route.firstManeuver()`), and a
domain change never rebuilds the design system. Domain copy (error texts, category names) lives in
the module of the screen; the design system keeps only its own texts and generic `sample_*` texts
for previews.

### What belongs at each level

| Level | Contains | May use | Example |
|---|---|---|---|
| tokens | Raw values: hex colors, dp, sp, ms, amplitudes | nothing | `BasePalette.Volt90`, `HaptiSpacing.m` |
| theme | Mapping tokens to light/dark and providing them | tokens | `HaptiMapTheme(ThemeMode.DARK)` |
| atoms | One indivisible UI element | theme | `HaptiButton`, `DistanceText`, `NoticeLabel` |
| molecules | A small group of atoms with one job | atoms, theme | `ManeuverIndicator` |
| organisms | A self-contained section of a screen | molecules, atoms, theme | `PlaceResultsList`, `StatusMessage`, `ConfirmDialog` |
| templates | Screen layout without real data | organisms ↓, theme | `GlanceScreenTemplate`, `CenteredScreenTemplate`, `RoundScreenTemplate` |
| screens (features) | Templates filled with real state | everything above | `RouteSummaryScreen` |

**Dependencies only flow downward.** An atom never imports a molecule, and a molecule never
imports an organism. An atom may specialize another atom (`DirectionArrow` is a `HaptiIcon` with
the `navigationArrow` color and a TalkBack description).

## 2. Tokens

### Always go through the theme

```kotlin
Text(
    text = stringResource(R.string.catalog_title),
    style = HaptiMapTheme.typography.title,
    color = HaptiMapTheme.colors.onBackground,
)
Modifier.padding(HaptiMapTheme.spacing.m)
```

### Never use raw values

```kotlin
Color(0xFF3F5600)      // ✗ bypasses light/dark and the palette
8.dp / 14.sp           // ✗ an orphan value nobody can update globally
Text("Start route")    // ✗ never translated
```

Why this matters:

- **One change, whole app.** `primary` is defined once per scheme. Changing `BasePalette.Volt90`
  recolors every maneuver disc, primary button and the map route line.
- **Light/dark for free.** Components ask for a *role* (`onSurface`), not a color, so the
  theme switch is a single `CompositionLocal` change.
- **Guaranteed contrast.** Every text/background pair was checked against WCAG AA (≥ 4.5:1).
  A raw color skips that check.

### Two layers of color

1. `BasePalette` (internal): the named swatches (`Volt90`, `Orchid80`, `Neutral12`...).
   Components **cannot** see it.
2. `HaptiColors` (public): semantic roles. These are the only colors components use.

Palette **Pulso** (redesign, see §22). Each color has exactly one meaning:
volt = brand and Focus, orchid = Tour and its places, sky = arrival, amber = warning, red = a problem.

| Role | Dark (OLED) | Light (outdoors) | Used for |
|---|---|---|---|
| background | `#000000` | `#F7F9F1` | screen background |
| onBackground | `#F3F6EC` | `#12150C` | main text, clock |
| surface | `#161A10` | `#E8ECDE` | fill of secondary buttons, chips, cards |
| onSurface | `#F3F6EC` | `#12150C` | text on surface |
| onSurfaceVariant | `#B4BCA6` | `#4A5240` | secondary text |
| outline | `#747E66` | `#7A846C` | 1.5 dp border of every surface control (≥ 3:1 on background) |
| primary / navigationArrow | `#D4F53C` | `#3F5600` | brand, Focus, primary actions, maneuver disc, route line |
| onPrimary | `#172000` | `#FFFFFF` | content on primary |
| tour | `#FF8FD8` | `#8E2A9E` | Tour mode, POI suggestion/detour, POI marker, detour line |
| onTour | `#3A0030` | `#FFFFFF` | content on tour |
| success | `#5CCFFF` | `#00658A` | arrival (disc, destination marker) |
| onSuccess | `#00283A` | `#FFFFFF` | content on success |
| warning | `#FFB547` | `#8F5600` | degraded conditions: offline, weak GPS |
| offRoute / error | `#FF5A3C` | `#C0122C` | off route, failures |

`ColorContrastTest` (`:core:designsystem`, JVM) locks this down in both themes: every text and accent
color ≥ 4.5:1 on background and on surface, every `on*` color ≥ 4.5:1 on its container, `outline`,
`primary` and `tour` ≥ 3:1 (WCAG 1.4.11, the boundary of cards and buttons), and every two semantic
colors at least ΔE 30 apart in Lab (none can be mistaken for volt or orchid).

`HaptiMapTheme` also feeds these roles into the Wear Material 3 `ColorScheme`. Stock Wear
components (scaffold, scroll indicator) therefore follow the tokens too.

### No elevation

The design is intentionally flat, following the Wear OS guidelines: hierarchy comes from color
and shape, never from shadows. There is no elevation token; don't add one.

### Haptic language (data only, played by `:core:haptics`)

The patterns live in **`:core:model`** (`HapticPattern.kt`), next to `HapticEvent`, not in the
design system. They are pure data, and the module that plays them (`:core:haptics`, step 09)
must not depend on Compose or the UI. The bench and the debug indicator read them from there too:
`HapticTokens.patternFor(HapticEvent.TURN_LEFT)`. **Step 14 retunes them in that one file**, without
touching any logic.

| Event | Pattern | Data (ms @ amplitude 0–255) |
|---|---|---|
| TURN_LEFT | 2 medium pulses, **slow and long** | 200 on · 250 off · 200 on @160 (650 ms) |
| TURN_RIGHT | 3 medium pulses, **fast and short** | 3 × 80 ms @160, 100 ms gaps (440 ms) |
| CONTINUE | 1 soft pulse | 120 ms @80 |
| TURN_IMMINENT | progressive pulses | 40 → 70 → 100 → 150 ms @80 → 160 → 255 → 255, gaps 150 → 110 → 70 (690 ms) |
| ARRIVAL | long pulse + double tap | 500 ms @160, 150 gap, 2 × 40 ms @255 |
| OFF_ROUTE | long intense pulse | 900 ms @255 |
| BACK_ON_ROUTE | one soft swell | 130 @80 → 130 @120 → 140 @160, no gaps (400 ms) |
| POI_SUGGESTION | distinct burst (version 2) | 4 × 40 ms taps, 50 ms gaps, alternating @80/@255 |

**Left vs right.** The product document's counts (2 and 3) are kept, but the rhythm differs too: a
left pulse is 2.5× longer than a right one and its gap 2.5× wider, so "taaa — taaa" vs "ta-ta-ta"
is distinguishable even without counting, and without amplitude control. First item to test on a
real watch (step 14 checklist in `docs/steps/09-motor-haptico.md`).

**Without amplitude control** `HapticPattern.onOffTimings()` turns the pattern into on/off timings;
contiguous non-zero segments merge into one pulse. Every pattern was designed to survive that:
imminent still grows (longer pulses, shorter gaps), back-on-route becomes one 400 ms pulse.

**Intensity.** `HapticPattern.scaled(intensity)` multiplies every non-zero amplitude, clamped to
1–255 so a pulse never disappears; `HapticEngine.intensity` applies it (the step 25 setting).

`HapticPattern(timings, amplitudes)` uses the exact format of `VibrationEffect.createWaveform`.
The exhaustive `when` in `patternFor` means a new `HapticEvent` without a pattern does not compile.

## 3. Light and dark mode

```kotlin
HaptiMapTheme(themeMode = ThemeMode.SYSTEM) { … }   // SYSTEM | LIGHT | DARK
```

`SYSTEM` follows the device's `uiMode`. The catalog's theme button cycles through all three modes.
The choice is kept in memory for now; persisting it is a later step.

**Ambient is dark in both themes (step 12).** `HaptiMapTheme(themeMode, ambient = true)` ignores `themeMode` and
uses the ambient roles (§18): black background, one light gray. Wear's always-on rules (mostly black, few lit
pixels, no large fills) apply to the display, not to the user's theme, and a white AOD would burn in and drain the
battery. The interactive Eco view follows the theme as every other screen does.

## 4. Languages

All user-visible text comes from `stringResource()` / `pluralStringResource()`.

| Folder | Language |
|---|---|
| `res/values/` | English (default, used for any unsupported locale) |
| `res/values-es/` | Spanish |

Strings that belong to design-system components (arrow descriptions, distance formats, preview
samples) live in `:core:designsystem`. Screen strings live in their own module. Token names shown
in the catalog are code identifiers and are marked `translatable="false"`.

### Adding a new language (example: Portuguese)

1. Create `res/values-pt/strings.xml` in **every** module that has a `res/values/strings.xml`
   (`app`, `core/designsystem`, …). Translate each `<string>` and `<plurals>` entry, keeping the
   same `name`. Skip entries marked `translatable="false"`.
2. Add `<locale android:name="pt" />` to `app/src/main/res/xml/locales_config.xml`. This makes
   the language appear in the system's per-app language settings (Android 13+).
3. Add `locale = "pt"` variants to `HaptiPreviews` if it should be previewed.
4. Run `./gradlew lintDebug`. The `MissingTranslation` check lists any string you forgot.

**Per-app language picker: Android 13+ only.** `locales_config.xml` drives the system's
per-app language setting, which exists only from Android 13 (API 33). On older watches
the app follows the device language. We don't build an in-app picker for them: it would duplicate
a system feature for a shrinking set of devices.

**Place categories.** `PlaceCategory` values are English code identifiers (`PHARMACY`, `TRANSIT`…).
Users never see them: `PlaceCategory.label` (in `:feature:search` `SearchUi.kt`) maps each one
to a `category_*` string in both languages, and `PlaceCategory.icon` to its drawable. `CategoryChip` only receives the resolved text and a `Painter`.

## 5. Adding a new component

1. **Choose the level.** One element → atom. A few atoms with one job → molecule. A screen
   section → organism. A screen skeleton → template.
2. **Create one file** in that folder, named after the component (`SpeedText.kt`).
3. **Only use the theme:** `HaptiMapTheme.colors / typography / spacing / shapes / dimens`.
   If a value you need is missing, **add a token first**. Never write it inline.
4. **Text comes in as a `String`** already resolved by the caller with `stringResource()`, or
   from the module's own `strings.xml` (in both `values/` and `values-es/`).
5. **Accessibility:** interactive elements are at least `dimens.minTouchTarget` (48 dp). Icons have
   a `contentDescription` (or `null` if purely decorative).
6. **Preview** with `@HaptiPreviews` and `PreviewSurface { … }`. That gives all 12 combinations
   (small round, large round and square × light/dark × en/es).
7. **Show it** in `DesignSystemCatalog`.

**`CategoryChip` is a pill button, not a Wear chip/toggle.** It is a `Button` with
`shapes.full`, and its selection is shown by the primary color and announced through
`semantics { selected }`. It has a single label (and, since step 11, a decorative icon) and uses the same touch target as every other
button. A selectable chip component would add a second visual style without adding anything.

```kotlin
@Composable
fun SpeedText(kmh: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.speed_kmh, kmh),
        modifier = modifier,
        style = HaptiMapTheme.typography.body,
        color = HaptiMapTheme.colors.onSurfaceVariant,
    )
}

@HaptiPreviews
@Composable
private fun SpeedTextPreview() = PreviewSurface { SpeedText(kmh = 5) }
```

## 6. Offline indicator

The offline indicator is a state of the **`HaptiTimeText` atom**, not a separate widget. When
offline, a curved label in `warning` comes before the time: **"Offline · 10:56"** /
**"Sin red · 10:56"**.

| Choice | Why |
|---|---|
| In the clock band | The band at the top is where Wear OS puts status on every screen. The label is always visible and never takes space from the content or covers a maneuver. |
| Atom | It is one indivisible piece of the clock, not a group of components. |
| `warning` color | Not a failure (`error`): navigation continues. It passes AA in both themes. |
| Text, no icon | Clearer at a glance than a crossed-out cloud, and TalkBack reads it as is. |
| Spanish "Sin red" | "Sin conexión" doesn't fit next to the time within TimeText's maximum sweep. |

```kotlin
HaptiTimeText()                // reads LocalOffline (default: false)
HaptiTimeText(offline = true)  // explicit, for previews
CompositionLocalProvider(LocalOffline provides isOffline) { … }   // done once in MainActivity
```

`RoundScreenTemplate` reads `LocalOffline` and passes it to its `HaptiTimeText`. Wear draws the
`ScreenScaffold`'s `timeText` inside `AppScaffold`, which is outside any provider set by the
screen, so without this the catalog's "Simulate offline" toggle wouldn't work. Previews:
`HaptiTimeTextOfflinePreview` (12 combinations). In the catalog, "Simulate offline" forces the
state without cutting the network.

**Safe top padding: `dimens.clockBandSafeTop` (8 dp round, 16 dp square).** The curved label runs down the sides of
the clock band, below where Wear's own top padding ends, so it used to overlap the first item
(step 03 note). Every template adds this token to the top of the `ScreenScaffold`
`contentPadding`, so every screen gets the room. It depends on the screen shape (step 12): on square,
`ScreenScaffold`'s padding is smaller and content reaches the corners, so 8 dp let "10:10" touch "¿A dónde vas?";
`HaptiMapTheme` provides `haptiDimens(round = isScreenRound)`, so no screen has to know. It is always applied, online too, so content
doesn't jump when connectivity changes. Checked on the small round emulator with "Simulate
offline" in English and Spanish: the label's lowest point now sits above the first item.

## 7. Screens and the scaffold

**First-view rule (step 07).** On no screen size may a line of text, or a button's label, be cut
by the bottom edge in the first view, before any scroll. Whatever doesn't fit whole starts entirely
below the edge, and the scroll indicator shows there is more. Checked in the screenshot tests,
12 combinations per state.

| Screen kind | Template | How the rule holds |
|---|---|---|
| A state with content and actions: navigation, route summary, resume, arrival, location permission | `GlanceScreenTemplate` (§12) | The only template for these. It measures: the glance always in the first view; each detail (text or button), in priority order, only if it fits whole |
| A short message, maybe with one button: loading, route errors | `CenteredScreenTemplate` | No scroll; a message of up to 3 lines plus a button fits inside the circle (§9). Longer copy must be shortened, not scrolled |
| A list of rows: search results, catalog, debug screens | `RoundScreenTemplate { item { … } }` | The exception: a row cut at the bottom is how a list shows it scrolls. Rows are touch targets, not reading text |

Picking the order of `details` is the screen's design decision: the screen's main action goes
before secondary information (route summary: Start before the first-maneuver preview). The glance
must fit the smallest screen (small round, about 127 dp of safe height); when copy is too long to
fit whole, shorten the copy (location rationale, step 07).

The app root must be
`AppScaffold(timeText = { HaptiTimeText() })`. After a recomposition (for example a theme
switch), Wear's scaffold can fall back to the app-level time text. If that time text is not
`HaptiTimeText`, the clock reverts to Wear's 80%-luminance gray, which is unreadable in light mode.

## 8. Place search (step 04)

Three pieces, shown in the catalog under the category chips. Each one has `@HaptiPreviews`
(12 combinations since step 05).

| Component | Level | What it shows |
|---|---|---|
| `PlaceRow(item: PlaceRowItem, onClick)` | molecule | Name (`label`, up to 2 lines) over "Category · 850 m" and, since step 11, the short address (`caption`, `onSurfaceVariant`) in a `surface` button with `shapes.large`. Text-search results with no category show only the distance. The whole row is one touch target, at least `minTouchTarget`. |
| `PlaceResultsList(items, onItemClick: (index) -> Unit)` | organism | A `PlaceRow` per item, in the order received, spaced `spacing.s`. It is a `Column`, not a lazy list: it goes as one item inside `RoundScreenTemplate`, which already scrolls, and never holds more than `MAX_RESULTS` (10). |
| `StatusMessage(text, tone, loading)` | organism | The text in `body`, colored by `StatusTone` (`Default` → `onBackground`, `Muted` → `onSurfaceVariant`, `Warning`, `Error`), with a spinner in `primary` above it when `loading`. A `liveRegion`, so TalkBack announces each change. Used by search and route alike. |

**One mapping for error texts.** `PlaceSearchError.message` and `.tone` (in `:feature:search`
`SearchUi.kt`, in `:app` until step 11) are the only place that turns a search error into a text and a
tone; `SearchStatusMessage` renders it with `StatusMessage`.
Tones: `NO_RESULTS` is not a failure (`Muted`);
`NO_CONNECTION` and `NO_LOCATION` are conditions the user can wait out (`Warning`, like the offline
label); quota, invalid key and other API failures are `Error`.

**Distances follow the app language.** `formatDistance(meters)` formats with the locale from
`Context.appLanguage()`: "850 m", "1.2 km" in English, "1,2 km" in Spanish. `DistanceText` and
`PlaceRow` both use it. `appLanguage()` reads the `app_language` string ("en" / "es"), which
Android resolves from the Context's `Configuration`, per-app language included. An unsupported
device language therefore falls back to English for the decimal separator, the same as the texts.
The Places `languageCode` comes from the same function, so place names, addresses and distances
always agree with the rest of the UI.

## 9. Route summary (step 05)

| Component | Level | What it shows |
|---|---|---|
| `DirectionArrow` / `ArrowDirection.ARRIVE` | atom | A destination pin for the arrival maneuver ("Arrive at destination" / "Llegada al destino" for TalkBack). The `Maneuver` → `ArrowDirection` mapping is in `:feature:navigation-ui` (`RouteUi.kt`). |
| `RouteStatsText(distanceMeters, duration)` | atom | "1.2 km · 15 min" in `body`, `onSurfaceVariant`. One `Text`, so an atom. `formatDuration()` lives here, next to how `formatDistance()` lives in `DistanceText`: "14 min", "1 h 5 min", rounded up, digits in the app language. |
| `HeadingText(text)` | atom | A screen heading: `title`, `onBackground`, centered, up to 2 lines, announced as a heading by TalkBack. The route screen uses it for the destination name. Since step 06 it is `dimens.centeredContentWidthFraction` wide: it sits at the top, where the round screen is narrowest, and at full width a two-line name was cut at the sides. |
| `StatusMessage(text, tone, loading)` | organism | The same organism as search. `RouteStatusMessage` (`:feature:navigation-ui`) turns `RouteStatus` into it: "Finding route…" while loading; `RouteError.message` / `.tone` are the only route error → text/tone mapping. |

**First maneuver.** `Route.firstManeuver()` (`:core:model`, unit-tested): the route's first step
is the departure (Google's `DEPART`, a `CONTINUE`), so it is step 1's maneuver at step 0's distance:
"in 120 m, turn left". A route with only the arrival step gives the arrival at the total distance.
`Route.toSummary()` turns it and the route totals into `GuidanceUi`, the same values the
navigation screen shows.

**Error tones** follow §8: no route is not a service failure (`Muted`); no connection
and no location are `Warning`; missing key, invalid key, quota and other API failures are `Error`.
The screen adds "Try again" (secondary) under every failure except `MISSING_KEY`, which a retry
cannot fix.

`RouteSummaryScreen` (`:feature:navigation-ui`): with a route, `GlanceScreenTemplate` (since step
07; the `RouteSummary` organism of step 05 was removed because one block can't be split between the
first view and below it): glance = `HeadingText` (destination), the offline notice, `RouteStatsText`;
details = primary "Start", `ManeuverIndicator`, `InstructionText`. While loading or after an error,
`CenteredScreenTemplate` with `RouteStatusMessage` (and "Try again").

**Layout rules found with the screenshot tests:**

- **The first item of a `RoundScreenTemplate` must be short.** `TransformingLazyColumn` centers its
  first item; when that item is taller than the screen, its top ends up under the clock. A short
  first item (a heading) is clamped to the top. Only lists use it now (§7).
- **A screen that is only a message uses `CenteredScreenTemplate`.** Text placed right under the
  clock is cut by the round edge (large round, long Spanish texts). Centered and at
  `dimens.centeredContentWidthFraction`, a 3-line message plus a button fits inside the circle.

## 10. Screenshot tests

**Design review sheet:** `docs/review/review-sheet.png`, the key screens in the 12 combinations
from the references. `python3 docs/review/make_review_sheet.py` regenerates it after re-recording.

Compose Preview Screenshot Testing renders `@Preview` functions with layoutlib (the renderer of
Studio's previews) and compares them with the committed reference images. Functions annotated
`@PreviewTest @HaptiPreviews` in `src/screenshotTest/kotlin` give 12 images each.

| Module | File | What |
|---|---|---|
| `:core:designsystem` | `PlacesScreenshots.kt` | `PlaceResultsList` with `samplePlaceRows()` |
| `:feature:search` | `SearchStatusScreenshots.kt` | Every search state with its real text and tone (`SearchStatusMessage`); in `:app` until step 11 |
| `:core:designsystem` | `SearchScreenshots.kt` | `CategoryChip` with icons, `VoiceSearchButton`, `HaptiButton` with an icon (step 11) |
| `:feature:search` | `SearchScreenScreenshots.kt` | The search and results screens in 10 states (step 11, §16) |
| `:core:designsystem` | `NoticeLabelScreenshots.kt` | `NoticeLabel` in its four tones |
| `:feature:navigation-ui` | `RouteSummaryScreenshots.kt` | The route screen in all 11 states (ready, ready with a long name, ready offline, loading, 7 errors) and the resume screen (pending, loading, short and long destination name) |
| `:feature:navigation-ui` | `NavigationScreenshots.kt` | The navigation screen in 12 states (step 07, §12) |
| `:feature:onboarding` | `PermissionScreenshots.kt` | The 5 location-permission steps (step 07, §7) |

```bash
./gradlew updateDebugScreenshotTest     # record or re-record the references after a deliberate UI change
./gradlew validateDebugScreenshotTest   # fail if a screen changed
```

Preview names are file-safe (`round-small-light-en`, `square-dark-es`…) because they end up in the
image names. `HaptiTimeText` shows a fixed `10:10` in inspection mode; otherwise every reference
would fail as the clock moves. For the same reason `StatusMessage` draws its spinner as a fixed
270° arc (same stroke as the real one) in inspection mode: layoutlib captures an arbitrary frame of
the animation, empty or half drawn. The app always shows the animated indeterminate spinner.

## 11. Offline notice (step 06)

| Component | Level | What it shows |
|---|---|---|
| `NoticeLabel(text, tone)` | atom | One `Text` in `caption`, colored by `StatusTone`, on a `surface` pill (`shapes.full`, `spacing.l` × `spacing.s`). Not interactive; a `liveRegion`, so TalkBack announces it when it appears. |
| `StatusTone` + `StatusTone.color` | atom (`StatusTone.kt`) | Moved from `organisms/StatusMessage.kt`: an atom cannot import an organism. `StatusMessage` and `NoticeLabel` share the tone → color mapping. Same package change only; no reference image changed. |

`RouteOfflineNotice()` (`:feature:navigation-ui`, `RouteUi.kt`) is the only domain → UI mapping:
a loaded route while offline → "Works offline" / "Funciona sin red" in `Warning`. It sits under the
destination heading on the route summary and, until the step 07 navigation screen exists, on
Session debug. It complements the clock-band label (§6): the clock says *you are offline*, the notice
says *what that means for this route*.

| Choice | Why |
|---|---|
| Atom, not molecule | One text with a background: nothing to split. No icon, for the reason in §6. |
| Short text | "Route saved · works offline" wrapped to an orphan word in Spanish inside the pill (seen in the screenshots). Two or three words fit on one line on every shape. |
| `surface` pill | Separates the notice from the route stats under it. All four tones pass AA on `surface` (lowest: light `warning`, 5.61:1). |

**Resume screen** (`ResumeRouteScreen`), the start screen once location is granted:

- While the cache is read: an empty `CenteredScreenTemplate` (background and clock). Only if the read
  takes longer than 300 ms (`SLOW_LOAD_THRESHOLD`, `withSlowNotice()`) does `StatusMessage("Loading…", loading = true)`
  appear; otherwise it flashed on every launch.
- With a route: `GlanceScreenTemplate` (since step 07): glance = the destination in `HeadingText` and
  "Resume this route?" in `StatusMessage`; details = `HaptiButton` primary "Resume", secondary
  "Discard". On small round and square "Discard" starts below the edge, never cut.

## 12. Navigation screen (step 07)

| Component | Level | What it shows |
|---|---|---|
| `ConfirmDialog(visible, title, confirmLabel, dismissLabel, onConfirm, onDismiss)` | organism | Wear M3 `AlertDialog` with the title (`title` style, `onBackground`) and two `HaptiButton`s as list items: confirm (primary), dismiss (secondary). Text buttons rather than Wear's ✓ / ✗ icons: TalkBack reads the actual action ("Stop") and they look like the rest of the app. Swipe right = dismiss. `ConfirmDialogContent` is the same content without the dialog window, for previews and screenshots (layoutlib doesn't draw dialog windows). First user: Stop navigation; future confirmations reuse it. |
| `InstructionText(text)` | atom | The next maneuver's instruction while walking: `body` in `onBackground` (more visible than the route summary's `caption`), centered, up to 3 lines with ellipsis, `centeredContentWidthFraction` wide. One `Text`, so an atom. |

| `GlanceScreenTemplate(glance, details, belowFold)` | template | A scrolling `Column` (crown through `rotaryScrollable`, scroll indicator) with a measured first view. `glance` is centered in the safe area; the `details` follow in priority order, each only if it fits whole; from the first that doesn't, all start below the screen edge; `belowFold` (actions) always starts below the edge. So no text line is ever cut by the bottom edge on the first view, and a glance never lands on a button. Safe area: the scaffold padding, and on round screens down to where the circle is still `centeredContentWidthFraction` wide (20 % of the height from the bottom). Not a `TransformingLazyColumn`: it centers its first item and can't fix what falls in the first view. |

`NavigationScreen` (`:feature:navigation-ui`) on `GlanceScreenTemplate`. The Eco/AOD screen of step
12 keeps the glance.

| Part | Content | Notes |
|---|---|---|
| glance | One notice (`NavigationNoticeLabel`), only when there is one | "Weak GPS signal" / "Señal GPS débil" or "Works offline" / "Funciona sin red", both `Warning` |
| glance | `ManeuverIndicator` (arrow + distance to the maneuver) | Always fully in the first view; `formatDistance()` gives "0 m", "45 m", "12.4 km" / "12,4 km" |
| detail 1 | `InstructionText`, if any | The provider's text; for the arrival, the destination name. In the first view on large round; below the edge on small round and square, and with a notice |
| detail 2 | `RouteStatsText` with the remaining distance · time | Only after the instruction: it matters less while walking |
| belowFold | `HaptiButton` "Stop", secondary | Opens `ConfirmDialog` "Stop navigation?" → "Stop" / "Keep going" |

**Two notices together.** Only one shows: weak GPS wins. It affects guidance now, while offline
doesn't (the route is cached), and the clock band already says "Offline" (§6), so nothing is lost.
`navigationNotice(weakSignal, offline)` in `RouteUi.kt` is the only place that decides.

**Arrival** (same screen, `Arrived` / `Completed`): `GlanceScreenTemplate`. Glance = the `ARRIVE`
pin at `HaptiIconSize.Medium` and `HeadingText` "You've arrived" / "Has llegado"; details = the
destination in `StatusMessage` (`Muted`, a live region so TalkBack announces it) and a primary
"New search" / "Nueva búsqueda". With the large pin, "New search" fit the first view on no screen;
with the medium one it does except with a two-line name on small round and square.

## 13. Off route (step 08)

**No new components.** The off-route screen is `NavigationScreen` on `GlanceScreenTemplate`, built from
existing pieces. The only new design-system asset is a drawable:

| Asset | Where | What it shows |
|---|---|---|
| `ic_off_route` | `:core:designsystem` `res/drawable` | A dashed vertical route and a dot beside it: "you are not on the line". Deliberately **not an arrow**: without a reliable watch heading we don't know which way the route is, and a wrong arrow is worse than none (an arrow toward the nearest route point is a step 14 idea). Drawn with `HaptiIcon` (`Medium`, `StatusTone.Warning.color`) |

The domain → UI mapping is in `RouteUi.kt`: `OffRouteStatus`, `offRouteStatus()` and
`OffRouteStatusMessage()`. The screen state is `NavigationUiState.OffRoute(status, metersFromRoute, notice)`.

| Part | Content | Notes |
|---|---|---|
| glance | `NavigationNoticeLabel(WEAK_GPS)`, only when GPS is weak | The only notice shown off route |
| glance | `HaptiIcon(ic_off_route)` | Hidden while recalculating: the spinner takes its place |
| glance | `HeadingText` "Off route" / "Fuera de ruta" | Always |
| glance | `OffRouteStatusMessage`: one line by precedence (below) | `StatusMessage`, a live region: TalkBack announces each change |
| detail 1 | `HaptiButton` "Try again" / "Reintentar" (primary), only after an error | The action goes before the information |
| detail 2 | `StatusMessage` "32 m from the route" / "A 32 m de la ruta" (`Muted`, `formatDistance`) | Feedback while walking back |
| belowFold | `HaptiButton` "Stop", secondary, with the same `ConfirmDialog` | Unchanged from §12 |

The old route's arrow and distance are **hidden** while off route: they describe a maneuver the user
isn't heading to.

**Notice precedence on the navigation screen.** One status line and at most one notice.
`offRouteStatus()` and `navigationNotice()` in `RouteUi.kt` are the only places that decide.

| Session | Status line (first that applies) | Notice pill |
|---|---|---|
| `Recalculating` | 1. "Finding a new route…" / "Buscando otra ruta…" with the spinner, `Default` | Weak GPS only |
| `OffRoute`, offline | 2. "New route once online" / "Nueva ruta al tener red", `Warning`. No Retry: it couldn't work, and the request goes out by itself on reconnect | Weak GPS only |
| `OffRoute` with an error | 3. "Couldn't get a new route" / "No se pudo recalcular", `Error`, + Retry | Weak GPS only |
| `OffRoute` | 4. "Head back to the route" / "Vuelve a la ruta", `Default` | Weak GPS only |
| `NavigationActive` | (guidance, §12) | Weak GPS, else "Works offline" |

Why: recalculating is what is happening right now. Offline outranks the error because Retry can't
work offline and the reconnect retries anyway. Off route, "Works offline" is not shown: it describes
following the cached route, and the guidance is hidden. The clock band still says "Offline" (§6).
Weak GPS stays as a pill because it explains why recalculation may be waiting.

**Fit.** Checked in 60 new references (5 states × 12). Every off-route state fits the first view on
every screen. With weak GPS plus an error, the tallest glance, Retry starts below the edge on small
round and on square, never cut.

## 14. Haptics (step 09)

Three components draw the haptic language. They take only UI values: `WaveformSegment(durationMs,
level 0–1)` lists, resolved strings and a semantic tint. `HapticUi.kt` in `:feature:onboarding` maps
`HapticPattern` to them (names, tint, segments, playback progress); it moved there from `:app` in step 13, and the
bench and the debug badge import it.

| Component | Level | What it does |
|---|---|---|
| `HapticWaveform` | atom | Canvas. Width is time, height is strength (a soft pulse is at least 25 % tall so it never vanishes), each pulse is a pill centered on a thin `onSurfaceVariant` baseline that shows the silences. `scaleMs` sets how much time fits in the width: the bench passes the longest pattern's duration to every row, so lengths compare at a glance. `progress` (0–1) paints what has played in `playedTint` (`onSurface`). Decorative: no semantics |
| `HapticPatternCard` | molecule | Title + play icon, `HapticWaveform`, a "2 pulses · 650 ms" summary, on a `surface` card with `shapes.large`. The whole card is one button (`Role.Button`, TalkBack action "Play"), at least `minTouchTarget` tall. Bordered in `outline`; while playing, the border turns `primary` and the waveform fills. Built for the step 13 tutorial as well |
| `HapticCueBadge` | molecule | A `surface` pill with a compact waveform (36 × 14 dp) and the pattern's name. Live region: TalkBack announces it |

**Tint per event** (`HapticEvent.tint`): the role the same state has elsewhere. `OFF_ROUTE` → `offRoute`,
`OFF_ROUTE` → `offRoute`, `ARRIVAL` → `success`, `POI_SUGGESTION`/`POI_ARRIVAL` → `tour`, the rest `primary`.

**Haptics bench** (`HapticsDebugScreen`, debug only: the catalog button and the route only exist when
`BuildConfig.DEBUG`). `RoundScreenTemplate` with a `HeadingText` and one `HapticPatternCard` per
`HapticEvent`, all on the same time scale. Tapping a card asks `HapticEngine.play(event)`, so it follows
the same scheduling and log as navigation; the progress follows `HapticEngine.played`.

**Debug indicator** (`DebugHapticOverlay`, `src/debug`; an empty composable in `src/release`). Over any
screen in `MainActivity`, a `HapticCueBadge` fades in when a pattern plays, fills while it plays and
stays 1.5 s more. It sits under the navigation screen's distance (`BiasAlignment(0, 0.72)`), at most 0.74 of the
screen wide (a bit over the round screen's chord there, since the pill's ends are round); a long name ends in an ellipsis, the waveform stays. It exists so the emulator, which doesn't vibrate, can demo the whole flow.

Screenshots: `HapticScreenshots` in `:core:designsystem` (waveforms, cards idle and playing, badge) and
`HapticsDebugScreenshots` in `:app` (the bench idle and playing), 12 combinations each.

## 15. Navigation notification and Ongoing Activity (step 10)

**No new components, drawables or tokens.** The foreground service's notification and its Wear OS
Ongoing Activity reuse the navigation screen's icons and strings. They are drawn by the system, not by
Compose, so the app's `ThemeMode` and color tokens don't apply to them.

| Part | Content | Source |
|---|---|---|
| Icon (small icon, Ongoing Activity static icon on the watch face and in recents) | The next maneuver's arrow (`ArrowDirection.icon`: `ic_turn_left`, `ic_turn_right`, `ic_straight`, `ic_arrive`), `ic_off_route` off route or recalculating, `ic_arrive` after arriving | `:core:designsystem` `res/drawable`, the same vectors as `DirectionArrow` and the off-route glance |
| Title | The maneuver label (`direction_left` "Turn left" / "Gira a la izquierda", …), the off-route status line (`OffRouteStatus.message`, the same strings as `OffRouteStatusMessage`), or `navigation_arrived` | `:core:designsystem`, `:feature:navigation-ui` |
| Text | `formatDistance` to the maneuver; `navigation_from_route` off route; the destination name after arriving | `formatDistance(context, meters)`, the non-Compose overload of the atom |
| Ongoing Activity status and content description | `navigation_status` "%2$s · %1$s" (text · title: "120 m · Turn right"; distance first since step 11, the collapsed row cuts the end) | `:app` `values` / `values-es` |
| Channel | `navigation_channel_name` "Navigation" / "Navegación", `IMPORTANCE_LOW`, silent | `:app` `values` / `values-es` |

The mapping is `NavigationUiState.toNotificationContent()` in `:feature:navigation-ui`
(`NavigationNotification.kt`), which takes only UI values (icon and string resources, distances, the
destination name); the service only resolves resources. Unit-tested in `NavigationNotificationTest`.

**Monochrome, light and dark.** Every icon above is a single-color vector (`@android:color/white` fill
or stroke, no other colors), which is what the system needs for a small icon and a watch-face
complication: it uses only the alpha and tints it. Checked on the emulator (API 37,
`docs/steps/img/10/`): the watch face draws the arrow in white on black, and the notification list a
dark arrow on a light chip. Both looked the same with the system in night mode `yes` and `no`, because
Wear's system UI is always dark. The app's own light/dark setting has no effect on either. The watch
face's own styles on a real watch are a step 14 check.

**Throttling.** A change of maneuver, state or destination updates the notification at once; a
distance-only change at most every 5 s (`NotificationThrottle`). The watch-face icon changes with the
next maneuver as soon as the previous one is passed.

## 16. Search Mode (step 11)

**New and changed components.** All in `:core:designsystem`, with `@HaptiPreviews`, in the catalog and in
`SearchScreenshots.kt` (12 combinations each). The screens in `:feature:search` only compose them.

| Component | Level | What changed |
|---|---|---|
| `CategoryChip(label, icon, selected, onClick)` | atom | Now takes a category icon (`HaptiIcon` Medium, decorative, tinted like the label) and fills the width, like every row of a round list; the label goes to 2 lines with ellipsis ("Hardware store" / "Ferretería"). Selected = `primary` / `onPrimary` (unselected: `surface` with the `outline` border), and the last searched category stays selected when the user comes back |
| `HaptiButton(…, icon)` | atom | Optional decorative icon before the label, which then aligns to the start. Without an icon the button is the same code path as before: no existing reference image changed |
| `VoiceSearchButton(label, onClick)` | atom | A primary full-width `HaptiButton` with `ic_mic`: the main action of search. A specialization like `DirectionArrow`, so every voice entry looks the same |
| `PlaceRowItem.address` / `PlaceRow` | molecule | A third line with the short address (`caption`, `onSurfaceVariant`, 1 line, ellipsis) under "Category · 850 m". Optional: a place without an address keeps two lines |
| `ic_category_*` (7), `ic_mic` | drawables | Material baseline icons (the set `ic_arrive` and the arrows come from), 24 dp, white fill, so they tint with the theme. Store `store`, hardware store `hardware` (hammer), restaurant `restaurant`, café `local_cafe`, pharmacy `local_pharmacy`, transport `directions_bus`, supermarket `local_grocery_store` (cart). Chosen for simple silhouettes that read at 24 dp on the small round screen; the detailed `storefront` was rejected for that reason |

**Reused, not new.** Empty and error states are `StatusMessage` (organism) with a secondary `HaptiButton`
"Try again" in `CenteredScreenTemplate`, the same pattern as the route summary (§9). The offline and no-voice
notices are `NoticeLabel` (`Warning` / `Error`). The replace confirmation is `ConfirmDialog` (§12).

**Search screen** (`SearchScreen`, `RoundScreenTemplate`, a list):

| Order | Content | Notes |
|---|---|---|
| 1 | `HeadingText` "Where to?" / "¿A dónde vas?" | Short first item, as the template needs (§9) |
| 2 | `ActiveRouteButton`, only with an active route | "Navigating to X" / "Navegando a X" → navigation; "Route ready: X" / "Ruta lista: X" → route summary. One line, ellipsis |
| 3 | `NoticeLabel` "Search needs internet" / "Buscar necesita internet", `Warning`, only offline | Said before the user tries. Search stays tappable and answers "No connection" at once; navigation is not blocked |
| 4 | `NoticeLabel` "No voice input" / "Sin dictado", `Error`, only after the recognizer was missing | Until the next search |
| 5 | `VoiceSearchButton` "Search by voice" / "Buscar por voz" | Primary: in the first view on every shape |
| 6 | A `CategoryChip` per category | In the order of the product document §7.2 |
| 7 | "Design system" (secondary), debug only | The catalog and debug screens |

**Results screen** (`SearchResultsScreen`): with places, `RoundScreenTemplate` with the category or the dictated
text in quotes ("“Museo del Prado”" / "«Museo del Prado»") as `HeadingText` and `PlaceResultsList`. Loading,
empty and errors: `CenteredScreenTemplate` with `SearchStatusMessage`, plus "Try again" when `canRetry` (not
for empty, invalid key or no permission). Checked in the screenshots: the longest message with its button (no
location, Spanish) fits the first view on every shape.

**Fit notes from the screenshots.** The active route row took three lines with a long destination and pushed the
voice button to the edge of the first view on small round; since the follow-up it is `ActiveRouteButton`, one
line, and with it plus both notices the voice button's label and icon are whole in the first view on all three
shapes (on round screens the edge clips the pill's lower corners, not its content). The no-voice notice
was shortened to "No voice input" / "Sin dictado" after it wrapped inside its pill.

Screenshots: `SearchScreenshots` (`:core:designsystem`: chips, voice button, icon button, `ActiveRouteButton` short
and long),
`SearchStatusScreenshots` (moved from `:app`, identical images, plus `NoPermission`) and
`SearchScreenScreenshots` (`:feature:search`: home, home navigating offline, home with a loaded route and no voice,
results by category and by voice, loading, empty, no location with retry, no permission, the replace dialog).

### `ActiveRouteButton` (step 11 follow-up)

| Component | Level | What it shows |
|---|---|---|
| `ActiveRouteButton(text, navigating, onClick)` | atom | A secondary full-width `HaptiButton`, `maxLines = 1` with an ellipsis: `ic_straight` while navigating, `ic_arrive` (pin) for a loaded route. The text is the screen's ("Navigating to X"); a long destination is cut, not wrapped, so it never pushes what follows. TalkBack reads the full text (the ellipsis is only visual) |
| `HaptiButton(…, maxLines)` | atom | New optional parameter, default unlimited: no existing button changed (validated against every reference) |

Any screen that needs a way back to the route in progress uses this component, not its own row.

## 17. Wear Compose 1.7.1 (step 11 follow-up)

Wear Compose `compose-material3`, `-foundation`, `-navigation`, `-ui-tooling` 1.5.6 → **1.7.1**, and the Compose BOM
2024.09.00 → **2026.03.01** (UI 1.10.6, the last 1.10: Wear 1.7.1 requires UI 1.10, and the BOM keeps `ui-tooling`,
which renders the screenshots, on the same version). What changed:

| Area | Change |
|---|---|
| Design-system components (no scaffold) | **None.** Every reference in `:core:designsystem` identical |
| Full screens (with `ScreenScaffold`) | The curved clock (`TimeText`) only: a few pixels of glyph placement in "10:10" (0.12 % of the image), up to 1 % with the curved "Offline · " label. Content, colors, sizes and scroll indicator unchanged. 576 references re-recorded, checked side by side |
| Lint | `activity-compose` 1.10 (transitive) adds `ContextCastToActivity`, an error in `LocationPermissionScreen` (`LocalContext as Activity`); it only needed the `Context`, so the cast is gone. `:app` warnings 25 → 21 (four "newer version available" for the Wear Compose libraries) |
| Behavior | Checked on the emulator: resume prompt, route summary, navigation, back to search, category search; no crash |

~~Known, not caused by the upgrade: on **square** with the Spanish offline label, the curved "10:10"
touches the "?" of the search heading.~~ Fixed in step 12: `clockBandSafeTop` is 16 dp on square (§6).

## 18. Eco / AOD (step 12)

Display Mode A in two forms: the **Eco view** (interactive) is `NavigationScreen` (§12, §13), unchanged; the
**ambient view** shows its first view in ambient. Both compose the same pieces, so there is one arrow, one distance
and one status line in the app.

**Ambient is a theme state, not a set of components.** `HaptiMapTheme(ambient = true)` swaps the tokens and the
existing components adapt; `HaptiMapTheme.ambient` tells a component it is in ambient.

| Token / component | Level | In ambient |
|---|---|---|
| `AmbientHaptiColors` | color roles | Black `background` **and `surface`** (so pills and secondary buttons lose their fill), every content role `Neutral75` (light gray, not white: burn-in; no hue: low-bit screens). Same in light and dark (§3) |
| `AmbientTypography` | type | The same sizes and line heights as `DefaultTypography`, so nothing moves on entering; `distance`, `title`, `label` at `Normal` weight: thin strokes, fewer lit pixels |
| `ArrowDirection.outlineIcon` / `DirectionArrow` | atom | `ic_turn_left_outline`, `ic_turn_right_outline`, `ic_straight_outline`, `ic_arrive_outline`: the same path as the filled icon, stroked 0.6 / 24 with no fill (right is the mirror of left, as before). `DirectionArrow` picks them by itself |
| `StatusMessage(loading = true)` | organism | No spinner: no animation in ambient; the text ("Finding a new route…") says it. The spinner's space stays, empty, so the text doesn't move |
| `NoticeLabel` | atom | Text only (its pill is `surface`, now black) |
| `HaptiTimeText` | atom | Gray, with the black time-text background, from the root theme. Wear updates it once a minute |
| `AmbientScreenTemplate(glance, reservedDetails)` | template | Lays out with `GlanceScreenTemplate`'s own `FirstView` (same safe area, same "details that fit" rule), so `glance` sits exactly where the interactive screen puts it. `reservedDetails` are the interactive screen's details: measured, not placed, so they hold their space without drawing; values that change every fix go in as a fixed value of the same height. Opaque black, no scroll or crown. Wraps its content in the ambient theme. No manual pixel shifting: with `isBurnInProtectionRequired` the system shifts the display, and `ScreenScaffold` padding keeps the ≥ 10 px edge margin |

Previews: `DirectionArrowAmbientPreview`, `AmbientScreenTemplatePreview`, and `PreviewSurface(ambient = true)`.
Catalog: section "Ambient (AOD)" / "Ambiente (AOD)", a black block with the indicator, the four outline arrows,
a notice and a loading message, on any theme. Screenshots: `AmbientScreenshots` (`:core:designsystem`: arrows,
notice and status, the template offline) and `AmbientNavigationScreenshots` (`:feature:navigation-ui`), 12
combinations each.

**The app root.** `MainActivity` passes `ambient` from `rememberAmbientModeManager()` to the root `HaptiMapTheme`:
Wear composes the clock inside `AppScaffold`, outside any screen, so only the root theme reaches it (same reason as
`LocalOffline`, §6). Screenshots of ambient screens do the same.

**The ambient view** (`AmbientNavigationScreen`, `:feature:navigation-ui`, on `AmbientScreenTemplate`):

| State | Shows | Not shown (Eco only) |
|---|---|---|
| Navigating | Notice (weak GPS, else "Works offline"), outline arrow, **approximate** distance (approved steps: nearest 10 m below 100 m, 50 m to 500 m, 100 m to 1 km, 500 m beyond) | Instruction, remaining distance and time, Stop (their space is reserved) |
| Off route / recalculating / offline / failed | Weak GPS notice, `ic_off_route` (not while recalculating), "Off route", the status line by the §13 precedence, without spinner | Distance to the route, Retry, Stop (their space is reserved) |
| Arrived | Outline pin (`Medium`), "You've arrived" | Destination, New search (their space is reserved) |

**Nothing moves on entering or leaving ambient**, on any shape: the same sizes (`AmbientTypography`), the same
layout (`FirstView`), the same reserved space for what ambient doesn't draw. `AmbientPositionTest`
(`:feature:navigation-ui`) checks it on the screenshot references, each ambient screenshot against its Eco twin.

Offline also shows as "Offline · " / "Sin red · " in the clock. Lit pixels, measured on the references: at most
6.2 % (square, "12.5 km" with a notice); Wear's limit is 15 %.

**Rules for future ambient screens.** Compose the interactive screen's pieces inside `AmbientScreenTemplate`; drop
what goes stale between redraws, what animates and what you would tap, but pass the interactive details as
`reservedDetails` so nothing moves; never add a hue or a fill for ambient; if a
custom `Canvas` draws in ambient, turn off anti-aliasing when `isLowBitAmbientSupported`.

## 19. Onboarding: haptic tutorial (step 13)

Pattern → meaning → action, one screen per pattern. The feature only composes design-system pieces; the two new
ones are atoms.

| Component | Level | What it does |
|---|---|---|
| `StepIndicator(current, total)` | atom | Dots with `spacing.s` between them: done steps filled `primary`, the current one filled and wider (`stepDotCurrentWidth` 14 dp), the rest a `borderWidth` ring in `onSurfaceVariant` (`stepDot` 6 dp). Readable without color (shape and width). One TalkBack node: "Step 2 of 6" / "Paso 2 de 6" |
| `BodyText(text)` | atom | A paragraph: `body`, `onBackground`, centered, no line limit, `centeredContentWidthFraction` wide. Used by the permission screen and the tutorial |
| `HaptiEdgeButton(text, onClick)` | atom | Wear M3 `EdgeButton` (`Small`, primary / onPrimary, `label`, one line): the screen's primary action on the bottom edge. Only inside `GlanceScreenTemplate(edgeButton = …)`, which pins it at the bottom (measured first), lays the content out in the space above it and lets it scroll behind. Always in the first view, whatever the text above. Not `ScreenScaffold`'s `edgeButton`: with a scrolling column it stays collapsed until the end of the scroll. Labels must be short (one word or two): its width is fixed by the shape |
| `typography.distanceCompact` / `DistanceText(compact = true)` | token / atom | The distance at 20/24 sp, bold, tabular digits (thin in ambient): as tall as a `Medium` icon. Where a distance sits with text that must fit too (the "Turn now" lesson) |

**Screens** (`HapticTutorialScreen`, `:feature:onboarding`), all on `GlanceScreenTemplate` (§7: the glance always,
details in the first view only when whole, Skip always below the fold):

| Screen | Glance | Details, in priority order |
|---|---|---|
| Welcome | `HeadingText` "Learn the vibrations" | `BodyText` (how many patterns, a plural); without a vibrator a `NoticeLabel` (`Error`) "This watch can't vibrate"; with a vibrator, the vibration-off hint as `BodyText`. Edge button: Start |
| Lesson | `StepIndicator`, the navigation visual at `Medium`, `HeadingText` with the pattern's name | `BodyText` with how it feels and what to do; `HapticPatternCard` titled "Play again" (its waveform fills while that pattern plays, the "playing" state; nothing animates in ambient), or the no-vibrator `NoticeLabel`. Edge button: Next |
| Done | `HeadingText` "You're ready" | `BodyText` with where to replay it. Edge button: Search |

**The primary action is always in the first view** (step 13 follow-up): Start, Next and Search are
`HaptiEdgeButton`s, on every shape and in both languages. `TutorialFirstViewTest` checks it on the 132 tutorial
references.

**Rule: an edge-button label is one or two words, in every language** (step 15A). The button's width comes from the
screen shape, not from the text, and the label is one line: "Empezar a buscar" was cut to "Start sear…" in step 13
and became "Buscar". Put the rest of the meaning in the screen's text above the button. `EdgeButtonLabelTest`
(`:app`, JVM) enforces it: every `HaptiEdgeButton(text = stringResource(R.string.x))` call in `src/main` and
`src/debug` of any module, with `x` looked up in each `values*/strings.xml`; a call that passes a literal or a
variable fails the test, so nothing skips the check.

**Same icons as navigation.** Left, right, straight and arrive are `DirectionArrow`; off route is `ic_off_route`
tinted `StatusTone.Warning`, as on the off-route screen; the imminent turn is `DistanceText(compact = true)` "5 m",
what navigation shows at that moment (the pattern has no direction), small enough that "Turn now" and its meaning fit
in the first view on every shape. Visuals are hidden from TalkBack: the title and the meaning say
it. Card tint per event: `HapticEvent.tint` (§14).

**Replay entry** until step 25: a secondary `HaptiButton` "Vibration guide" / "Guía de vibraciones" at the end of
Search, hidden while navigating.

Screenshots: `TutorialPiecesScreenshots` (`:core:designsystem`: `StepIndicator` at the start, middle and end, in
ambient, `BodyText`, `DistanceCompact`, and `GlanceWithEdgeButton` with long text above the button) and `TutorialScreenshots` (`:feature:onboarding`: welcome, welcome without vibrator, the six
lessons, a lesson playing, a lesson without vibrator, done), 12 combinations each; `SearchHomeEnd`
(`:feature:search`) shows the replay row. Catalog: the atoms under "Components", `distanceCompact` under
"Typography".

**Version 2.** The POI suggestion (step 19) is one more `HapticLesson` in `hapticLessons`: the indicator, the
welcome count and the flow follow.

## 20. Largest font size (step 15A)

The whole main flow (permission → tutorial → search → navigation → route summary → Eco/ambient) survives the
system's largest font size. On Wear OS (API 37) that is Settings → Display → Font size → "Largest",
`fontScale = 1.24` (`LARGEST_FONT_SCALE`). Three fixes, all in the design system so no screen handles scaling:

| Fix | Component | What it does |
|---|---|---|
| Label shrinks, never truncates | `HaptiEdgeButton` | The label (one or two words, §19) follows the system font size, but the button's width is fixed by the shape; when it no longer fits, the label auto-sizes down to its unscaled size (`TextAutoSize.StepBased`) instead of ellipsizing. At 1.24 on the square, "Siguiente" was becoming "Sigue…". |
| Clock band grows with the offline notice | `clockBandTop(offline)` in `HaptiTimeText` | Below the clock, templates reserve `clockBandSafeTop` plus, when offline, `clockBandOfflineGrowth` for how much "No network · 10:10" grows with the large font, so it stops overlapping the title. Without the notice nothing changes, so Start and Resume stay in the first view. |
| Center, else scroll | `CenteredScreenTemplate` | When the centered content no longer fits (e.g. "Retry" was cut off and unreachable) the template becomes scrollable (finger, crown, scroll indicator); when it fits, it stays centered as before. |

**Screenshots.** `@HaptiLargeFontPreviews` mirrors `@HaptiPreviews`' 12 combinations at `fontScale = 1.24`;
20 main-flow screens × 12 = 240 new references. No normal-size reference changed. `TutorialFirstViewTest`
also checks the tutorial references at 1.24 (the primary action stays in the first view).


## 21. Profile selector (step 16)

Choosing a navigation profile (Focus / Tour) after the destination and before the route loads.

| Component | Level | What it does |
|---|---|---|
| `ProfileOption(icon, title, subtitle, selected, onClick)` | molecule | One selectable option: a Wear M3 `Button` with a leading `HaptiIcon`, a `label` (title) and a `secondaryLabel` (short subtitle). `selected` paints it in its mode color (`tone`: volt Focus, orchid Tour; the preselected one, to confirm with one tap); the rest use `surface` with the `outline` border. The whole button is the touch target (≥ `minTouchTarget`), grows to full width, and the crown scrolls the list. TalkBack reads it as one phrase and announces `selected`. Generic, like `PlaceRow`: the strings arrive already resolved, so the feature owns the copy. |

It is a **molecule**: it composes the `HaptiIcon` atom with two text styles into a single labelled control, reused for both options and open to more profiles later (version 2 settings). Not an atom (it combines several), not an organism (it is one small unit, not a section).

**Icons.** `ic_profile_focus` (a target: locked on the destination) and `ic_profile_tour` (a compass: explore), in `:core:designsystem` drawables, tinted through the theme like every other icon.

**Screen.** `ProfileSelectorScreen` (`:feature:navigation-ui`) is a `RoundScreenTemplate` (scroll, rotary, safe margins) with a `HeadingText` ("Choose mode") and the two `ProfileOption`s (each in its `ModeTone`). One tap on an option chooses it, fires the `PROFILE_SELECTED` haptic through the engine (never the vibrator directly), remembers it and moves on to the route summary. The last choice is preselected (highlighted) so the common path is a single tap.

**Haptic.** `HapticEvent.PROFILE_SELECTED` is a short, crisp confirmation ("tk-TUM": a soft tap then a firm pulse, `HapticTokens`), deliberately unlike any navigation cue, and lowest priority since it never shares the navigation cue stream.

Screenshots: `ProfileOptions` (`:core:designsystem`, selected + unselected) and `ProfileSelectorFocus` / `ProfileSelectorTour` (`:feature:navigation-ui`, each profile preselected), 12 combinations each.


## 22. Visual identity: Pulso (redesign)

Chosen from three directions (process, research and previews in `docs/design/directions/`).
Concept: **the vibration made visible**. The maneuver sits in a filled disc with a thin ring around it, and when a
real haptic cue plays, rings ripple out at the rhythm of that pattern. The screen only confirms what the wrist felt.

**Tokens.** Palette in §2. Typography: numerals and titles in the system **Roboto Condensed** (no bundled font),
bold for interactive, regular in ambient; body and buttons stay in Roboto. `distance` 50 sp (the unit at half size in
the mode color, so the number reads first), `distanceCompact` 20 sp, `title` 18 sp. Dimens: `outlineWidth` 1.5 dp,
`maneuverDisc` 52 dp + `maneuverRingGap` 6 dp, `pulseReach` 22 dp, `poiDisc` 44 dp, `iconDisc` 32 dp,
`iconButtonLarge` 60 dp.

| Component | Level | What it does |
|---|---|---|
| `ModeTone { Focus, Tour }` + `color` / `onColor` | atom | The trip mode as color: volt or orchid. `ManeuverIndicator`, `MapManeuverCard`, `ProfileOption` and `HaptiEdgeButton` take a `tone`, so Focus and Tour are told apart without reading |
| `IconDisc` | atom | Icon on a filled disc: maneuver, POI, off route (red), arrival (sky). In ambient a thin ring + the icon in grey, same size: nothing moves |
| `PulseRings(pulse, color)` | atom | Resting ring + one ripple per pulse of the cue that just played. Drawn outside its bounds, never pushes layout |
| `HapticPulse(startsMs, atUptimeMs)` | atom (data) | A cue that just played: when each pulse starts and when it played |
| `VoiceSearchButton` | atom | Now a 60 dp round volt button with the mic, no text (TalkBack reads "Search by voice") |
| `SuggestionCountdown(progress, …) { content }` | molecule | The orchid ring now wraps the POI disc instead of a seconds number (TalkBack still reads the seconds) |

**Pulse rules (battery, step 24).** `NavigationViewModel.pulse` maps `HapticEngine.played` and stamps the time *when
the cue plays*. `PulseRings` animates once (pattern length + 700 ms, about 1–2 s) and stops: never a loop. It does nothing
in ambient (`HaptiMapTheme.ambient`, and the screens below the AOD view are lifecycle-stopped). A cue that is already
over when the screen recomposes (coming back from Settings) is not replayed. `PulseStartsTest` checks one ring per
felt pulse for every event.

**Surfaces.** Every surface control (secondary button, chip, row, card, stepper, map button) carries the 1.5 dp
`outline` border, so its edge is ≥ 3:1 against the background; filled ones (primary, tour) already are.

**Map.** Style JSON from the tokens: background, streets halfway between background and `outline`, labels in
`onSurfaceVariant`. Route line volt, detour line orchid; destination marker = sky disc, POI marker = orchid disc,
both with a background-colored ring; the user arrow is `onBackground` so it never disappears on the volt route.
The maneuver pill now sits under the clock (`ScreenScaffold` padding, like the templates). Screenshots use a
`surface` placeholder: no tiles, no Maps calls.

**Copy.** Shorter on every flagged screen, EN and ES together. The tutorial lessons describe the real pattern
("2 long, slow pulses"); `LessonCopyTest` reads both `strings.xml` and checks the stated counts against
`HapticTokens` (and that left is longer than right, off route the longest).
