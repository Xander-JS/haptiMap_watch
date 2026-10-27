# Step 01 — Location and permissions

## Goal

Ask for location permission and read the watch's real GPS position. This step covers the domain
model (`GpsStatus`), the location engine in `:core:location`, the precise-location permission flow
in `:feature:onboarding`, and a temporary "Location debug" screen that you open from the catalog.

Out of scope, with the hook each later step will use:

| Step | Hook |
|---|---|
| 02 navigation state machine | consumes `LocationSource.gpsStatus()` |
| 03 connectivity | none needed: location does not depend on the network |
| 10 foreground service | collects the same flow from a service instead of a screen |
| 14 calibration | `LOW_ACCURACY_THRESHOLD_METERS` (`core:model/GpsStatus.kt`) |
| 23 adaptive sampling | `gpsStatus(intervalMillis)`, default `DEFAULT_INTERVAL_MILLIS` = 1 s |

## Permission flow

```
launch ─ fine granted? ─yes─► catalog ─► "Location debug" ─► debug screen (collects gpsStatus)
              │no
              ▼
  Rationale / Approximate ─button─► system dialog
       ├ fine granted ─────────────────► catalog
       ├ dialog shown, fine denied ────► Denied / Approximate (button asks again)
       └ no dialog shown (system blocked) ► Blocked / ApproximateBlocked → "Open settings"
                                           (App info; fine is checked again on resume)
```

## Decisions

| Decision | Why |
|---|---|
| `android.hardware.location.gps` stays `required="true"` | The app is standalone and can't fall back to the phone's GPS. Watches without GPS can't run it, so Play should not offer it to them. This triggers lint's **`UnnecessaryRequiredFeature`** warning, kept on purpose (exact text below). |
| Reuse `LocationFix`, `LocationSource` and `:core:location` | They already existed from the scaffolding. `LocationSource.locationUpdates()` (never used) became `gpsStatus(intervalMillis): Flow<GpsStatus>`. No second repository interface was added. |
| Low-accuracy threshold: 20 m | Roughly half a city block: past that, the app can't tell which corner the user is at. To be calibrated in step 14. A fix exactly at 20 m counts as `Fix`. |
| Status logic in `gpsStatusFlow()`, without Android | Both `FusedLocationSource` and `FakeLocationSource` run the same function (permission → services on/off → fixes), so the unit tests cover the real logic. |
| Location services read when collection starts, then on every `PROVIDERS_CHANGED` | If location is already off, `LocationDisabled` is emitted right away. Turning it off cancels the inner FLP flow, so updates stop until it's turned back on. |
| Receiver registered with `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` | Required on API 34+. `PROVIDERS_CHANGED` is a system broadcast, so it still reaches a receiver that isn't exported. |
| Approximate location is not enough | Turn-by-turn guidance needs to know which side of the street the user is on. With only `ACCESS_COARSE_LOCATION` the app shows the "approximate" screen, and the engine reports `NoPermission`. |
| "Blocked" is detected from the system dialog not showing | `shouldShowRequestPermissionRationale(FINE)` returns `false` right after a coarse-only grant, even though the system still offers the upgrade dialog, so it can't tell blocked from askable. Instead: the dialog pauses the activity; if the answer arrives without a pause, the system denied without asking. Checked on the emulator (`wm_on_paused_called` appears only when the dialog is shown). |
| Plain `rememberSaveable` screen state, no navigation library | Three screens, two of them temporary. A back press or swipe on the debug screen returns to the catalog (`BackHandler`). |
| No new design-system components | The screens use `RoundScreenTemplate`, `HaptiButton`, `Text` and the theme tokens. The debug screen's label/value row is private to that temporary screen. |
| No background location | Not needed until the foreground service (step 10), which runs as foreground and does not need `ACCESS_BACKGROUND_LOCATION`. |

### Lint warning kept on purpose

```
app/src/main/AndroidManifest.xml:27: Warning: Consider whether this feature (android.hardware.location.gps) really is required for the app to function; you can set android:required="false" to indicate that the feature is used but not required [UnnecessaryRequiredFeature]
```

## Acceptance criteria

1. On first launch the app shows the rationale screen and requests permission.
2. If permission is denied, the app explains it and offers to try again; if denied permanently, it offers to open the app settings.
3. If location services are off, the app shows a clear message.
4. With permission granted, the debug screen shows a real position, and it updates when a new position is simulated on the emulator.
5. Location updates stop when the debug screen is left (verify with logs).
6. All new screens work in English/Spanish and light/dark, with no hardcoded colors, dimensions or texts.
7. `assembleDebug`, `lintDebug` and the unit tests pass.
8. If only approximate location is granted, the app explains that precise location is needed and offers to request it again, or to open the app settings if it can no longer be requested.

## Verification (2026-10-27, emulator `Wear_OS_Small_Round`, API 37)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass** | Uninstalled, installed and launched: the "Location" rationale screen showed. "Allow location" opened the system dialog. |
| 2 | **Pass** | "Don't allow" → "Without location…" + "Try again". After the 2nd denial the system stops showing the dialog; the 3rd tap shows "Location access is blocked…" + "Open settings", which opens App info in Wear Settings. Granting there and pressing back took the app straight to the catalog. A relaunch while blocked goes to the blocked screen on the first tap. |
| 3 | **Pass** | With the debug screen open, `adb shell cmd location set-location-enabled false` → "Location is off. Turn it on in the watch settings." (log: `Location updates stopped`). Turning it back on → `started`, then the fix again. With location already off, opening the debug screen shows the message right away, and no `started` line is logged. |
| 4 | **Pass** | `adb emu geo fix -3.703790 40.416775` → "GPS fix, Lat 40.416775, Lng -3.703790, Accuracy 5.0 m". `geo fix -0.375 39.4699` → updated to 39.469900 / -0.375000. `geo fix … 650 8 3` (3 knots) → "1.5 m/s". |
| 5 | **Pass** | `adb logcat -s HaptiLocation`: `Location updates started (interval 1000 ms)` on opening the screen; `Location updates stopped` on back key, swipe-to-dismiss and Home (the screen collects with `collectAsStateWithLifecycle`). `dumpsys location` afterwards: `gps provider: service: ProviderRequest[OFF]`. |
| 6 | **Pass** | Screenshots of the rationale (en light, en dark, es dark), approximate (en light, es dark) and debug (en light, es dark) screens. Every text switched language, numbers use "40,416775" in Spanish, and colors follow the theme. Searched every `.kt` file changed in this step for `Color(0x`, `<n>.dp`, `<n>.sp` and `Text("…")`: no matches. |
| 7 | **Pass** | `./gradlew assembleDebug lintDebug testDebugUnitTest`: build OK. Lint: 0 errors, and no new warnings besides `UnnecessaryRequiredFeature`, which is expected. Tests: 5/5 in `GpsStatusTest` (accurate fix, threshold boundary, no permission, location already off, off → on). |
| 8 | **Pass** | Fresh install, "Use precise location" switched off in the dialog → "Approximate location isn't enough…" + "Use precise location", which opens the system "approximate → precise" dialog. "Change to precise location" → catalog (FINE `granted=true`). Choosing "Keep approximate" instead makes the system block further requests; the next tap shows "…Turn on precise location in the app settings." + "Open settings". |

**Notes**

- `geo fix` always reports 5 m accuracy and 0° bearing, so `LowAccuracy` and a real bearing were
  not seen on screen. `LowAccuracy` is covered by the unit tests; a GPX route in extended controls
  gives a real bearing.
- The Wear OS settings app has no "precise" switch on the app's location page, so criterion 8's
  approximate-only grant was made from the permission dialog itself.
