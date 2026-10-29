# Step 09 — Haptic engine

## Goal

Turn the navigation cues into vibration. The rest of the app speaks only in semantic haptic events
(`TURN_LEFT`, `TURN_RIGHT`, `CONTINUE`, `TURN_IMMINENT`, `ARRIVAL`, `OFF_ROUTE`, `BACK_ON_ROUTE`,
`POI_SUGGESTION`), and one engine decides what plays, when and how. Product document §11–13. There
is no physical watch until the end of version 1 and the emulator doesn't vibrate, so everything is
checked with unit tests, the `HaptiHaptics` log and an on-screen indicator; physical calibration is step 14.
Full reference: [`ARCHITECTURE.md`](../../ARCHITECTURE.md), "Haptic Engine (step 09)"; components:
[`DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md) §2 (haptic language) and §14.

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 10 foreground service | done: `engine.follow(…)` and `recalculator.follow(…)` moved into the service; `haptics.follow(…)` stays in `HaptiMapApplication` (the bench needs it without navigation); screen-off vibration is in the checklist below, section 6 |
| 13 haptic tutorial | `HapticPatternCard`, `HapticEngine.play(event)` and `HapticEngine.played` |
| 14 calibration | `HapticTokens` (patterns), `HapticEngine.MIN_GAP`, the checklist below |
| 25 settings | `HapticEngine.intensity` (in memory now); an optional "respect Do Not Disturb" setting |
| version 2 POIs | `POI_SUGGESTION` is defined and drawn but never emitted; the intelligence layer brings its own adapter to `Flow<HapticEvent>` |

## Decisions

| Decision | Why |
|---|---|
| `HapticEngine` accepts only `HapticEvent` (`follow(events, active)`, `play(event)`); the `NavigationCue` → event mapping is a separate adapter, `HapticCues` | Agreed. Version 2 POI suggestions come from the intelligence layer through their own adapter, without the engine depending on that module. `HapticCues` is the only file in `:core:haptics` that imports `:core:navigation`; navigation never sees haptics. |
| Patterns stay in `HapticTokens` (`:core:model`), as data | One place, like design tokens; step 14 retunes them without touching logic. The bench, the indicator and the player all read them from there. |
| Left = 2 slow long pulses (200 / 250 / 200 ms), right = 3 fast short ones (80 / 100 ms) | The document's counts, plus rhythm: a left pulse is 2.5× longer, so "taaa — taaa" vs "ta-ta-ta" can be told apart without counting, and it survives a vibrator without amplitude control. **First thing to test on a real watch.** |
| `BACK_ON_ROUTE`: one 400 ms soft swell (@80 → 120 → 160, no gaps) | Soft and continuous: not the short single tap of `CONTINUE`, not the separate pulses of a turn, and much weaker and shorter than `OFF_ROUTE` (900 ms @255). |
| `TURN_IMMINENT`: 4 pulses growing in strength **and** length (40 → 150 ms) while gaps shrink | Still progressive without amplitude control. |
| Approaching → direction; Imminent → `TURN_IMMINENT`; Passed, Approaching(arrival), Imminent(continue) → nothing; `Rerouted` → `BACK_ON_ROUTE` | Silence confirms a turn; `Arrived` already announces the end; a straight maneuver needs no urgency. `Rerouted` → `BACK_ON_ROUTE` agreed: "you're being guided again"; the new route's Approaching follows. |
| **Direction is never lost** (correction from the review) | If `ManeuverImminent` arrives for a turn whose direction hasn't played (both thresholds in one fix, or `Approaching` dropped by collapsing or the pending slot), the direction plays instead of `TURN_IMMINENT`: direction carries more information than urgency. Tracked per `stepIndex` with the engine's `playCount`: the adapter knows what was *felt*, not only what was asked for. If the direction is still waiting, the second request collapses into it. |
| Priority `OFF_ROUTE` = `ARRIVAL` > `BACK_ON_ROUTE` > `TURN_IMMINENT` > turn / continue > POI | Losing the route or arriving matter most (and can't happen together). The direction cedes to urgency only in collapsing; the rule above brings it back. |
| Collapse window 100 ms, ties → newest | The engine's cues of one fix arrive with `tryEmit` within microseconds and carry no "end of update" marker; a tiny window collapses them without touching `:core:navigation`. |
| Never cut, never overlap; 600 ms of silence between two patterns | Two patterns back to back read as one unknown pattern. What arrives meanwhile is collapsed the same way when the engine is free (effectively one pending slot). Measured cost: a queued pattern also waits the 100 ms window. |
| Stop: `hapticsActive` false (`Cancel`, `Finish`) cancels the vibrator and drops the queue; `Arrived` keeps it | The arrival pattern is never cut by the session moving to `Arrived`. |
| Weak GPS: nothing extra | The tracker already drops poor fixes and emits no cue without a good one, so no unreliable cue reaches the engine. |
| Usage `ALARM` (`VibrationAttributes` API 33+, `AudioAttributes` below) | Agreed. Navigation the user started keeps vibrating in the default Do Not Disturb and with the app in the background (step 10). `NOTIFICATION` would be silenced by DND and theater mode. Trade-off: it ignores the notification-vibration switch. If the user turns alarm vibration off, the system silences it and the app doesn't fight it. **Step 25 may add a setting to respect DND instead.** |
| `VibratorManager.defaultVibrator` (API 31+) / `Vibrator` (API 30); capabilities read once and logged; no amplitude control → `createWaveform(onOffTimings, -1)` | The fallback merges contiguous non-zero segments into one pulse. |
| `VIBRATE` declared in `:core:haptics`'s own manifest | The library uses the vibrator; lint (`MissingPermission`) requires it there. It merges into `:app`'s. |
| One log tag `HaptiHaptics` (java.util.logging, like the other `Hapti*` tags) | `play event=… pattern=…ms amp=… at=HH:mm:ss.SSS [dropped=[…]]`, `cancel`, and `vibrator=… amplitudeControl=… sdk=…`. An emulator run can be verified from logcat alone. |
| Intensity: `HapticEngine.intensity` × amplitudes, clamped to 1–255 | Ready for step 25; a pulse never disappears at a low setting. In memory for now. |
| Bench and indicator | The bench (`HapticsDebugScreen`) asks the **engine**, not the vibrator, so it follows the same rules and log. The bench class is in `main` like the other debug screens, but its catalog button and route exist only when `BuildConfig.DEBUG`. The navigation indicator (`DebugHapticOverlay`) lives in `src/debug`; `src/release` has an empty composable. |
| Waveforms share one time scale in the bench (the longest pattern) | Lengths compare at a glance: left is visibly longer than right, imminent visibly grows. |

## Acceptance criteria

1. Only semantic events outside `:core:haptics`; nothing else touches the `Vibrator`. Pure core tested on the JVM; thin Android player behind `HapticPlayer`, fake for tests. Wired in `:app`; no navigation → haptics or haptics → UI dependency.
2. The haptic language of §12 as data in one place, with `BACK_ON_ROUTE`, a global intensity multiplier, and a rhythm that separates left from right.
3. Anticipation (§13): direction at Approaching, progressive pulses at Imminent, direction never lost; priority, collapsing, minimum gap, no overlap; cancellation when navigation stops.
4. Android playback with amplitudes or a timing-only fallback, capabilities logged once, `ALARM` usage, every played pattern logged under one tag.
5. Debug haptics bench in the catalog and an indicator while navigating (not in release), from design-system components; English/Spanish, light/dark, 12 screenshot combinations; review sheet regenerated.
6. Tests: mapping for every cue and maneuver; scheduling; intensity and fallback; a full simulated walk with a detour and a reroute.
7. All tests pass, lint 0 errors, no secrets in git; emulator run recorded.

## Verification (2026-10-28)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass** | `grep -rl 'Vibrator\|VibrationEffect'` in Kotlin sources: only `AndroidHapticPlayer` (and a comment in `HapticPattern`). `:core:haptics` depends on `:core:model` and `:core:navigation` (pure JVM) only; `HapticEngine` imports nothing from navigation. Wiring in `HaptiMapApplication` and `di/HapticsModule`. |
| 2 | **Pass (unit + screenshots)** | `HapticPatternTest` (4): intensity scales, keeps silences, clamps to 1–255; on/off fallback merges contiguous segments and drops a trailing pause; pulse counts of the language (left 2, right 3, continue 1, off route 1, back on route 1); a left pulse ≥ 2× a right one. |
| 3 | **Pass (unit)** | `HapticEngineTest` (8): one update collapses to the most relevant; ties → newest; a pattern is never cut and the next waits duration + 600 ms (+ the 100 ms window); one pending slot keeps the highest priority; nothing ever closer than the gap; stopping cancels and drops the pending one, and plays again after restarting; `Arrived` doesn't cut arrival, `Completed` does; the bench plays without navigating, with the intensity applied. `HapticCuesTest` (5): every cue × maneuver; **Approaching + Imminent in one update → the direction once; Imminent after a dropped Approaching → the direction; the normal case → direction at 18 m then `TURN_IMMINENT` at 6 m**; Imminent while the direction is still waiting → no repeat. |
| 4 | **Pass (emulator log)** | `vibrator=true amplitudeControl=true sdk=37`, then one `play` line per pattern, below. The fallback path is unit-tested; this emulator reports amplitude control, so it didn't run here. |
| 5 | **Pass (screenshots + emulator)** | 60 new references: `HapticWaveforms`, `HapticPatternCards`, `HapticCueBadgeSample` (`:core:designsystem`), `HapticsBench`, `HapticsBenchPlaying` (`:app`), × 12. Review sheet: two new rows. Emulator: bench and indicator screenshots below. |
| 6 | **Pass (unit)** | `HapticWalkTest`: the real `NavigationEngine` + store + `HapticCues` + `HapticEngine` wired as in `:app`, a route left · right, a detour west and back, a second detour that ends in a recalculated route with a left turn, walked to arrival at 1 fix/s: exactly `TURN_LEFT, TURN_IMMINENT, OFF_ROUTE, BACK_ON_ROUTE, TURN_RIGHT, TURN_IMMINENT, OFF_ROUTE, BACK_ON_ROUTE, TURN_LEFT, TURN_IMMINENT, ARRIVAL`. |
| 7 | **Pass** | `testDebugUnitTest :core:model:test :core:navigation:test validateDebugScreenshotTest lintDebug --rerun-tasks`: all green, 0 failures; lint 0 errors in every module (`:app` 25 warnings, unchanged). `git diff` scanned for `AIza[0-9A-Za-z_-]{35}`: 0. One earlier full run (`test validateDebugScreenshotTest lint`) failed while the emulator was crashing on the same machine; the cause was not captured and it did not reproduce in two later runs, the second with `--rerun-tasks`. |

## Emulator run with the simulator (2026-10-28)

Emulator `Wear_OS_Small_Round` (192 dp, API 37), debug build, no API key, the sample route from the
catalog (1.0 km, four turns, a straight step, then the arrival). `adb logcat -v epoch -s HaptiHaptics
HaptiSession HaptiReroute`; times are seconds after `StartNavigation`. The emulator reports
`vibrator=true amplitudeControl=true sdk=37` and doesn't vibrate; the log and the indicator are the evidence.

**Run 1, "Leave and return"** (detour at 60 m and back, no request):

| t (s) | Session | Haptic |
|---|---|---|
| 66.1 | `LeaveRoute → OffRoute` | 66.2 `OFF_ROUTE` |
| 75.1 | `ReturnToRoute → NavigationActive` | 75.2 `BACK_ON_ROUTE` |
| | turn 1, right | 195.3 `TURN_RIGHT`, 203.3 `TURN_IMMINENT` |
| | turn 2, left | 281.4 `TURN_LEFT`, 289.4 `TURN_IMMINENT` |
| | turn 3, right | 466.6 `TURN_RIGHT`, 475.6 `TURN_IMMINENT` |
| | turn 4, left | 531.7 `TURN_LEFT`, 539.7 `TURN_IMMINENT` |
| | straight step | 731.8 `CONTINUE` |
| 776.8 | `Arrive → Arrived` | 776.9 `ARRIVAL` |
| — | `Cancel` (leaving the arrival for run 2) | `cancel` |

**Run 2, "Leave route"** (300 m out, recalculated with the debug fake route):

| t (s) | Session | Haptic |
|---|---|---|
| 66.1 | `LeaveRoute → OffRoute` | 66.2 `OFF_ROUTE` |
| 76.1 | `Recalculate`, `Request #1` | — |
| 78.1 | `RouteRecalculated → NavigationActive` | 78.2 `BACK_ON_ROUTE` (from `Rerouted`) |
| | the new route's turn, right | 567.6 `TURN_RIGHT`, 575.6 `TURN_IMMINENT` |
| 858.8 | `Arrive → Arrived` | 858.9 `ARRIVAL` |

**Result: pass.** Every cue produced exactly the expected event, each pattern 0.1 s after its cue
(the collapse window), nothing was dropped or repeated, and leaving the arrival logged `cancel`.
At 1.4 m/s the direction comes 8–9 s before `TURN_IMMINENT`, i.e. ~13 s and ~4 s before the corner.
A third, shorter run in "Fast (5 m/s)" put them only 2.0 s apart (440 ms right + 600 ms gap + 100 ms
window fit), the case where a speed-scaled anticipation (step 14) matters.

**Screens.** The bench plays each card and fills its waveform while it plays. The navigation indicator
appeared for every pattern of run 1 (off route, back on route, four turns with their imminent, continue).
**Found and fixed during the run:** at `BiasAlignment(0, 0.55)` the badge covered the lower half of the
distance ("17 m"); it moved to 0.72, under the distance, with its width capped (0.74 of the screen) and
the name ellipsized, and the compact waveform went from 44 to 36 dp so "Turn right" fits whole. Checked
again on the emulator in "Fast (5 m/s)".

**Not verified on a device:** any real vibration (no watch; the emulator doesn't vibrate), the
timing-only fallback on a vibrator without amplitude control (unit-tested), Do Not Disturb and theater
mode, the screen off (step 10), Spanish and dark mode of the bench on the emulator (covered by the 12
screenshot combinations), and TalkBack on the bench and the indicator.


## Real-watch calibration checklist for step 14

Run each item on the watch, walking where it applies. Use the bench (catalog → Haptics) for the
patterns and the simulator plus a real walk for timing. Record the result next to each item; change
only `HapticTokens`, `HapticEngine.MIN_GAP` and `NavigationThresholds`.

**1. Left vs right (first, before anything else)**
- [ ] Blind test: someone else plays left or right at random from the bench, 20 times; the wearer answers. Target ≥ 19/20, also walking.
- [ ] If they are confused: first widen the rhythm (longer left pulse, shorter right gap), only then consider changing counts.
- [ ] The same blind test with the watch on the other wrist.

**2. Each pattern, felt**
- [ ] `TURN_LEFT`, `TURN_RIGHT`: noticed while walking, on a loose strap.
- [ ] `CONTINUE`: noticed, and clearly weaker than a turn (not mistaken for a turn).
- [ ] `TURN_IMMINENT`: reads as "increasing", not as 4 equal taps.
- [ ] `ARRIVAL`: long + double tap distinct from `OFF_ROUTE`.
- [ ] `OFF_ROUTE`: urgent without being unpleasant; noticed in a pocket-free walk with traffic noise.
- [ ] `BACK_ON_ROUTE`: the swell is felt as one soft event, not as nothing, and not mistaken for `CONTINUE` or `TURN_LEFT`.
- [ ] `POI_SUGGESTION`: distinct from all of the above (version 2 will use it).
- [ ] The log says `amplitudeControl=true`? If not, repeat all the above (timing-only fallback).

**3. Intensity**
- [ ] The softest amplitude (@80) is felt at all; if not, raise `SOFT` before anything else.
- [ ] `intensity` 0.5 and 1.5 (from code until step 25): still distinguishable at 0.5? Unpleasant at 1.5?

**4. Timing at walking pace**
- [ ] Direction at 18 m (~13 s before the turn at 1.4 m/s): early enough to decide, not so early it's forgotten.
- [ ] `TURN_IMMINENT` at 6 m (~4 s): arrives **before** the corner with real GPS lag, not after.
- [ ] **Is the direction-less `TURN_IMMINENT` enough at 6 m, or should it repeat the direction rhythm?** (e.g. the left/right rhythm at higher strength). Ask after a walk with several turns.
- [ ] 600 ms gap: two patterns in a row (bench: tap two cards quickly) never feel like one.
- [ ] Off route → back on route (simulator "Leave and return"): the two are clearly different and not too close.

**5. System modes**
- [ ] Do Not Disturb on: patterns still play while navigating (`USAGE_ALARM`).
- [ ] Theater mode / bedtime mode: what happens? Record it; decide for step 25 whether to respect it.
- [ ] Alarm vibration turned off in system settings: patterns are silent (the app must not fight it).
- [ ] Screen off (step 10's service): patterns still play.
- [ ] Battery saver: patterns still play.

**6. Screen off and battery (step 10's foreground service; see [step 10](10-servicio-en-primer-plano.md))**
- [ ] Screen off (palm or timeout), walking a real route: every turn pattern plays **on time** (compare the `HaptiHaptics` `at=` with the corner). This is the check for the partial wake lock.
- [ ] Same, in ambient mode and after 30+ min with the screen off (real Doze, not `force-idle`): fixes, cues and a reroute still arrive.
- [ ] Battery drain per hour of navigation (screen mostly off): record % per hour from `dumpsys batterystats` and the wake-lock time; compare with a run without the app. Decide whether step 23 must release the wake lock between fixes.
- [ ] Ongoing Activity on the real watch face: the icon appears, its status text updates (at most every 5 s while walking), tapping it opens the navigation screen; also from recents. Check the icon in the watch face's light and dark styles.
- [ ] `POST_NOTIFICATIONS` denied: navigation and vibration unaffected; the launcher returns to navigation.

**7. Search Mode (step 11; see [step 11](11-modo-busqueda.md))**
- [ ] Voice search with the real microphone: which app answers `RECOGNIZE_SPEECH` on the watch (Google's recognizer or Gboard), and how long from the tap to listening.
- [ ] Recognition accuracy outdoors, walking, with traffic and wind: a street address, a business name, a landmark, in English and in Spanish. Note what comes back wrong (numbers, accents, proper names).
- [ ] If the recognizer offers a keyboard (Gboard on the emulator did): typed text arrives in the app. On the emulator it came back empty (`EXTRA_RESULTS=[]`), so typing there does nothing.
- [ ] Outdoor readability in sunlight, light and dark: category icons and labels, the offline notice (`warning` on `surface`), the "Searching…" spinner, and the result rows' third line (address, `caption`).
- [ ] The first view of search on the real screen size: the voice button whole, at least two categories visible; with a long destination in the active route row.
- [ ] Real Places with a key: category results nearby and their distances against the map; a dictated text search that only matches far away (the 3 km bias).
- [ ] A crown scroll through the seven categories is comfortable, and a tap on a moving wrist doesn't hit the wrong row.

## Pending and ideas for step 14

- The checklist above, especially left vs right and the imminent timing.
- Whether `TURN_IMMINENT` should carry the direction (see the checklist). The adapter already tracks
  direction per maneuver, so this is a change in `HapticCues` and `HapticTokens` only.
- Scale anticipation with speed (product document §13): the cue distances are in `NavigationThresholds`.
- A bench intensity control for calibration, if tuning from code turns out slow.
- `intensity` and the DND choice persisted with the step 25 settings.
