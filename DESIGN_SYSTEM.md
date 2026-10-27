# HaptiMap Watch — Design System

The design system lives in its own Gradle module, **`:core:designsystem`**. Any feature module
can depend on it, and it depends only on `:core:model` (for `PlaceCategory`). Every color,
type style, spacing, shape, size and piece of text the user sees is defined here or in
`res/values*`. The haptic language is the one exception: it lives in `:core:model` (see §2). **Nothing else in the app holds raw design values.**

## 1. Folder structure (Atomic Design)

```
core/designsystem/src/main/
├── java/com/xanderjs/haptimap/designsystem/
│   ├── tokens/                     ← raw design decisions (the ONLY place with literal values)
│   │   ├── ColorTokens.kt          BasePalette (internal) + HaptiColors (semantic roles)
│   │   ├── Typography.kt           HaptiTypography: distance, title, body, label, caption
│   │   ├── Spacing.kt              HaptiSpacing: xs 2 · s 4 · m 8 · l 12 · xl 16 dp
│   │   ├── Shape.kt                HaptiShapes: small 8 · medium 16 · large 24 dp · full (pill)
│   │   └── Dimens.kt               HaptiDimens: minTouchTarget 48 dp, icon sizes, borderWidth, clockBandSafeTop 8 dp
│   ├── theme/                      ← turns tokens into a theme (+ AppLanguage.kt: Context.appLanguage())
│   │   ├── ThemeMode.kt            SYSTEM · LIGHT · DARK
│   │   ├── ColorSchemes.kt         LightHaptiColors, DarkHaptiColors, mapping to Wear M3 ColorScheme
│   │   └── HaptiMapTheme.kt        HaptiMapTheme(themeMode) { } + accessor object HaptiMapTheme.*
│   ├── atoms/                      ← smallest UI pieces; only use tokens
│   │   ├── HaptiButton.kt          primary / secondary
│   │   ├── HaptiIcon.kt            Small / Medium / Large from dimens
│   │   ├── HaptiTimeText.kt        curved clock in onBackground; offline label + LocalOffline
│   │   ├── DirectionArrow.kt       LEFT / RIGHT / STRAIGHT, tinted navigationArrow
│   │   ├── DistanceText.kt         formatDistance(): "850 m", "1.2 km" / "1,2 km"; DistanceText (large)
│   │   └── CategoryChip.kt         selectable search category + PlaceCategory.label
│   ├── molecules/                  ← a few atoms working as one unit
│   │   ├── ManeuverIndicator.kt    DirectionArrow + DistanceText, read as one phrase by TalkBack
│   │   └── PlaceRow.kt             a place: name + "category · distance", one touch target
│   ├── organisms/                  ← full sections of a screen
│   │   ├── PlaceResultsList.kt     search results: a PlaceRow per place
│   │   └── StatusMessage.kt        search loading / empty / error + PlaceSearchError.message
│   ├── templates/                  ← screen skeletons, no real data
│   │   └── RoundScreenTemplate.kt  ScreenScaffold + TimeText + safe round padding + scrolling list
│   └── preview/                    ← preview tooling (not part of the hierarchy)
│       ├── HaptiPreviews.kt        multipreview: {small, large round} × {light, dark} × {en, es}
│       └── PreviewSurface.kt       theme + background wrapper for previews
└── res/
    ├── values/strings.xml          English (default)
    ├── values-es/strings.xml       Spanish
    └── drawable/                   ic_turn_left, ic_turn_right, ic_straight
```

Screens built from templates live in the feature modules (`:feature:search`,
`:feature:navigation-ui`, `:feature:onboarding`). The temporary catalog lives in
`app/.../presentation/catalog/DesignSystemCatalog.kt`.

### What belongs at each level

| Level | Contains | May use | Example |
|---|---|---|---|
| tokens | Raw values: hex colors, dp, sp, ms, amplitudes | nothing | `BasePalette.Teal80`, `HaptiSpacing.m` |
| theme | Mapping tokens to light/dark and providing them | tokens | `HaptiMapTheme(ThemeMode.DARK)` |
| atoms | One indivisible UI element | theme | `HaptiButton`, `DistanceText` |
| molecules | A small group of atoms with one job | atoms, theme | `ManeuverIndicator` |
| organisms | A self-contained section of a screen | molecules, atoms, theme | `PlaceResultsList`, `StatusMessage` |
| templates | Screen layout without real data | organisms ↓, theme | `RoundScreenTemplate` |
| screens (features) | Templates filled with real state | everything above | (future) `NavigationScreen` |

**Dependencies only flow downward.** An atom never imports a molecule, and a molecule never
imports an organism. An atom may specialize another atom (`DirectionArrow` is a `HaptiIcon` with
the `navigationArrow` color and a TalkBack description).

## 2. Tokens

### Always go through the theme

```kotlin
Text(
    text = stringResource(R.string.catalog_title),
    style = HaptiMapTheme.typography.title,
    color = HaptiMapTheme.colors.onBackground,
)
Modifier.padding(HaptiMapTheme.spacing.m)
```

### Never use raw values

```kotlin
Color(0xFF00695C)      // ✗ bypasses light/dark and the palette
8.dp / 14.sp           // ✗ an orphan value nobody can update globally
Text("Start route")    // ✗ never translated
```

Why this matters:

- **One change, whole app.** `primary` is defined once per scheme. Changing `BasePalette.Teal80`
  recolors every header, maneuver arrow and primary button. This was checked on the emulator.
- **Light/dark for free.** Components ask for a *role* (`onSurface`), not a color, so the
  theme switch is a single `CompositionLocal` change.
- **Guaranteed contrast.** Every text/background pair was checked against WCAG AA (≥ 4.5:1).
  A raw color skips that check.

### Two layers of color

1. `BasePalette` (internal): the named swatches (`Teal40`, `Amber80`, `Neutral10`...).
   Components **cannot** see it.
2. `HaptiColors` (public): semantic roles. These are the only colors components use.

| Role | Dark (OLED) | Light (outdoors) | Used for |
|---|---|---|---|
| background | `#000000` | `#FFFFFF` | screen background |
| onBackground | `#E8ECEB` | `#0F1414` | main text, clock |
| surface | `#1A1D1D` | `#EEF2F1` | secondary buttons, unselected chips |
| onSurface | `#E8ECEB` | `#0F1414` | text on surface |
| onSurfaceVariant | `#B5BDBB` | `#3D4644` | secondary text, outlines |
| primary | `#4FD8C4` | `#00695C` | primary actions, headers |
| onPrimary | `#00201C` | `#FFFFFF` | text on primary |
| accent / poiHighlight / warning | `#FFC046` | `#8A5300` | selected chip, POIs |
| onAccent | `#2A1800` | `#FFFFFF` | text on accent |
| navigationArrow | `#4FD8C4` | `#00695C` | maneuver arrows |
| offRoute / error | `#FF8A80` | `#B3261E` | off-route state, errors |
| success | `#7DD88A` | `#1E6B2E` | confirmations |

The lowest measured contrast is 5.61:1 (light `accent` on `surface`). Every pair passes AA.

`HaptiMapTheme` also feeds these roles into the Wear Material 3 `ColorScheme`. Stock Wear
components (scaffold, scroll indicator) therefore follow the tokens too.

### No elevation

The design is intentionally flat, following the Wear OS guidelines: hierarchy comes from color
and shape, never from shadows. There is no elevation token; don't add one.

### Haptic language (data only, played in step 09)

The patterns live in **`:core:model`** (`HapticPattern.kt`), next to `HapticEvent`, not in the
design system. They are pure data, and the module that plays them (`:core:haptics`, step 09)
must not depend on Compose or the UI. The catalog reads them from there too:
`HapticTokens.patternFor(HapticEvent.TURN_LEFT)`.

| Event | Pattern | Data |
|---|---|---|
| TURN_LEFT | 2 medium pulses | 120 on · 150 off · 120 on @160 |
| TURN_RIGHT | 3 medium pulses | 3 × 120 ms @160, 150 ms gaps |
| CONTINUE | 1 soft pulse | 120 ms @80 |
| TURN_IMMINENT | progressive pulses | 4 pulses @80→160→255→255, gaps shrinking 150→120→50 |
| ARRIVAL | long pulse + double tap | 500 ms @160, then 2 × 40 ms @255 |
| OFF_ROUTE | long intense pulse | 900 ms @255 |
| POI_SUGGESTION | distinct burst | 4 × 40 ms taps, 50 ms gaps, alternating @80/@255 |

`HapticPattern(timings, amplitudes)` uses the exact format of `VibrationEffect.createWaveform`.
The exhaustive `when` in `patternFor` means a new `HapticEvent` without a pattern does not compile.

## 3. Light and dark mode

```kotlin
HaptiMapTheme(themeMode = ThemeMode.SYSTEM) { … }   // SYSTEM | LIGHT | DARK
```

`SYSTEM` follows the device's `uiMode`. The catalog's theme button cycles through all three modes.
The choice is kept in memory for now; persisting it is a later step.

## 4. Languages

All user-visible text comes from `stringResource()` / `pluralStringResource()`.

| Folder | Language |
|---|---|
| `res/values/` | English (default, used for any unsupported locale) |
| `res/values-es/` | Spanish |

Strings that belong to design-system components (arrow descriptions, distance formats, preview
samples) live in `:core:designsystem`. Screen strings live in their own module. Token names shown
in the catalog are code identifiers and are marked `translatable="false"`.

### Adding a new language (example: Portuguese)

1. Create `res/values-pt/strings.xml` in **every** module that has a `res/values/strings.xml`
   (`app`, `core/designsystem`, …). Translate each `<string>` and `<plurals>` entry, keeping the
   same `name`. Skip entries marked `translatable="false"`.
2. Add `<locale android:name="pt" />` to `app/src/main/res/xml/locales_config.xml`. This makes
   the language appear in the system's per-app language settings (Android 13+).
3. Add `locale = "pt"` variants to `HaptiPreviews` if it should be previewed.
4. Run `./gradlew lintDebug`. The `MissingTranslation` check lists any string you forgot.

**Per-app language picker: Android 13+ only.** `locales_config.xml` drives the system's
per-app language setting, which exists only from Android 13 (API 33). On older watches
the app follows the device language. We don't build an in-app picker for them: it would duplicate
a system feature for a shrinking set of devices.

**Place categories.** `PlaceCategory` values are English code identifiers (`PHARMACY`, `TRANSIT`…).
Users never see them: `PlaceCategory.label` (in `CategoryChip.kt`) maps each one to a
`category_*` string in both languages.

## 5. Adding a new component

1. **Choose the level.** One element → atom. A few atoms with one job → molecule. A screen
   section → organism. A screen skeleton → template.
2. **Create one file** in that folder, named after the component (`SpeedText.kt`).
3. **Only use the theme:** `HaptiMapTheme.colors / typography / spacing / shapes / dimens`.
   If a value you need is missing, **add a token first**. Never write it inline.
4. **Text comes in as a `String`** already resolved by the caller with `stringResource()`, or
   from the module's own `strings.xml` (in both `values/` and `values-es/`).
5. **Accessibility:** interactive elements are at least `dimens.minTouchTarget` (48 dp). Icons have
   a `contentDescription` (or `null` if purely decorative).
6. **Preview** with `@HaptiPreviews` and `PreviewSurface { … }`. That gives all 8 combinations
   (2 screen sizes × light/dark × en/es).
7. **Show it** in `DesignSystemCatalog`.

**`CategoryChip` is a pill button, not a Wear chip/toggle.** It is a `Button` with
`shapes.full`, and its selection is shown by the accent color and announced through
`semantics { selected }`. It has a single label and uses the same touch target as every other
button. A selectable chip component would add a second visual style without adding anything.

```kotlin
@Composable
fun SpeedText(kmh: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.speed_kmh, kmh),
        modifier = modifier,
        style = HaptiMapTheme.typography.body,
        color = HaptiMapTheme.colors.onSurfaceVariant,
    )
}

@HaptiPreviews
@Composable
private fun SpeedTextPreview() = PreviewSurface { SpeedText(kmh = 5) }
```

## 6. Offline indicator

The offline indicator is a state of the **`HaptiTimeText` atom**, not a separate widget. When
offline, a curved label in `warning` comes before the time: **"Offline · 10:56"** /
**"Sin red · 10:56"**.

| Choice | Why |
|---|---|
| In the clock band | The band at the top is where Wear OS puts status on every screen. The label is always visible and never takes space from the content or covers a maneuver. |
| Atom | It is one indivisible piece of the clock, not a group of components. |
| `warning` color | Not a failure (`error`): navigation continues. It passes AA in both themes. |
| Text, no icon | Clearer at a glance than a crossed-out cloud, and TalkBack reads it as is. |
| Spanish "Sin red" | "Sin conexión" doesn't fit next to the time within TimeText's maximum sweep. |

```kotlin
HaptiTimeText()                // reads LocalOffline (default: false)
HaptiTimeText(offline = true)  // explicit, for previews
CompositionLocalProvider(LocalOffline provides isOffline) { … }   // done once in MainActivity
```

`RoundScreenTemplate` reads `LocalOffline` and passes it to its `HaptiTimeText`. Wear draws the
`ScreenScaffold`'s `timeText` inside `AppScaffold`, which is outside any provider set by the
screen, so without this the catalog's "Simulate offline" toggle wouldn't work. Previews:
`HaptiTimeTextOfflinePreview` (8 combinations). In the catalog, "Simulate offline" forces the
state without cutting the network.

**Safe top padding: `dimens.clockBandSafeTop` (8 dp).** The curved label runs down the sides of
the clock band, below where Wear's own top padding ends, so it used to overlap the first item
(step 03 note). `RoundScreenTemplate` adds this token to the top of the `ScreenScaffold`
`contentPadding`, so every screen gets the room. It is always applied, online too, so content
doesn't jump when connectivity changes. Checked on the small round emulator with "Simulate
offline" in English and Spanish: the label's lowest point now sits above the first item.

## 7. Screens and the scaffold

Each screen uses `RoundScreenTemplate { item { … } }`. The app root must be
`AppScaffold(timeText = { HaptiTimeText() })`. After a recomposition (for example a theme
switch), Wear's scaffold can fall back to the app-level time text. If that time text is not
`HaptiTimeText`, the clock reverts to Wear's 80%-luminance gray, which is unreadable in light mode.

## 8. Place search (step 04)

Three pieces, shown in the catalog under the category chips. Each one has `@HaptiPreviews`
(8 combinations).

| Component | Level | What it shows |
|---|---|---|
| `PlaceRow(place, onClick)` | molecule | Name (`label`, up to 2 lines) over "Category · 850 m" (`caption`, `onSurfaceVariant`) in a `surface` button with `shapes.large`. Text-search results with no category show only the distance. The whole row is one touch target, at least `minTouchTarget`. |
| `PlaceResultsList(places, onPlaceClick)` | organism | A `PlaceRow` per place, in the order received, spaced `spacing.s`. It is a `Column`, not a lazy list: it goes as one item inside `RoundScreenTemplate`, which already scrolls, and never holds more than `MAX_RESULTS` (10). |
| `StatusMessage(status)` | organism | `SearchStatus.Loading` (spinner in `primary` + "Searching…") or `SearchStatus.Failed(error)`. A `liveRegion`, so TalkBack announces each change. |

**One mapping for error texts.** `PlaceSearchError.message` (in `StatusMessage.kt`) is the only
place that turns an error into a `@StringRes`. Step 11 (the final search screen) reuses
`StatusMessage` as is. Colors: `NO_RESULTS` is not a failure (`onSurfaceVariant`);
`NO_CONNECTION` and `NO_LOCATION` are conditions the user can wait out (`warning`, like the offline
label); quota, invalid key and other API failures are `error`.

**Distances follow the app language.** `formatDistance(meters)` formats with the locale from
`Context.appLanguage()`: "850 m", "1.2 km" in English, "1,2 km" in Spanish. `DistanceText` and
`PlaceRow` both use it. `appLanguage()` reads the `app_language` string ("en" / "es"), which
Android resolves from the Context's `Configuration`, per-app language included. An unsupported
device language therefore falls back to English for the decimal separator, the same as the texts.
The Places `languageCode` comes from the same function, so place names, addresses and distances
always agree with the rest of the UI.
