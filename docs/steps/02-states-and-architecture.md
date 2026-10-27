# Step 02 — States and architecture

## Goal

Define the app's navigation session state machine, introduce dependency injection (Hilt) and
ViewModels, and replace the temporary screen switching with real Wear navigation. A temporary
"Session debug" screen, opened from the catalog, shows the session state and fires events by hand.
Full reference: [`ARCHITECTURE.md`](../../ARCHITECTURE.md).

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 03 connectivity | `RouteFailed(SessionError.NO_CONNECTION)`, `RecalculationFailed(NO_CONNECTION)` |
| 04 Places | replaces the `Destination` placeholder; dispatches `StartSearch` / `SelectDestination` |
| 05 Routes | replaces the `Route` placeholder; dispatches `RouteLoaded` / `RouteFailed` |
| 07–08 navigation engine | dispatches `LeaveRoute`, `ReturnToRoute`, `Recalculate`, `RouteRecalculated`, `Arrive` |
| 10 foreground service | injects the same `NavigationSessionStore` singleton |

## Decisions

| Decision | Why |
|---|---|
| Session machine in `:core:navigation`, which is now a pure Kotlin/JVM module | It is the Navigation Engine's module. A JVM module can't depend on an Android library, so `:core:model` became pure Kotlin too (it had no Android code). This means the compiler enforces criterion 6. |
| Removed the `NavigationState` enum from `:core:model` | It was never used, and the sealed `NavigationSession` replaces it. |
| States as a sealed interface that carries data; transitions in a pure `reduce()` | Each state carries only what it needs (`destination`, `route`, `error?`). An event that doesn't apply returns the same instance, so the store can tell that it was ignored. |
| `Cancel` from every state, including `Completed` | One event to go back to Idle. On `Completed` it acts as "close". |
| Failures keep you in the previous stable state with an `error` | `RouteFailed` → `DestinationSelected(error)`, where `RequestRoute` retries. `RecalculationFailed` → `OffRoute(error)`, where `Recalculate` retries. |
| Ignored events logged with `java.util.logging` (tag `HaptiSession`) | Android sends it to logcat, and the module still doesn't depend on Android. |
| `NavigationSessionStore` is `@Singleton @Inject constructor()` in `:core:navigation` | Only `javax.inject` is needed. Dagger in `:app` builds it. It lives outside ViewModels so the step 10 service shares it. |
| Hilt 2.60.1 + KSP 2.3.12 | AGP 9 support was added in Dagger 2.59. KSP 2.3 no longer depends on the Kotlin version and works with Kotlin 2.2.10 and AGP 9's built-in Kotlin. Checked by building. |
| `hiltViewModel()` from `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.4.0 | Its replacement in `hilt-navigation-compose` is deprecated. It works with any `ViewModelStoreOwner`, including Wear's `NavBackStackEntry`. |
| `LocationSource` binding in `:app` (`di/LocationModule`, `@Provides`) | `:core:location` stays free of DI. |
| Permission logic moved to `PermissionViewModel` | `asked`/`blocked` go in `SavedStateHandle`, which replaces `rememberSaveable`, and the step is computed there. The composable keeps only the platform glue: the launcher, detecting the dialog through the Activity pause, and opening settings. |
| `LocationDebugViewModel` uses `stateIn(WhileSubscribed())` with no timeout | GPS stops as soon as the screen stops collecting (Home, screen off), as in step 01 criterion 5. |
| Wear `compose-navigation` 1.5.6 with a sealed `Screen` wrapper | No Wear navigation release, up to 1.8.0-alpha02, has type-safe `composable<T>`. `Screen` keeps the route string private, and `navigateTo(Screen)` is the only way to navigate. It is not called `navigate()` because in navigation 2.8+ the member `navigate(Any)` would win over the extension and expect a serializable route. |
| Start screen chosen once in `MainActivity` | `Permission` if fine location is missing, otherwise `Catalog`. When the permission is granted, `popUpTo(Permission, inclusive)` makes the catalog the root. Revoking in settings kills the process, as in step 01. |
| Swiping right on the start screen exits the app | This is the standard Wear OS behaviour for the root screen. |

## Acceptance criteria

1. The app builds and runs on the emulator with Hilt; no manual wiring remains.
2. Navigation uses `SwipeDismissableNavHost`; swiping right goes back on every screen except the start screen.
3. The permission flow from step 01 still works, including coming back from settings and the approximate-location case.
4. The session debug screen shows the current state and every transition behaves as defined; invalid events change nothing.
5. The session state survives leaving and reopening the session debug screen (it lives in the singleton store, not in the ViewModel).
6. The state machine module has no Android dependencies.
7. All new screens work in English/Spanish and light/dark, with no hardcoded colors, dimensions or texts.
8. `assembleDebug`, `lintDebug` and all unit tests pass.

## Verification (2026-10-27, emulator `Wear_OS_Small_Round`, API 37)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass** | Fresh install and launch, no crash. `FusedLocationSource(this)` and the hand-built screen switching are gone; `LocationSource`, the store and the three ViewModels come from Hilt. |
| 2 | **Pass** | Swiping right on Location debug and on Session debug returned to the catalog. On the catalog (the root once permission is granted) and on the permission screen (the root without permission), the swipe exits the app. |
| 3 | **Pass** | Fresh install: rationale → "Don't allow" → Denied + "Try again" → 2nd denial → Blocked + "Open settings" → App info → Permissions → Location → "While using app" → back key → catalog. Second fresh install: precise switched off in the dialog → Approximate screen → "Use precise location" → system "approximate → precise" dialog → "Change to precise location" → catalog (`ACCESS_FINE_LOCATION: granted=true`). Relaunch with the permission granted → straight to the catalog. |
| 4 | **Pass** | On screen: `Arrive` in Idle → still Idle (log `Ignored Arrive in Idle`). Then StartSearch → Searching → SelectDestination → Destination selected → RequestRoute → Loading route → RouteFailed → Destination selected + "No route available" → RequestRoute → Loading route → RouteLoaded → Navigating → LeaveRoute → Off route → Recalculate → Recalculating → RecalculationFailed → Off route + "No connection" → Back on route → Navigating → Arrive → Arrived → Finish → Completed → Cancel → Idle. Each transition is logged as `HaptiSession: <before> + <event> -> <after>`. |
| 5 | **Pass** | In `Completed`, swiped back to the catalog and reopened Session debug: it showed `Completed`. Changing the app language, which recreates the Activity, also kept the state and the screen. |
| 6 | **Pass** | `:core:navigation` and `:core:model` use `org.jetbrains.kotlin.jvm`. `./gradlew :core:navigation:dependencies --configuration runtimeClasspath`: only `:core:model`, kotlin-stdlib, kotlinx-coroutines-core and `javax.inject`. |
| 7 | **Pass** | Screenshots of Session debug in en/light, es/light, es/dark and en/dark: every text switched language and colors follow the theme. Searched every changed `.kt` file for `Color(0x`, `<n>.dp`, `<n>.sp` and `Text("…")`: no matches. |
| 8 | **Pass** | `./gradlew assembleDebug lintDebug testDebugUnitTest :core:navigation:test :core:model:test`: build OK. Lint: 0 errors. The new warnings are only "newer version available" notices (`GradleDependency` / `NewerVersionAvailable`) for `compose-navigation` and `kotlin.jvm`, the same kind already reported for the existing libraries, plus the expected `UnnecessaryRequiredFeature`. Tests: 6/6 in `NavigationReducerTest` (every valid transition, Cancel from all 9 states, retry after both failures, every other state × event pair ignored, full trip) and 2/2 in `NavigationSessionStoreTest`. The 5 tests in `GpsStatusTest` still pass. |

**Notes**

- Home on Location debug still stops GPS right away (`Location updates stopped` about 3 s after
  opening the screen, when Home was pressed).
- The debug destination ("Puerta del Sol") is a fixed sample in `SessionDebugViewModel`. It isn't
  shown on screen.
