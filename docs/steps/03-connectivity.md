# Step 03 — Connectivity

## Goal

Detect whether the watch has internet access and react to changes, following section 17 of the
product document:

| | Connected | Disconnected |
|---|---|---|
| Already loaded route | continues | **continues** |
| New search | available | not available |
| New route | available | not available |
| New POIs | available | not available |

Connectivity changes never interrupt an active navigation. Connectivity is an **independent state**:
it is not part of the navigation session state machine, and consumers combine both when they need to.

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 04 Places | `ConnectivityPolicy.canSearch` before searching |
| 05 Routes | observes `RouteLoading` to call the API; the ViewModel sends `RouteRequestRejected(NO_CONNECTION)` instead when `canRequestRoute` is false |
| 06 route cache | `canContinueLoadedRoute` is always true: a loaded route never depends on the network |
| 08 recalculation | `canRequestRoute` before `Recalculate`; `RecalculationFailed(NO_CONNECTION)` already exists |
| POIs (version 2) | `ConnectivityPolicy.canFetchPois` |

## Decisions

| Decision | Why |
|---|---|
| `ConnectivityStatus` (`Connected(transport)` / `Disconnected`) and `Transport` in `:core:model`; replaces the unused `ConnectivityState` enum | Pure Kotlin, shared by the engine, the policy and the UI. The transport is kept for debugging and future decisions (for example, heavier downloads only on Wi-Fi). |
| `ConnectivityPolicy` is a pure `object` in `:core:model` | The section 17 rules in one place, testable on the JVM. `canContinueLoadedRoute` ignores the status on purpose: that is the rule. |
| Engine in the existing `:core:network` | The module already existed for "Google Places/Routes and connectivity" and held the `ConnectivityMonitor` stub. No new module. |
| `ConnectivityMonitor.status: StateFlow<ConnectivityStatus>` | A `StateFlow` always has a value, so a screen can read it synchronously (`status.value`) when deciding, as `SessionDebugViewModel` does. |
| `registerDefaultNetworkCallback`, connected only with `NET_CAPABILITY_INTERNET` **and** `NET_CAPABILITY_VALIDATED` | The default network is the one the app's requests use. `VALIDATED` rules out captive portals and Wi-Fi without internet. On a watch the default network moves between Wi-Fi, LTE and the phone's Bluetooth proxy, and the callback follows it. |
| `callbackFlow` that sends the current status first | If there is no network at all, the callback never fires; the first value must not wait for it. |
| `DISCONNECT_GRACE = 3 s` (`DisconnectGrace.kt`) | Covers the default network switching between transports and short drops. Measured on the emulator: a 1 s data cut is lost and validated again in ~1.2 s. Reconnection and the first value are reported immediately. Implemented with `collectLatest`: a new value cancels the pending `Disconnected`. |
| `stateIn(appScope, WhileSubscribed(5 s))`, a `@Singleton` from `di/ConnectivityModule` | One callback for the whole app however many screens collect. The 5 s keeps it registered across Activity recreation (dark mode, language change) and releases it soon after the app goes to the background. The scope is created in the module (`SupervisorJob + Default`); it lives as long as the process, like the singleton. |
| Binding in `:app`, `:core:network` stays without Hilt | Same pattern as `LocationModule` (step 02). |
| `ACCESS_NETWORK_STATE` declared in `:core:network`'s manifest | The module that calls the API declares what it needs. Lint (`MissingPermission`) flagged it; the app's manifest also had it, and the merge is harmless. |
| `FakeConnectivityMonitor` in `main` (`MutableStateFlow`) | Same as `FakeLocationSource`: available to tests and previews of any module. |
| Offline indicator = an `offline` state of the `HaptiTimeText` atom | A curved "Offline" / "Sin red" label in `warning` before the time. The clock band is the status area Wear reserves on every screen, so the indicator is visible everywhere and never takes space from the content. It is an atom: one indivisible piece of the clock, not a group. |
| `LocalOffline` CompositionLocal, provided in `MainActivity` | Every screen gets the indicator without each ViewModel injecting the monitor. `RoundScreenTemplate` reads it where it is composed, because Wear draws the `ScreenScaffold`'s `timeText` inside `AppScaffold`, outside any provider in the screen. |
| Spanish label "Sin red" instead of "Sin conexión" | "Sin conexión · 23:00" exceeded TimeText's maximum sweep and pushed the time down the right edge (seen on the emulator). "Sin red" has the same length as "Offline". |
| New event `RouteRequestRejected(error)`, valid only from `DestinationSelected` | Goes to `DestinationSelected(destination, error)` **without** passing through `RouteLoading`. In step 05 something will observe `RouteLoading` to call the Routes API, so even a transient `RouteLoading` would fire a request. The reducer still knows nothing about connectivity. |
| The ViewModel decides, using `ConnectivityPolicy` | `SessionDebugViewModel.onEvent` turns `RequestRoute` into `RouteRequestRejected(NO_CONNECTION)` when `canRequestRoute` is false. |
| Catalog toggle "Simulate offline" (`rememberSaveable`) | Shows the indicator without cutting the network, and survives theme and language changes. It only adds offline: a real loss still shows when the toggle is off. |

## Acceptance criteria

1. When the emulator loses internet, the offline indicator appears after the grace period; when it comes back, it disappears immediately.
2. Short connectivity flickers (shorter than the grace period) do not show the indicator.
3. While offline, requesting a route in the Session debug screen ends in `RouteFailed(NO_CONNECTION)` with a visible message (implemented as `RouteRequestRejected(NO_CONNECTION)`, see Decisions).
4. While in `NavigationActive`, losing connectivity does NOT change the session state.
5. The network callback is registered once for the whole app and unregistered when nothing collects (verified with logs).
6. `ConnectivityPolicy` matches every product rule and is fully unit tested.
7. New UI works in English/Spanish and light/dark, with no hardcoded colors, dimensions or texts.
8. `assembleDebug`, `lintDebug` and all unit tests pass.

## Verification (2026-10-27, emulator `Wear_OS_Small_Round`, API 37)

The emulator's only network is cellular (`dumpsys connectivity`: `CELLULAR … INTERNET&VALIDATED`),
so connectivity was toggled with `adb shell svc data disable/enable`. Each command was marked in
logcat with `log -t HaptiTest`, and the app logs under the tag `HaptiConnectivity`.

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass** | `svc data disable` at 22:56:45.118 → `Status: Disconnected` at 22:56:48.179 (3.06 s). Screenshot at 1.5 s: no label; at 4.5 s: "Offline · 10:56". `svc data enable` at 22:57:04.401 → `Connected(transport=CELLULAR)` at 22:57:04.642 (0.24 s, the time the system took to validate the network); the label was gone in the next screenshot. Repeated after the final changes on the catalog: 3.09 s to appear, 0.30 s to disappear. |
| 2 | **Pass** | `svc data disable; sleep 1; svc data enable` in a single shell. `ConnectivityService` logged the cellular network as `DISCONNECTED` at 22:57:14.885 and the new one `+VALIDATED` at 22:57:16.063 (1.2 s without internet). The app logged no status change, and the indicator never appeared. |
| 3 | **Pass** | Session debug: Start search → Select destination → `svc data disable`, waited 4 s → Request route. The screen shows "Destination selected" + "No connection" in `error`, "Offline", and the policy rows (Search: no, New route: no, Loaded route: yes, New POIs: no). The only `HaptiSession` log is `DestinationSelected(…, error=null) + RouteRequestRejected(error=NO_CONNECTION) -> DestinationSelected(…, error=NO_CONNECTION)`; no `RouteLoading`. |
| 4 | **Pass** | Back online → Request route → Route loaded → "Navigating". `svc data disable`, waited 5 s: `HaptiConnectivity` logged `Disconnected`, `HaptiSession` logged nothing, and the screen still shows "Navigating" with the offline label. |
| 5 | **Pass** | One `Network callback registered` from app start, with the catalog (via `MainActivity`) and Session debug's ViewModel both collecting. Home at 22:58:44.319 → `Network callback unregistered` at 22:58:49.774 (5.4 s, the `WhileSubscribed` timeout). Reopen → a single `registered`. Two Activity recreations (`cmd uimode night yes/no`) → no new registration. |
| 6 | **Pass** | `ConnectivityPolicyTest` (3 tests): every transport allows search, routes and POIs; offline blocks all three; a loaded route continues in every status. `DisconnectGraceTest` (4 tests, virtual time): the first value is immediate even when offline; a disconnect shows at exactly 3 s and not at 3 s − 1 ms; a flicker of 3 s − 1 ms is not reported; reconnection (including a transport change) is immediate. `SessionDebugViewModelTest` (3 tests): online request → `RouteLoading`; offline → `DestinationSelected(NO_CONNECTION)`; losing and regaining the connection keeps `NavigationActive`. `NavigationReducerTest`: the new transition, a retry after a rejection, and the event ignored from every other state. |
| 7 | **Pass** | Screenshots: Session debug offline in es/dark; catalog with "Simulate offline" in en/dark, es/dark, en/light and es/light. All texts switch language, and the label uses `warning` (amber) in both themes. The toggle survives theme and language changes. Searched every new or changed `.kt` file for `Color(0x`, `<n>.dp`, `<n>.sp` and `Text("…")`: no matches. |
| 8 | **Pass** | `./gradlew assembleDebug lintDebug testDebugUnitTest :core:model:test :core:navigation:test`: build OK, 24/24 tests. Lint: 0 errors in every module; `:app` has the same 19 warnings as before this step (version notices, `UnnecessaryRequiredFeature`, `UnusedAttribute`). |

**Notes**

- At rest, the curved label's left end touches a wide first item placed right under the clock
  (for example "Design system" or "Session debug"). Content scrolling under the clock is normal
  Wear behaviour, but the navigation screens (step 07+) should leave room at the top for the
  wider clock band.
- `Status: Connected(...)` is logged again each time the callback is registered, because the
  `distinctUntilChanged` starts over with every new collection. The `StateFlow` value doesn't
  change, so nothing recomposes.
