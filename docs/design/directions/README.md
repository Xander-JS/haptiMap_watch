# HaptiMap visual redesign: process

How the current look was chosen. The rules it produced live in `DESIGN_SYSTEM.md` §2 and §22.

## 1. Research: what applies to a haptic navigation watch

| Principle | Where it comes from | What it means for HaptiMap |
|---|---|---|
| One key piece of information per screen | Wear OS Material 3 Expressive (non-scrolling screens "optimized for glanceability and focus"); Apple Watch Maps shows only the next turn and keeps the list one tap deeper | The maneuver and its distance *are* the navigation screen; totals and the provider's instruction go to the second level |
| The wrist does the talking | Apple Watch plays distinct tap patterns for left and right | The screen confirms what was felt. That became the identity: the vibration made visible |
| Use the round shape | M3 Expressive edge-hugging buttons, shapes that follow the circle, margins in percentages | Keep the edge button; round discs and rings; nothing cut by the bezel |
| Big numerals, fixed-width digits | Every sports/navigation watch | Condensed bold numerals, `tnum`, the unit at half size |
| Color means one thing | Garmin outdoor units: one color for the course, another for off-course, another for caution | One job per color (see §4) |
| OLED | Wear guidance for AOD | Pure black, bright accents; ambient grey-only, thin, no fills |

Sources: [Material 3 Expressive for Wear OS](https://android-developers.googleblog.com/2025/08/introducing-material-3-expressive-for-wear-os.html),
[Wear OS design quality tiers](https://developer.android.com/design/ui/wear/guides/foundations/quality-tiers),
[Directions in Maps on Apple Watch (watchOS 26)](https://support.apple.com/en-il/guide/watch/apdea7480950/26/watchos/26),
[Garmin route color coding](https://www8.garmin.com/manuals/webhelp/gpsmap8400-8600/EN-GB/GUID-EC4B1BB4-3F3B-4D52-AC7F-1C71A9E079F1.html).

## 2. Audit of the previous look (`sheet_current_dark.png`, `sheet_current_light.png`)

- **No identity**: a stock teal, default Roboto, every button the same teal pill. Focus and Tour looked identical while navigating.
- **Weak hierarchy**: the voice search was styled like the category rows; off route was three equal lines and a small icon.
- **Overlap**: on the map, the maneuver pill covered the clock.
- **Invisible surfaces**: unselected cards were 1.2:1 against black (1.1:1 in light), under the 3:1 a control boundary needs.
- **Too much text**: two-line titles and subtitles, four-line permission paragraph, two-sentence errors, tutorial sentences that repeated the title.

## 3. Three directions (`sheet_A_Pulso.png`, `sheet_B_Senda.png`, `sheet_C_Calma.png`)

Each sheet: navigation (Focus, Tour), POI suggestion, profile selector, search; dark row over light row.
Rendered with the project's Compose screenshot pipeline from `DirectionMockups.kt.txt` (mockups, not app code).

| | A · Pulso | B · Senda | C · Calma |
|---|---|---|---|
| Concept | The vibration made visible: rings around the maneuver | Pedestrian wayfinding signage, topographic map | Walk without looking: the bezel is the instrument |
| Brand | Volt `#D4F53C` / `#3F5600` | Trail orange `#FF7A2F` / `#B04500` | Dusk indigo `#A3AEFF` / `#3647C4` |
| Tour | Orchid | Topo green | Sunset amber |
| Type | Condensed bold numerals | Heavy sans, tight tracking | Light, large numerals |
| Shapes | Discs, rings, pills | 14 dp "sign plates" | Soft 28 dp, progress arc |

## 4. Decision: A · Pulso

Chosen because it is the only direction whose identity *is* the product (haptic guidance), it has the highest glance
contrast on OLED (volt on black 16.9:1), and Focus/Tour are told apart by color alone. Without Calma's arc.

Conditions applied in the implementation:
1. The rings pulse only when a real haptic cue plays, at that pattern's rhythm, once (about 1–2 s), never in ambient.
2. Each color has one job, ΔE ≥ 30 from every other in both themes: volt = brand/Focus, orchid = Tour and its places,
   sky = arrival, amber = warning, red = a problem.
3. Tutorial copy states the real pattern ("2 long, slow pulses"), checked against `HapticTokens` by `LessonCopyTest`.
4. Map pill moved under the clock; every surface control has a 1.5 dp border ≥ 3:1 against the background.
5. System Roboto / Roboto Condensed only.

## 5. Result

`../before-after/before_after_round-large-dark-en.png` and `…-light-en.png`: the main screens before (left) and
after (right). Every screen is also covered in the screenshot references for small round, large round and square,
English and Spanish, light and dark, and the largest system font.
