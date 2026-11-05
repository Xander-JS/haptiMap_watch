"""Hoja de revisión de diseño: las pantallas clave en las 12 combinaciones, desde las capturas de referencia.

Uso, desde la raíz del repo, tras regrabar capturas: python3 docs/review/make_review_sheet.py
Escribe docs/review/review-sheet.png. Necesita Pillow.
"""
import glob
import os

from PIL import Image, ImageDraw, ImageFont

NAV = "feature/navigation-ui/src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/navigationui"
DEBUG = "app/src/screenshotTestDebug/reference/com/xanderjs/haptimap/presentation/debug"
ONBOARDING = "feature/onboarding/src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/onboarding"
SEARCH = "feature/search/src/screenshotTestDebug/reference/com/xanderjs/haptimap/feature/search/SearchScreenScreenshotsKt"
ROWS = [
    ("Location permission", f"{ONBOARDING}/PermissionScreenshotsKt/PermissionRationale"),
    ("Resume", f"{NAV}/RouteSummaryScreenshotsKt/ResumeRoute"),
    ("Search", f"{SEARCH}/SearchHome"),
    ("Search · navigating + offline", f"{SEARCH}/SearchHomeNavigatingOffline"),
    ("Search · route ready + no voice", f"{SEARCH}/SearchHomeRouteReadyNoVoice"),
    ("Search results", f"{SEARCH}/SearchResultsCategory"),
    ("Results · empty", f"{SEARCH}/SearchResultsEmpty"),
    ("Results · error + retry", f"{SEARCH}/SearchResultsNoLocationRetry"),
    ("Replace route confirmation", f"{SEARCH}/SearchReplaceRoute"),
    ("Route summary", f"{NAV}/RouteSummaryScreenshotsKt/RouteReady"),
    ("Navigation (Eco)", f"{NAV}/NavigationScreenshotsKt/NavigationTurn"),
    ("Navigation · weak GPS + long instruction", f"{NAV}/NavigationScreenshotsKt/NavigationWeakGpsLongInstruction"),
    ("Off route", f"{NAV}/NavigationScreenshotsKt/NavigationOffRoute"),
    ("Off route · recalculating", f"{NAV}/NavigationScreenshotsKt/NavigationOffRouteRecalculating"),
    ("Off route · weak GPS + error", f"{NAV}/NavigationScreenshotsKt/NavigationOffRouteWeakGpsFailed"),
    ("Stop confirmation", f"{NAV}/NavigationScreenshotsKt/NavigationStopConfirmation"),
    ("Arrival", f"{NAV}/NavigationScreenshotsKt/NavigationArrived"),
    ("Ambient · navigating", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientNavigating"),
    ("Ambient · offline", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientNavigatingOffline"),
    ("Ambient · off route", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientOffRoute"),
    ("Ambient · recalculating", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientRecalculating"),
    ("Ambient · weak GPS + error", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientOffRouteWeakGpsFailed"),
    ("Ambient · arrival", f"{NAV}/AmbientNavigationScreenshotsKt/AmbientArrived"),
    ("Haptics bench (debug)", f"{DEBUG}/HapticsDebugScreenshotsKt/HapticsBench"),
    ("Haptics bench · playing", f"{DEBUG}/HapticsDebugScreenshotsKt/HapticsBenchPlaying"),
]
COLUMNS = [(shape, theme, lang) for shape in ("round-small", "round-large", "square") for theme in ("light", "dark") for lang in ("en", "es")]
CELL, PAD, LABEL_W, HEAD_H = 200, 8, 230, 56
OUT = "docs/review/review-sheet.png"


def font(size):
    for path in ("/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf", "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf"):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def main():
    width = LABEL_W + len(COLUMNS) * (CELL + PAD) + PAD
    height = HEAD_H + len(ROWS) * (CELL + PAD) + PAD
    sheet = Image.new("RGB", (width, height), "#8a8a8a")
    draw = ImageDraw.Draw(sheet)
    head, label = font(16), font(18)
    for c, (shape, theme, lang) in enumerate(COLUMNS):
        x = LABEL_W + c * (CELL + PAD) + PAD
        draw.text((x + 4, 8), shape, fill="white", font=head)
        draw.text((x + 4, 30), f"{theme} · {lang}", fill="white", font=head)
    for r, (title, prefix) in enumerate(ROWS):
        y = HEAD_H + r * (CELL + PAD) + PAD
        words, line = title.split(" "), ""
        lines = []
        for w in words:
            if len(line + w) > 20:
                lines.append(line.strip())
                line = ""
            line += w + " "
        lines.append(line.strip())
        for i, text in enumerate(lines):
            draw.text((PAD, y + CELL // 2 - 12 * len(lines) + i * 24), text, fill="white", font=label)
        for c, (shape, theme, lang) in enumerate(COLUMNS):
            match = glob.glob(f"{prefix}_{shape}-{theme}-{lang}_*.png")
            if len(match) != 1:
                raise SystemExit(f"missing reference: {prefix} {shape}-{theme}-{lang}")
            tile = Image.open(match[0]).convert("RGB").resize((CELL, CELL), Image.LANCZOS)
            sheet.paste(tile, (LABEL_W + c * (CELL + PAD) + PAD, y))
    sheet.save(OUT, optimize=True)
    print(f"{OUT}: {width}x{height}")


if __name__ == "__main__":
    main()
