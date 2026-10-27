# Step 00 — Design foundations

## Goal

Set up the design foundation every later step builds on: the `com.xanderjs.haptimap` package,
a reusable design system module (tokens, light/dark theme, Atomic Design components), the
haptic language as data, English/Spanish support, and a temporary catalog screen that shows
everything. Full reference: [`DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md).

## Acceptance criteria

1. The project builds and runs on a Wear OS emulator.
2. The catalog screen shows all components.
3. Changing the emulator language switches all texts (en/es).
4. Toggling the theme switches every component to the other scheme.
5. Changing a single color token is reflected in every component that uses it.
6. No hardcoded colors, dimensions or texts exist outside `designsystem/tokens` and `res/values`.

## Decisions

| Decision | Why |
|---|---|
| Base package `com.xanderjs.haptimap` | One namespace for app and modules (`…core.model`, `…designsystem`). |
| Separate `:core:designsystem` module | Feature modules share tokens and components without depending on `:app`. |
| Haptic patterns in `:core:model` (`HapticPattern.kt`, next to `HapticEvent`) | They are pure data. `:core:haptics` (step 09) plays them without depending on Compose; the catalog reads them from there. |
| Flat design, no elevation token | Wear OS guidelines: hierarchy comes from color and shape, not shadows. The unused `Elevation` token was removed. |
| Per-app language picker on Android 13+ only | `locales_config.xml` drives the system per-app language setting (API 33+). Older watches follow the device language; no in-app picker. |
| `CategoryChip` is a pill button | A `Button` with `shapes.full`; selection shown by accent color and announced via `semantics { selected }`. One visual style, one touch target. |
| `PlaceCategory` values in English (`STORE`, `HARDWARE_STORE`, `RESTAURANT`, `CAFE`, `PHARMACY`, `TRANSIT`, `SUPERMARKET`) | Code identifiers only. Users see `PlaceCategory.label` → `category_*` strings in English and Spanish. |
| Removed unused `ic_launcher_round` | Nothing referenced it (lint `UnusedResources`). The deletion landed inside commit `48ee61b`. |

Left for step 01: whether `android.hardware.location.gps` stays `required="true"`.

## Verification (2026-10-27, emulator `Wear_OS_Small_Round`, API 37, HEAD `d3275cf`)

| # | Result | How it was checked |
|---|---|---|
| 1 | **Pass** | `assembleDebug` and `lintDebug` succeed (lint: 0 errors). `installDebug`, launched; `MainActivity` is the resumed activity. |
| 2 | **Pass** | Scrolled the whole catalog, capturing screenshots and UI dumps. Seen: ManeuverIndicator, 3 DirectionArrows, DistanceText (20 m, 1.5 km), 3 HaptiIcon sizes, primary/secondary HaptiButton, all 7 CategoryChips, 15 color roles, 5 type styles, 5 spacings, 4 shapes, and 7 haptic patterns. The haptic totals (390, 660, 120, 560, 780, 900, 310 ms) match the data in `core:model`. |
| 3 | **Pass** (see note) | Set the app language to Spanish with `cmd locale set-app-locales com.xanderjs.haptimap --locales es`. Every user-visible text switched (e.g. "Sistema de diseño", "Ferretería", "Giro inminente", "2 pulsos · 390 ms"), and so did the decimal separator ("1,5 km"). Only the token identifiers stay English (`translatable="false"`). |
| 4 | **Pass** | Tapped the theme button System → Light → Dark. Background, text, buttons, chips, arrows, swatches and clock all switched to the dark scheme (compared screenshots). |
| 5 | **Pass** | Temporarily set `BasePalette.Teal40` to `#C2185B`. Section headers, the maneuver arrow, the direction arrows, the primary button and the spacing bars all turned magenta. Reverted before committing. |
| 6 | **Pass** | Searched the code outside `tokens/` (`app`, `core/designsystem`, `feature`) for `Color(0x`, `<n>.dp`, `<n>.sp` and literal `Text("…")`: no matches. Every English string has a Spanish version or is marked `translatable="false"`. |

**Note on criterion 3:** the system language could not be changed on this emulator. Wear OS
syncs it from the paired phone (Settings → System → Language only says "Open on phone"), the
image has no root, and the emulator's `-change-locale es-ES` flag was ignored. The per-app locale
goes through the same resource lookup, so the result is the same as a system change.
