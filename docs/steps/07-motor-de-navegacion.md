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
| **Progress is the maximum of a filtered position, not of raw fixes** (follow-up, found on the emulator) | With "Noisy GPS" the screen went from "Turn right, 12 m" to the next maneuver in 2 s. The maximum of noisy positions only moves forward: measured over 20 seeded walks, `ManeuverPassed` fired up to 10 m before the corner with 4 m of noise, and up to 24 m before with 6 m. Now an alpha-beta filter (position and speed along the route, `filterAlpha` 0.3, `filterBeta` 0.05 in `NavigationThresholds`, chosen from four pairs by the same measurement) feeds the maximum. With 4 m of noise the turn is always passed 2–11 m after the corner (target +5); with 6 m, at the corner at the earliest. Cost: progress runs 0–3 m behind the walker. **These gains (0.3 / 0.05) were tuned on simulated Gaussian noise only and must be validated in the step 14 field tests** (real GPS noise is correlated in time and biased near buildings, which a white-noise simulation does not reproduce). Overshooting the end still arrives from the raw fix. Regression test with 20 seeds. Re-checked on the emulator: 0 m at the corner, then the next maneuver 6 m past it. |
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
| Navigation screen on a new template, `GlanceScreenTemplate` (follow-up) | Rule from the review: on no screen size may a text line be cut by the bottom edge on the first view, and the first view shows the full instruction or the instruction starts entirely below the fold. A `TransformingLazyColumn` can't guarantee it (it centers its first item and lets the next one fall under the edge), so the template is a scrolling `Column` with the crown (`rotaryScrollable`) and a measured first view: the glance (notice, arrow, distance) centered in the safe area; then the details by priority (instruction, then remaining distance · time), each only if it fits whole; from the first that doesn't, all start below the screen edge; Stop always below the edge. Safe area on round: down to where the circle is still `centeredContentWidthFraction` wide (20 % of the height from the bottom). New pieces in all: `GlanceScreenTemplate` (template), `ConfirmDialog` (organism), `InstructionText` (atom). |
| On small round and square, the first view is notice + arrow + distance | Measured from the references: the previews are about 192 dp (small round), larger on large round, and smaller on square in dp; the 56 dp arrow and the 40 sp distance take almost all of the safe area on the two smaller ones. Shrinking the main cue to fit the instruction was not worth it; the instruction is one crown turn away and the haptics (step 09) carry the guidance. On large round the short instruction and the remaining line fit in the first view. |
| One notice at a time: weak GPS wins over offline | Weak GPS affects guidance right now; offline doesn't (the route is cached). The clock already says "Offline", so that information isn't lost. Asked in the plan review. |
| "Navigation" entry in the catalog | Found while writing the device checklist: swiping back from the navigation screen leaves navigation running, and nothing reopened its screen (the route summary is out of the back stack). The catalog is the temporary home; step 10's notification is the real way back. |
| Debug sample route, `sampleRoute(language)` in `app/src/debug` (`null` stub in `app/src/release`) | Demo without an API key: Sol → Museo del Prado through the Barrio de las Letras, 1.04 km, two left and two right turns and a long last instruction, in the app language. Real street names, straight legs with approximate bearings (not a Google polyline). Loaded like a resumed route (`Cancel` + `RestoreRoute` → `RouteReady`), so nothing is requested from the network. The release dex has neither the fixture nor `SimulatableLocationSource` (checked by searching the APK). |
| `HaptiSession` logs names, not data (follow-up, found on the emulator) | Each transition logged the whole `Route` (~10 KB with every point); logcat cut the line and the `-> NewState` part was lost. Now: state and event type plus the destination name, e.g. `NavigationActive(Museo del Prado) + Arrive -> Arrived(Museo del Prado)`. |
| Design review sheet, `docs/review/review-sheet.png` | Route summary, navigation (plain, and with a notice and a long instruction), stop confirmation, arrival and resume in the 12 combinations, from the screenshot references. Regenerate with `python3 docs/review/make_review_sheet.py` after re-recording. |
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
| 6 | **Pass (unit + emulator)** | `SimulatedWalkTest` (4): speed and interval, stays at the end, seeded noise, spikes, as `GpsStatus`. `SampleRouteTest` (3, debug only): the turns, the language, a simulated walk through `RouteTracker` passes every maneuver and arrives. Release build: no fixture, no simulator. Device checklist below. |
| 7 | **Pass** | 144 new references (12 states × 12): turn, long instruction, no instruction, 0 m / 3 m, 12.4 km / 2 h 35 min, toward arrival, offline, weak GPS (also with a long instruction), stop confirmation, arrived (short and long name). Plus 12 for `ResumeRoutePending`. Follow-up `GlanceScreenTemplate`: **108 re-recorded on purpose** (the 9 navigating states × 12), each checked for a line cut by the bottom edge; stop confirmation and arrival unchanged. `NavigationViewModelTest` (8): mapping, arrival instruction, offline notice, Stop confirmation and cancel, arrival and new search. |
| 8 | **Pass** | `./gradlew assembleDebug assembleRelease lintDebug testDebugUnitTest :core:model:test :core:navigation:test validateDebugScreenshotTest`: **176 unit tests** (2 skipped, `RoutesLiveTest`), **432 screenshot tests**; the 276 references from before step 07 validate unchanged. Lint 0 errors; `:app` keeps its 23 warnings. `git log -p --all \| grep -cE 'AIza[0-9A-Za-z_-]{35}'` = 0. |

## Device check with the simulator (2026-10-28)

Emulator `Wear_OS_Small_Round` (API 37, 384 × 384 px, 320 dpi = 192 dp), debug build, the sample
route from the catalog (no API key). Driven with `adb` (`uiautomator dump` + `input`), screenshots
with `screencap`, the cold starts recorded with `screenrecord` and checked at 10 frames per second.
`adb logcat -s HaptiSession HaptiLocation HaptiRouteCache`.

| Action | Expected | Result |
|---|---|---|
| Cold start with a saved route | No "Loading…" flash: the clock alone, then the resume prompt | **Pass.** Splash → background and clock → resume prompt; no "Loading…" frame (it would last ≥ 3 frames). Without a saved route: splash → catalog |
| Resume / Discard on the prompt | Resume → route summary; Discard → catalog | **Pass.** `Idle + RestoreRoute(Museo del Prado) -> RouteReady(Museo del Prado)`; Discard (in Spanish) → catalog |
| Mode "Walking", sample route, Start | Arrow and distance counting down about 1.4 m/s; details below the fold; no route summary in the back stack | **Pass.** 224 → 216 → 208 m every ~6 s; scrolling shows the instruction, "987 m · 12 min" and Stop with the scroll indicator |
| Keep watching up to a turn | 0 m at the corner, the next maneuver ~5 m later | **Pass.** "Turn right, 0 m" → "Turn left, 115 m" (the 120 m leg minus 5 m) |
| Let it reach the end | Arrival screen; `Arrive -> Arrived`; cache deleted | **Pass.** "You've arrived · Museo del Prado · New search"; `no_backup` empty |
| "New search" | Search; `Arrived + Finish -> Completed` | **Pass.** Places search opens; `Arrived(…) + Finish -> Completed(…)`. Search sends `Cancel` / `StartSearch` when a search is made |
| Mode "Fast (5 m/s)" | The whole route in a few minutes | **Pass.** All five maneuvers in order, arrival in ~3.5 min |
| Mode "Noisy GPS" | Distance never goes up; one switch per maneuver; no false arrival | **Failed, fixed, pass.** First run: no increase, but the turn switched from 12 m straight to the next maneuver (progress ratcheted ahead by the noise; see Decisions). After the filter: 81 samples, no increase, "0 m" at the corner, then "Turn left, 114 m" |
| Mode "Weak GPS" | "Weak GPS signal" after 10 s of bad fixes, gone at the next good fix | **Pass.** Shows 30 s in and clears 5 s later, every 40 s |
| `svc wifi disable` + `svc data disable` while navigating | "Works offline"; with weak GPS at the same time, only the weak notice | **Pass.** "Works offline" within the 3 s grace; alternates with "Weak GPS signal", which takes precedence, every 40 s |
| Stop → "Keep going" / Stop → "Stop" | Keep going: still navigating. Stop: catalog, `Cancel -> Idle`, cache cleared | **Pass.** `NavigationActive(Museo del Prado) + Cancel -> Idle`; `no_backup` empty; the next cold start goes to the catalog |
| Swipe back from navigation, then catalog → "Navigation" | Navigation kept going; no route summary | **Pass.** Back → catalog; reopened at 139 m (it was 177 m) |
| Language `es`, dark mode | Spanish texts and locale distances | **Pass.** "1,0 km · 13 min", "Gira a la derecha por la Calle del Príncipe", "¿Detener la navegación?" / "Detener" / "Seguir", "Has llegado" / "Nueva búsqueda"; dark theme on every screen. Settings restored afterwards |
| Release build | No simulator selector, no sample route | **Pass (APK check, not installed).** The release dex contains neither "Carrera de San Jerónimo" nor `SimulatableLocationSource`. The release APK is unsigned, so it was not installed |

**Not verified on a device:** square (there is no square AVD; covered by the screenshot tests), a
physical watch and real GPS, the crown (the emulator was scrolled by swipes), TalkBack, and the
app in the background for long (step 10).

**Found but out of this step's rule:** the route summary (step 05) still has a line cut by the
bottom edge on the first view ("120 m" on small round and square, the instruction on large round;
visible in the review sheet and on the emulator). Resume and arrival have their last button partly
under the edge on some sizes. They are scrolling lists; moving them to `GlanceScreenTemplate` is a
small change, pending a decision.
