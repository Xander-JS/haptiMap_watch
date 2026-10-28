# Step 08 — Off route and recalculation

## Goal

Detect when the user leaves the route and warn them. Recalculate a walking route from where they are
to the same destination. If they come back by themselves, resume without recalculating. Emit semantic
cues for the haptics of step 09. Full reference: [`ARCHITECTURE.md`](../../ARCHITECTURE.md), "Off route
and recalculation (step 08)"; screen: [`DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md) §13.

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 09 haptics | `NavigationCue.OffRoute` (product document §12: long intense pulse), `BackOnRoute`, `Rerouted`; suggested: one soft pulse for `BackOnRoute`, nothing extra for `Rerouted` (the next maneuver's cue follows) |
| 10 foreground service | moves `recalculator.follow(…)` out of `HaptiMapApplication` with the engine |
| 14 tuning | the off-route and recalculation values in `NavigationThresholds`; the ideas below |

## Decisions

| Decision | Why |
|---|---|
| Off fix: `crossTrack > 20 m + min(accuracy, 10 m)`; declared after 5 s **and** 3 consecutive good off fixes | Accuracy-aware, like arrival. Needing both time and fixes means a burst of fixes can't declare it in under 5 s, and neither can two fixes 10 s apart. Agreed in the plan. |
| Rejoin: `≤ 12 m + min(accuracy, 10 m)` for 2 fixes (hysteresis) | A lower bar than leaving, so it doesn't flap at the edge. |
| Poor fixes (> 20 m) neither count nor break a streak; the weak-signal flag resets both streaks | Nothing is declared from fixes taken before a gap. Agreed. |
| Off fixes (and any fix while off route) don't move progress or fire maneuver cues; the progress filter restarts from the next on-route fix | A wander past a corner must not mark the turn as done. After 60 s off route the filter's along-route speed would have predicted a jump of 80 m. Arrival is still checked on an undeclared off fix: overshooting the end by 30 m is an off fix, and it must still arrive (step 07 test). |
| While off route, the matching window keeps growing ahead (≤ 100 m) | A user who rejoins a little further on is found. The filter and window now count from the last fix that moved progress (`lastMatchedMillis`), and the weak signal from the last good fix. On the route both are the same, so no step 07 test changed. |
| Wrong direction: no rule in this step | The 15 m backward window already makes walking back off route after about 40–55 m (tested). A faster rule based on heading needs field data on how noisy fix bearings are: step 14. Agreed. |
| `RouteRecalculator` in `:core:navigation`, taking `suspend (GeoPoint, Place) -> RouteResult` | No `navigation → network` dependency. `:app` passes `RoutesRepository.computeRoute`. Agreed. |
| `ReturnToRoute` also from `Recalculating` | A rejoin cancels the recalculation (`collectLatest`). A late `RouteRecalculated` is ignored by the reducer, as it already ignored any event that doesn't apply. Agreed. |
| **Recalculation delay 10 s** (the plan said 5 s) | Measured while tuning the simulator. At 1.4 m/s, a user declared off route (5 s beyond 25 m) needs at least 8 s to get back inside the 17 m rejoin line. With 5 s, nobody could rejoin before the request: "leave and return" was identical to "return late". With 10 s, someone who turns back at the alert rejoins with no API call. Asked and agreed during implementation. The alert still comes at about 5 s; the first request at about 15 s after leaving. |
| Spacing: ≥ 15 s between any two requests; failures back off 15 → 30 → 60 s; ≤ 4 automatic requests per episode; `MISSING_KEY`, `API_INVALID_KEY`, `API_QUOTA` stop them at once | No storms however the user wanders, and a hard ceiling. Retry always works but respects the 15 s gap. |
| Offline: no request and no session change (`OffRoute`, old route kept); waits for connectivity, then asks at once. Also waits while GPS is weak | The origin would be stale. Offline time doesn't count toward the 4 attempts. A Retry while offline gives `OffRoute(NO_CONNECTION)` without a request. |
| Origin: the last good fix (`NavigationProgress.position`) | The tracker smooths along the route, which means nothing off it. |
| Every request logged under `HaptiReroute` | `Request #n (episode e, auto a/4) from … to …`, then the length, the error, or `cancelled: back on route`. |
| Cues `OffRoute`, `BackOnRoute`, `Rerouted`, once per episode | The tracker emits the first two from its streaks. The engine emits `Rerouted` when the route it follows changes without leaving navigation, which only `RouteRecalculated` can do. |
| Screen precedence: recalculating > offline > error (+ Retry) > head back; off route only the weak-GPS pill | See DESIGN_SYSTEM §13. Offline went above error compared with the plan: Retry can't work offline, and the reconnect retries by itself. |
| No arrow while off route | Without the watch heading we don't know which way the route is. An icon (`ic_off_route`) marks the state instead. |
| Debug without a key: `rerouteRequest()` (`src/debug`) builds an L-shaped route to the destination after 2 s; `src/release` always calls Google | Demos a full recalculation without a key. The fake's wait **can't be cancelled**, like a response that is already arriving, so the emulator shows the reducer ignoring a late `RouteRecalculated`. The release dex has no `debugReroute` (checked). |
| `ConnectivityModule` per build type; debug forces `Disconnected` while "Leave offline" asks for it | Same pattern as `LocationModule` (step 07). Release is unchanged. |
| Simulator detours (`Detour`, `withDetour()` in `:core:location`) | A perpendicular exit to the left at 60 m. Applied only to the route the session started with (the one in `RouteReady`); a recalculated route is walked as is. Modes and their timing are tested in `OffRouteSimulationTest` with the real configuration. |

## Simulator modes

All of them leave the sample route at 60 m (43 s) at 1.4 m/s. Times are from Start.

| Mode | Detour | Expected |
|---|---|---|
| Leave route | 300 m out, stays there | OffRoute ≈ 66 s, request ≈ 76 s, new route ≈ 78 s, then walks the new route |
| Leave and return | 30 m out and back | OffRoute ≈ 66 s, back ≈ 75 s, **no request** |
| Return late | 31.5 m out and back | OffRoute ≈ 66 s, request ≈ 76 s, back ≈ 77 s with the request in flight, late result ignored |
| Leave offline | 300 m out, offline for 45 s from the exit | OffRoute ≈ 66 s, offline notice, request when online again ≈ 88 s, new route ≈ 90 s |

## Acceptance criteria

1. Off route from `crossTrackMeters` with an accuracy-aware threshold and hysteresis; never on poor fixes or with weak GPS.
2. Rejoining before a new route arrives cancels the recalculation and goes back to `NavigationActive`.
3. Recalculation from the current position to the same destination, with spacing, backoff and a maximum, logged; offline waits and retries on reconnect; API and key errors keep the route and offer Retry.
4. On success: route replaced, cache updated, tracker reset, `Recalculating → NavigationActive` directly.
5. Cues `OffRoute`, `BackOnRoute`, `Rerouted`, once per episode, in order.
6. Off-route and recalculating screens from existing components, English/Spanish, light/dark, 12 combinations, documented precedence.
7. Simulator scenarios and a recalculation demo without a key in debug only.
8. All tests pass, lint 0 errors, no secrets in git.

## Verification (2026-10-28)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass (unit)** | `OffRouteTest` (12): declared at the 6th fix 26 m off and not before; 24 m never; 28 m with 15 m accuracy never; two 4 s excursions never; 2 fixes 9 s apart not, the 3rd yes; 60 m off with 25 m accuracy never, and bad fixes between good ones don't break the streak; weak GPS resets the streak (5 s more after the signal returns); a parallel walk 30 m off past a turn moves nothing and gives no maneuver cue; hysteresis band (20 m stays off route for 25 s); hairpin: cutting across to the other leg is off route and progress doesn't jump; walking back is off route after 40–55 m. All step 07 `RouteTrackerTest` cases unchanged. |
| 2 | **Pass (unit + emulator)** | `RouteRecalculatorTest`: rejoin before the delay (no request); rejoin with the request in flight (cancelled once, state `NavigationActive(old)`, one request); an uncancellable response after rejoining is ignored. `NavigationReducerTest`: `Recalculating + ReturnToRoute → NavigationActive`. Emulator: "Leave and return", "Return late" below. |
| 3 | **Pass (unit + emulator)** | `RouteRecalculatorTest` (11): first request at 15 s from the last good fix to the same `Place`; failures at 15, 30, 60, 120 s then none; Retry right after a failure waits for the 15 s gap; `MISSING_KEY`, `API_INVALID_KEY`, `API_QUOTA` one request then only Retry; offline 3 min no request then one at reconnect; Retry offline not sent; no request while GPS is weak. |
| 4 | **Pass (unit + emulator)** | `RouteRecalculatorTest`: the cache holds the new route; `NavigationEngineTest`: a new tracker on the new route (80 m remaining, not off route); the reducer goes `Recalculating → NavigationActive`, never `RouteReady`. |
| 5 | **Pass (unit)** | `NavigationEngineTest`: an excursion gives `OffRoute, BackOnRoute, Approaching(arrival), Arrived` and sessions `NavigationActive → OffRoute → NavigationActive → Arrived`, with the GPS collected once; reroute gives `OffRoute, Rerouted`. `RouteRecalculatorTest` and `OffRouteSimulationTest` check the same per scenario. |
| 6 | **Pass (unit + screenshots)** | `NavigationViewModelTest` (+5): mapping with the distance to the route; precedence recalculating > offline > failed > head back; only the weak-GPS notice off route; Retry and rejoin; Stop off route. 60 new references (off route, recalculating, offline, failed, weak GPS + failed × 12); none of the earlier ones changed. Review sheet regenerated with three off-route rows. |
| 7 | **Pass (unit + emulator + APK)** | `SimulatedWalkTest` (+1, the detour path); `OffRouteSimulationTest` (4, debug only): each mode's timing window with its real configuration, and the fake route is walkable and arrives. Release dex: no `debugReroute`, no `SimulatableLocationSource`. |
| 8 | **Pass** | `./gradlew clean` then `assembleDebug assembleRelease lintDebug testDebugUnitTest :core:model:test :core:navigation:test validateDebugScreenshotTest`: **216 unit tests** (2 skipped, `RoutesLiveTest`), **552 screenshot tests**. Lint 0 errors in every module; `:app` has 25 warnings, the same kinds as before (2 manifest notes, the rest dependency-version notices, whose count rose when `clean` refreshed lint's cache). `git log -p --all \| grep -cE 'AIza[0-9A-Za-z_-]{35}'` = 0. The first validation after `clean` reported 5 rendering **errors** (no pixel diff, 3 of them in references from earlier steps); `validateDebugScreenshotTest --rerun-tasks` then passed all 552. |

## Device check with the simulator (2026-10-28)

Emulator `Wear_OS_Small_Round` (192 dp), debug build, no API key, the sample route from the catalog.
Driven with `adb` (`uiautomator dump` + `input`), screenshots with `screencap`,
`adb logcat -v epoch -s HaptiSession HaptiReroute HaptiLocation`. Times are seconds after `StartNavigation`.

| Mode | Log | Screen | Result |
|---|---|---|---|
| Leave route | `+66.1 LeaveRoute -> OffRoute`; `+76.1 Recalculate`, `Request #1 (episode 1, auto 1/4) from … to Museo del Prado`; `+78.1 … 1106 m`, `RouteRecalculated -> NavigationActive` | Guidance → "Off route · Head back to the route · 37 m from the route" → "Finding a new route… · 46 m" → "Turn right, 687 m" on the new route | **Pass** |
| Leave and return | `+66.1 LeaveRoute`; `+75.1 OffRoute + ReturnToRoute -> NavigationActive`; no request | "Off route · 23 m" → guidance at "168 m", the same maneuver | **Failed, fixed, pass.** First run (mode changed while the previous rerouted navigation was still running): no detour at all, the walk went straight. The exact path wasn't pinned down, but the "first route only" rule lived in a variable inside `gpsStatus()`, which the engine calls again for every route, so it was fragile; it would also have re-applied the detour to a recalculated route. Now the simulator remembers the route of `RouteReady` and logs `Simulating … detour true/false`; all four modes then behaved as expected |
| Return late | `+76.1 Recalculate`, `Request #1`; `+77.1 Recalculating + ReturnToRoute -> NavigationActive`; `+78.1 Request #1 … cancelled: back on route; its late result is ignored`; `+78.1 Ignored RouteRecalculated(Museo del Prado) in NavigationActive(Museo del Prado)` | "Finding a new route… · 17 m from the route" → guidance on the old route | **Pass.** The request was cancelled (its coroutine), the late result arrived and the reducer ignored it, one `ReturnToRoute` (the engine dispatches one per `BackOnRoute`) |
| Leave offline | `+66.1 LeaveRoute`; no request while offline; `+88.1 Recalculate`, `Request #2 …`; `+90.1 RouteRecalculated`; `Simulating LEAVE_OFFLINE …, detour false` | Before leaving: "Works offline" + "Offline" clock → off route: "New route once online · 37 m" → "Finding a new route… · 63 m" → "Turn right, 692 m" | **Pass.** The recalculated route is walked without the detour |
| Reinstall while navigating | — | The resume prompt (step 06) | **Pass**, as expected |

**Not verified on a device:** Spanish and dark mode on the off-route screens (covered by the 60
screenshot references), square and large round (screenshots), a physical watch and real GPS, a real
Google reroute (no API key on this machine; the request path is the same `RoutesRepository` as step 05),
TalkBack announcing the status changes, and the app in the background (step 10).

## Pending and ideas for step 14

- **Arrow back to the route.** While off route, show an arrow pointing toward the nearest route point,
  if field tests show the watch heading is reliable enough. It needs the compass or the fix bearing,
  and its error measured in real walks; a wrong arrow is worse than the current icon.
- Tune the detection (20 m + accuracy, 5 s, 3 fixes), the rejoin (12 m, 2 fixes) and the 10 s
  recalculation delay with real GPS. Real noise is correlated and biased near buildings, which the
  simulator doesn't reproduce.
- A faster wrong-direction rule based on heading.
- `ConnectivityMonitor.status` shares its upstream `WhileSubscribed(5 s)`. The recalculator subscribes
  while it waits for connectivity, so the value it reads when it sends the request is fresh. With the
  screen off and no waiting (step 10), `.value` could be stale until the first collector; the
  repository's own `IOException → NO_CONNECTION` covers that case.
