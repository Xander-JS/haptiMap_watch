# Step 07 — Navigation engine

## Goal

Follow the user along the active route: which step they are on, the distance to the next maneuver,
the remaining distance and time, and arrival. Emit semantic cues for the haptics of step 09, and
show it all on the navigation screen. Full reference: [`ARCHITECTURE.md`](../../ARCHITECTURE.md),
"Navigation Engine (step 07)".

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 08 off-route | `NavigationProgress.crossTrackMeters` and `fixAccuracyMeters` (don't flag off-route on a bad fix); dispatches `LeaveRoute` / `ReturnToRoute`; `RouteRecalculated` already restarts the tracker on the new route |
| 09 haptics | collects `NavigationEngine.cues` |
| 10 foreground service | moves the `engine.follow(locationSource.fixes())` call out of `HaptiMapApplication` |
| 12 Eco / AOD | derives from the navigation screen: arrow + distance (+ remaining) |
| 14 tuning | `NavigationThresholds`: values by speed and maneuver type |

## Small fix before the step

The resume screen showed "Loading…" for an instant on every launch. `withSlowNotice()` (`:core:model`)
runs a load and calls `onSlow` only if it takes longer than `SLOW_LOAD_THRESHOLD` (300 ms). The
delay runs on the caller's dispatcher, so tests drive it with virtual time. `ResumeRoute` starts in
`Pending` (background and clock only) and switches to `Loading` only after 300 ms. Its own commit.

## Decisions

| Decision | Why |
|---|---|
| Geometry in `:core:model` (`GeoMath.kt`: `bearingTo`, `offset`, `interpolate`, `projectOnto`, `MeasuredPath`) | The simulator in `:core:location` needs it too, and that module can't depend on `:core:navigation`. Projections use a local flat plane per segment: centimeters of error at walking scale. |
| Engine in `:core:navigation`: `RouteTracker` (pure, no clock: time comes from the fixes) + `NavigationEngine` (`@Singleton`, the flows) | Every replay test runs against `RouteTracker` without coroutines. Agreed in the plan. |
| `NavigationEngine.follow()` started in `HaptiMapApplication`, gated on `NavigationActive` | Navigation doesn't stop when the screen is left; GPS is used only while navigating. Agreed. Until step 10, Android may throttle GPS with the app in the background. |
| Maneuver positions: each `step.start` projected onto the route polyline, in order | Distances come from the geometry, not from Google's rounded step meters. |
| Map matching in a window around the current progress: 15 m behind, 30 m + 3 m/s × elapsed ahead (max 100 m); progress never decreases | A route passing near itself doesn't snap to the other leg; noise doesn't move progress back. The first fix searches the whole route (resume mid-route after process death); maneuvers already passed then are not announced. |
| Fixes worse than 20 m (`LOW_ACCURACY_THRESHOLD_METERS`, step 01) are ignored | They don't move progress or fire cues; only `fixAccuracyMeters` is updated. |
| Weak GPS: no fix within the accuracy threshold for 10 s (`weakSignalAfter`) | Covers both bad fixes (by fix timestamps, in the tracker) and silence (no fix at all, timed by the engine, also from the start). Cleared by the next good fix. Asked in the plan review. |
| Cues are `NavigationCue`, not `NavigationEvent` | `NavigationEvent` moves the session; cues only say what happened on the route. |
| Cue thresholds: approaching 18 m, imminent 6 m, passed 5 m after the maneuver | Product document §13: first indication about 15–20 m before, progressive reinforcement over the last meters, intensification when imminent. The step advances at "passed", the same moment the screen switches to the next maneuver. |
| **`ManeuverImminent` at 6 m is the value most likely to need tuning (step 14)** | At walking pace (~1.4 m/s) 6 m is about 4 s, close to GPS lag (1 s fixes plus filtering). With a late fix, "imminent" may arrive when the user is already at the corner. Tune with real walks; it may need to depend on speed. |
| Each cue exactly once per maneuver and threshold; a fix that skips thresholds emits the missed ones in order in the same update | The guarantee is simple to test and to consume. Within one fix, cues are ordered by maneuver: passing turn 1 and nearing turn 2 gives `Passed(1)`, then `Imminent(2)`. Step 09 can collapse a burst. The departure (step 0) gets no cues; the arrival step gets `Approaching` and then `Arrived`. |
| Arrival: remaining along-route distance ≤ 10 m + min(accuracy, 10 m) | Accuracy-aware radius. Overshooting the end leaves progress at the end (projection is clamped), so it is arrival too. Along-route rather than straight-line, so a loop that passes near its end doesn't arrive early. |
| Remaining time: the unwalked part of the current step's duration plus the later steps' durations | Uses Google's own pace per step. |
| Session: no new states or events. The engine dispatches `Arrive`; "New search" dispatches `Finish`; Stop dispatches `Cancel` | `Arrived` and `Completed` already clear the cache (step 06). Stop = `Cancel` → `Idle`, cache cleared, no resume: agreed. Search already starts from any state (`Cancel` + `StartSearch`). |
| Simulator: `simulatedWalk()` in `:core:location`, the switch only in `app/src/debug` | `LocationModule` is now per build type. Debug binds a source that walks the active route while navigating with a mode chosen in the catalog; release binds only `FusedLocationSource`. The catalog shows the selector only when `BuildConfig.DEBUG`. Modes: Off, Walking (1.4 m/s), Noisy GPS (4 m noise, a 40 m spike every 15 fixes), Weak GPS (14 bad fixes every 40), Fast (5 m/s). |
| Navigation screen as separate list items, not one organism | The first item of a `RoundScreenTemplate` must be short (`DESIGN_SYSTEM.md` §9), and step 12 keeps only the arrow and distance. New pieces: `ConfirmDialog` (organism) and `InstructionText` (atom). |
| Remaining "distance · time" above the arrow | Below the instruction it was cut in half at the bottom of the large round and square screens (seen in the screenshots). Now the remaining line, arrow and distance are fully visible on every shape. On large round and square the instruction starts at the bottom edge and continues on scroll, and with a notice it is below the fold: the arrow + distance (and the haptics, step 09) carry the guidance. Stop is last, reachable by scrolling, so a glance doesn't tap it. |
| One notice at a time: weak GPS wins over offline | Weak GPS affects guidance right now; offline doesn't (the route is cached). The clock already says "Offline", so that information isn't lost. Asked in the plan review. |
| "Navigation" entry in the catalog | Found while writing the device checklist: swiping back from the navigation screen leaves navigation running, and nothing reopened its screen (the route summary is out of the back stack). The catalog is the temporary home; step 10's notification is the real way back. |
| `OffRoute` / `Recalculating` show the navigating layout | `shortcut:` until step 08 gives them their own screen. Step 07 never dispatches `LeaveRoute`; only Session debug can. |

## Acceptance criteria

1. The engine follows a route from location fixes: step, next maneuver, distance to it, remaining distance and time, cross-track distance.
2. Progress never jumps back on noisy fixes or on a route that passes near itself; poor-accuracy fixes are ignored.
3. Cues are emitted once per maneuver and threshold, in order; arrival by radius and by overshooting the end.
4. Session: `NavigationActive` → `Arrived` → `Completed`, clearing the cache.
5. A weak-signal flag after 10 s without a good fix, shown as a notice, cleared by a good fix.
6. A simulated walk, debug only, selectable from the catalog.
7. Navigation and arrival screens from design-system components, English/Spanish, light/dark, 12 combinations; Start opens navigation.
8. All tests pass, lint 0 errors, no secrets in git.

## Verification (2026-10-28)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass (unit)** | `GeoMathTest` (6): bearings to the cardinal points, offset, interpolation, projection inside/before/after a segment and on a zero-length one, `MeasuredPath` lengths and positions. `RouteTrackerTest`: start-of-route progress; straight walk; remaining time; step advance at the maneuver + 5 m. |
| 2 | **Pass (unit)** | `RouteTrackerTest`: seeded 4 m noise (progress monotonic, same cues as clean); 40 m spikes with poor accuracy ignored; standing still for 2 min; a hairpin route with fixes 6 m off toward the other leg; resume mid-route. |
| 3 | **Pass (unit)** | `RouteTrackerTest`: every maneuver type (left, right, continue, arrival) gives exactly `Approaching, Imminent, Passed` per maneuver then `Approaching(arrival), Arrived`; thresholds at 18 / 6 / 5 m; a skipping fix emits the missed cues in order; arrival at 14 m (accuracy 5) but not 16 m, wider with poorer accuracy; overshooting the end by 30 m arrives once. |
| 4 | **Pass (unit)** | `NavigationEngineTest`: fixes collected only in `NavigationActive`; a walk to the end emits the cues and reaches `Arrived`; `Finish` → `Completed`; the cache file is deleted at `Arrived` and stays deleted. |
| 5 | **Pass (unit + screenshots)** | `RouteTrackerTest`: weak at 10 s of bad fixes, not at 9.999 s; also from the start; configurable; cleared by a good fix. `NavigationEngineTest`: silence for 10 s (and no fix at all) sets it. `NavigationViewModelTest`: weak wins over offline and clears back to offline. Screenshots `NavigationWeakGps`, `NavigationWeakGpsLongInstruction`. |
| 6 | **Pass (unit) · PENDING device** | `SimulatedWalkTest` (4): speed and interval, stays at the end, seeded noise, spikes, as `GpsStatus`. `assembleRelease` builds with the release `LocationModule`. Device checklist below. |
| 7 | **Pass** | 144 new references (12 states × 12): turn, long instruction, no instruction, 0 m / 3 m, 12.4 km / 2 h 35 min, toward arrival, offline, weak GPS (also with a long instruction), stop confirmation, arrived (short and long name). Plus 12 for `ResumeRoutePending`. `NavigationViewModelTest` (8): mapping, arrival instruction, offline notice, Stop confirmation and cancel, arrival and new search. |
| 8 | **Pass** | `./gradlew assembleDebug assembleRelease lintDebug testDebugUnitTest :core:model:test :core:navigation:test validateDebugScreenshotTest`: **172 unit tests** (2 skipped, `RoutesLiveTest`), **432 screenshot tests**; the 276 previous references validate unchanged. Lint 0 errors; `:app` keeps its 23 warnings. `git log -p --all \| grep -cE 'AIza[0-9A-Za-z_-]{35}'` = 0. |

## Device check with the simulator (pending)

Debug build. Catalog → "Simulated walk: …" cycles the mode; it applies while navigating.
`adb logcat -s HaptiSession HaptiLocation HaptiRouteCache`

| Action | Expected |
|---|---|
| Cold start with a saved route | No "Loading…" flash: the clock alone, then the resume prompt |
| Mode "Walking", search a place, get a route, Start | Navigation screen: remaining "distance · time", arrow, distance counting down about 1.4 m per second, instruction. The route summary is gone from the back stack |
| Keep watching up to a turn | The distance reaches 0 m; about 5 m later the arrow and instruction switch to the next maneuver and the remaining line keeps dropping |
| Let it reach the end | Arrival screen: pin, "You've arrived", destination, "New search". `HaptiSession: NavigationActive(…) + Arrive -> Arrived(…)`; `HaptiRouteCache` deletes the file |
| "New search" | Search screen; `Arrived + Finish -> Completed`, then `Cancel` / `StartSearch` from search |
| Mode "Fast (5 m/s)" | The same walk in a fifth of the time, for a quick demo |
| Mode "Noisy GPS" | The distance never goes up mid-walk; maneuvers still switch once; no false arrival |
| Mode "Weak GPS" | About every 40 s: "Weak GPS signal" notice after 10 s of bad fixes, gone at the next good fix |
| `svc data disable` while navigating | After 3 s: clock "Offline" + "Works offline" notice; navigation continues. With "Weak GPS" at the same time only the weak notice shows |
| Stop → "Keep going" / Stop → "Stop" | Keep going: dialog closes, still navigating. Stop: catalog; `Cancel -> Idle`; kill and reopen: no resume prompt |
| Swipe back from the navigation screen, then catalog → "Navigation" | Navigation kept going meanwhile (the distance has dropped); the route summary is not offered again. Step 10 adds a service and a notification to return |
| Language `es`, dark mode, small round and square | Spanish texts and locale distances ("1,2 km"); nothing cut except the instruction below the fold on large screens, reachable by scrolling |
| Release build (`assembleRelease`) | No simulator selector in the catalog |
