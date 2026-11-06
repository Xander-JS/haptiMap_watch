# Step 15 — Version 1 close-out

## Goal

Close version 1 so it can go to internal testing without a physical watch. **15A** (this step) is everything that
can be done and checked on the emulator: a design-system rule, up-to-date dependencies, accessibility at the
largest font size, a battery review, the bug fixes from steps 10–13 that need neither a watch nor a decision, and
the release setup (signing, version, keys). **15B** is pending: it needs the physical watch (the step 14 calibration
checklist) and the internal-testing upload itself.

## 15A parts

| # | Part | Status | Commit |
|---|---|---|---|
| 1 | Design-system rule: an edge-button label is one or two words | done | `904216e` |
| 2 | Dependencies to current stable (AGP 9.5.1, Gradle 9.8.2, Kotlin 2.4.21, Compose BOM 2026.09.00, OkHttp 5.5.1…) | done | `8b46b01` |
| 3 | Accessibility: the main flow at the largest font size (1.24) | done | `b8971e8` |
| 4 | Battery: release the CPU wake lock ~5 s after arrival + the review below | done | `38b6612` |
| 5 | Bug fixes (steps 10–13 open items) | done — none fixable without a watch or a decision (see Bug review) | — |
| 6 | Release setup (signing, version, DeepSeek key) | done | `92b369d` |
| 7 | Docs | done | this commit |

## Decisions

### Release (confirmed)

- **Signing.** The upload keystore is generated **outside** the repo at `~/keystores/haptimap-upload.jks` with a
  random password. `keystore.properties` at the repo root points to it; both `keystore.properties` and `*.jks` are
  git-ignored, so no keystore or password is ever versioned. Without the file, `release` builds **unsigned** with a
  clear Gradle warning (`HaptiMap: sin keystore.properties…`). See [ARCHITECTURE.md, Version 1 close-out](../../ARCHITECTURE.md).
- **Version.** `versionName = "1.0.0-rc1"`, `versionCode = 1`.
- **DeepSeek key.** `DEEPSEEK_API_KEY` is `""` in `release` (overrides `defaultConfig`); `debug` keeps reading it
  from `local.properties`. The Maps key is unchanged (empty when not provided); the release run is without a key.
- **Release emulator run** covers first launch, the tutorial and the no-key search message with no crash, and that
  no debug surface shows. The navigation flow is covered by the debug build with the fake APIs.

### Battery review

Static review of the four draws the step 15A prompt asked about. Numbers while navigating:

| Draw | Now | Verdict |
|---|---|---|
| GPS interval | 1 Hz, `PRIORITY_HIGH_ACCURACY` (`DEFAULT_INTERVAL_MILLIS = 1000`), 60 fixes/min | Deliberate for pedestrian turn-by-turn; adaptive sampling is already earmarked for step 23. Not changed. |
| Wake lock | Held only while the service runs; released ~5 s after arrival (was 60 s); 6 h safety cap | **Fixed this step** (`38b6612`): the only clearly wasteful item. |
| Redraws — interactive | Driven by fixes (≤ 60/min) and only when a shown value changes; no always-on animation or polling loop in the nav UI. The notification is throttled to ≤ 12/min (one post per 5 s for distance). | Fine. |
| Redraws — ambient | ~0 from fixes: `AmbientLifecycle` caps the screens under the ambient view at `CREATED`, so they stop collecting their `StateFlow`s. The ambient view updates on the watch's ambient tick (~1/min). | Fine. |
| Network calls per route | 1 `computeRoute` at route start, then recalculations only on off-route, bounded: ≥ 10 s initial delay, ≥ 15 s between requests, exponential backoff to 60 s, max 4 auto per off-route episode. No polling. | Fine. |

### Bug review (steps 10–13 open items)

Every open item in the "Still open / Pending / Design questions" sections of steps 10–13 is **already decided or
done**, **deferred to a later step/version**, or **needs the physical watch**. None is a currently-broken behavior
fixable on the emulator without a decision, so part 5 added no code.

**Needs the physical watch (step 14 checklist, [step 09 §6–9](09-motor-haptico.md#real-watch-calibration-checklist-for-step-14)):**

- Step 10: wake-lock timing with the screen off; screen-off/ambient vibration over a real walk incl. 30+ min Doze;
  battery drain and wake-lock time from `dumpsys batterystats`; the Ongoing Activity on the real watch face.
- Step 11: the real microphone and which recognizer answers; outdoor accuracy walking (both languages); typed text
  through the watch's recognizer; outdoor readability; the first view on the real screen; real Places with a key;
  crown scrolling and taps while walking.
- Step 12: the real AOD look (dimming, gray, outlines, low-bit halos); burn-in; ambient battery cost; outdoor
  ambient readability; how often the real watch lets an ambiactive app redraw.
- Step 13: pattern distinguishability on a real wrist; whether users tell left from right after the tutorial;
  vibration turned off in system settings.

**Deferred ideas (parked, not blocking V1):**

- Step 10: a stationary user's notification distance can be up to 5 s stale — an optional timer; low priority.
- Step 10: a navigation longer than 6 h loses the wake lock → step 23.
- Step 11: the category order could become usage-based → later.
- Step 12: Live Updates (Wear OS 7+) as the Ongoing Activity's successor → version 2.

## What 15A built, per layer

- **`:core:designsystem`** — `HaptiEdgeButton` auto-sizes its label down instead of truncating; `clockBandTop(offline)`
  grows the clock band when the offline notice shows; `CenteredScreenTemplate` scrolls when content overflows;
  `@HaptiLargeFontPreviews` (`LARGEST_FONT_SCALE = 1.24`). See [DESIGN_SYSTEM §20](../../DESIGN_SYSTEM.md).
- **`:core:navigation`** — `ARRIVED_WAKE_LOCK_LINGER = 5 s`; `navigationServiceRuns` reused with it.
- **`:app`** — the service holds the CPU wake lock on the 5 s policy; `EdgeButtonLabelTest` enforces the label rule
  across every module's `HaptiEdgeButton` call; release signing / version / DeepSeek in `build.gradle.kts`.
- **Build** — dependency bump (`8b46b01`); `keystore.properties` + `.gitignore`.

## Verification (2026-11-06)

_To be filled: unit + screenshot tests, lint (0 errors per module, `:app` warnings vs. 22)._

## Emulator run (2026-11-06)

_To be filled: the release run (no key), the largest font size on all three shapes, TalkBack on the main flow._

## Pending: 15B

- The whole step 14 calibration checklist on the physical watch ([step 09](09-motor-haptico.md#real-watch-calibration-checklist-for-step-14)).
- Generate a fresh upload keystore on the release machine (the one here is this dev box's) and keep the password in a
  password manager; register it with Play App Signing on the first upload.
- Build the signed release bundle and upload it to internal testing.
