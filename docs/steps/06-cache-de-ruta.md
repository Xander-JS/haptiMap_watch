# Step 06 — Route cache

## Goal

Keep the active route on the device so navigation continues when connectivity is lost and survives
the app process being killed. Not offline navigation: new searches, new routes and recalculation
still need the network.

Out of scope: following the route (07), recalculation (08), haptics (09), the foreground-service
resume (10).

## Decisions

| Decision | Why |
|---|---|
| `RouteCache` in the existing `:core:navigation` | The cache follows session transitions exactly, and the module is pure Kotlin, so the tests run on the JVM. The step 10 service will read the same store. The only addition is the kotlinx-serialization plugin. Agreed before implementing. |
| One JSON file, `noBackupFilesDir/active_route.json`, no DataStore or database | One route of 5–50 KB. `java.io.File` + temp file + `renameTo` gives atomic writes without a dependency. `noBackupFilesDir`: a route belongs to this watch and this moment, so it's never backed up to the cloud. Agreed. |
| Private DTOs in `RouteCache.kt` (`CachedRouteFile`, `CachedRoute`, `CachedStep`, `CachedPlace`, `CachedPoint`) | `:core:model` has no serialization annotations. The model enums (`Maneuver`, `PlaceCategory`) are serialized by name; renaming one means bumping `ROUTE_CACHE_VERSION`. |
| Polylines stored encoded (`encodePolyline`, new in `:core:model` next to `decodePolyline`) | The route's and each step's points come from Google's 1e5 polyline, so encoding them again is exact (tested). Step start/end and the destination stay as raw doubles: they can have more precision. |
| `version` + `savedAtMillis`; max age `ROUTE_CACHE_MAX_AGE = 6 h` (constructor parameter) | A walk resumed after process death is minutes old; a route from hours ago no longer matches where the user is. Missing, corrupted, other-version, expired and step-less entries are all `null`, and the invalid file is deleted. |
| Lifecycle in `RouteCache.follow(store.state)`, started in `HaptiMapApplication` on an IO scope | Saves each new `activeRoute`, clears when there is none. `drop(1)` skips the `Idle` at process start, so the saved route waits for the resume prompt. |
| `NavigationSession.activeRoute` = `RouteReady`, `NavigationActive`, `OffRoute`, `Recalculating` | `Arrived` clears the cache along with `Completed`: an arrived route has nothing left to resume. |
| **"Offline in the session state" resolved by the step 03 decision** | The prompt asked to expose the offline condition in the session state. Step 03 made connectivity an independent state that the reducer knows nothing about. Agreed: the reducer stays network-free, and `ObserveActiveRoute` (`:feature:navigation-ui`) combines `store.state` with `ConnectivityMonitor.status` once into `ActiveRoute(route, offline)?`. `RouteSummaryViewModel` and `SessionDebugViewModel` use it; steps 08/09 can too. |
| Fail fast offline | Already in place from steps 04/05: `ConnectivityPolicy` before any request. Retry goes through `loadRoute`, which checks again (new test). |
| Resume: new event `RestoreRoute(route)`, `Idle` → `RouteReady` | Agreed flow. Resume → `RestoreRoute` → route summary (catalog below it); Discard → `clear()` → catalog. |
| `ResumeRoute` is the start screen whenever location is granted; it reads the cache off the main thread | Follow-up: the first version read the file on the main thread in `MainActivity` and again in the ViewModel. Now `ResumeRouteViewModel` reads it once on an injected `CoroutineDispatcher` (`Dispatchers.IO` from `RouteCacheModule`; tests pass a `StandardTestDispatcher`) and shows "Loading…" (`CenteredScreenTemplate` + `StatusMessage`) meanwhile. No route, or a session that is not `Idle` → the catalog, removing itself from the back stack like the permission screen. One read, one decision point, and `MainActivity` only checks the permission. |
| Resume screen in `RoundScreenTemplate`, not `CenteredScreenTemplate` | The approved sketch was centered. The screenshots showed "Discard" cut off on small round and square with a long name. Now: name in `HeadingText`, "Resume this route?", two buttons, scrollable. |
| `HeadingText` is `centeredContentWidthFraction` wide everywhere | Follow-up: at full width, the round top cut two-line names at the sides (first seen on the resume screen). Moved into the atom so every screen gets it. Re-recorded 4 references: `RouteReady` in Spanish on small round and square (light/dark); "Museo de la Ciudad" used to touch the round edge and now wraps to two lines. New `RouteReadyLongName` checks a two-line name in all 12 combinations. |
| Offline notice: atom `NoticeLabel(text, tone)`; `StatusTone` moved to atoms | Agreed. Mapping in `RouteOfflineNotice()` (`RouteUi.kt`). Text shortened to "Works offline" / "Funciona sin red" after the screenshots showed "Ruta guardada · sigue sin red" wrapping to an orphan word. See `DESIGN_SYSTEM.md` §11. |

## Acceptance criteria

1. The active route is saved at `RouteReady`, kept while navigating, and cleared on completion, cancel or a new destination.
2. The cache uses its own DTOs, an encoded polyline, a version and a timestamp; invalid entries are discarded without crashing.
3. Offline during `RouteReady` / `NavigationActive`, the session keeps the route and the UI shows it; network actions fail at once.
4. After process death, a valid route is offered for resume or discard.
5. New UI uses design-system components only, in English/Spanish and light/dark, with screenshots in all 12 combinations.
6. All tests pass, lint is clean, and no secret is in git.

## Verification (2026-10-28)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass (unit)** | `RouteCacheTest`: RouteReady saves and navigating keeps; recalculated route replaces; arrive + finish clears; cancel clears; a new search clears and the new route is saved; the initial Idle keeps the saved route and `RestoreRoute` reaches `RouteReady`. |
| 2 | **Pass (unit)** | `RouteCacheTest`: round trip with every `Maneuver`, the `ARRIVAL` step, null and non-null optional fields; polylines stored encoded; no file, garbage, truncated JSON, other version, older than 6 h (exactly 6 h still loads), no steps → `null`. `PolylineTest`: encoder matches Google's example and round-trips. |
| 3 | **Pass (unit)** | `ObserveActiveRouteTest`: offline and back during `NavigationActive` emits `offline = true/false` with the same route and session. `RouteSummaryViewModelTest`: ready route offline shows the notice and keeps `RouteReady`; retry offline fails with `NO_CONNECTION` without a request; an offline error doesn't show the notice. `SessionDebugViewModelTest`: `routeOffline` flips and the session stays `NavigationActive`. |
| 4 | **Pass (unit) · PENDING device** | `ResumeRouteViewModelTest`: `Loading` until the IO read finishes, then `Ready(route)`; resume → `RouteReady`; discard clears and stays `Idle`; no cache, expired, or session past `Idle` → `NoRoute` and resume is a no-op. `NavigationReducerTest`: `RestoreRoute` only from `Idle`. Device check below. |
| 5 | **Pass** | 72 new reference images: `NoticeLabelTones`, `RouteReadyOffline`, `RouteReadyLongName`, `ResumeRouteLoading`, `ResumeRoute`, `ResumeRouteLongName` (12 each). Of the 204 previous references, 4 were re-recorded on purpose (`HeadingText` width, see Decisions); the other 200 validate unchanged. |
| 6 | **Pass** | `./gradlew assembleDebug lintDebug testDebugUnitTest :core:model:test :core:navigation:test validateDebugScreenshotTest`: **127 unit tests** (2 skipped, `RoutesLiveTest`), **276 screenshot tests**. Lint 0 errors; `:app` keeps its 23 warnings. `git log -p --all \| grep -cE 'AIza[0-9A-Za-z_-]{35}'` = 0. |

## Device check (pending)

`adb logcat -s HaptiRouteCache HaptiSession`:

| Action | Expected |
|---|---|
| Get a route, Start, home, `adb shell am kill com.xanderjs.haptimap`, reopen | A brief "Loading…", then the resume prompt with the destination. `HaptiSession` logs nothing until Resume |
| Resume | Route summary with the route; `HaptiSession: Idle + RestoreRoute(…) -> RouteReady(…)`. Swipe back → catalog |
| Same as row 1, then `svc data disable` before Resume | Prompt and route summary work offline: clock "Offline" + "Works offline" notice; Start → Session debug "Navigating" with the notice |
| Same as row 1, Discard | Catalog; `adb shell run-as com.xanderjs.haptimap ls no_backup` has no `active_route.json` |
| `svc data disable` on a freshly loaded route summary | After 3 s: clock "Offline" + "Works offline" notice; Start still works; "Try again" on an error fails at once with "No connection" |
| Finish or Cancel in Session debug, kill, reopen | "Loading…" for an instant, then the catalog, no prompt; `HaptiRouteCache` logs nothing |
| `adb shell date` +7 h (or wait), reopen with a saved route | Catalog; `HaptiRouteCache: Discarding route cache: expired` |
| Language `es`, dark mode, long destination name | Spanish texts; the name never cut by the round top; "Discard" reachable by scrolling |
