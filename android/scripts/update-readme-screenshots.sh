#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if command -v magick >/dev/null 2>&1; then
  IMG="magick"
elif command -v convert >/dev/null 2>&1 && command -v identify >/dev/null 2>&1; then
  IMG="convert"
else
  echo "error: ImageMagick not found." >&2
  exit 1
fi

./gradlew :app:recordPaparazziDebug
mkdir -p screenshots

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

$IMG scripts/device-frame/pixel-9-back.webp -alpha extract -negate \
  \( +clone -fill black -colorize 100 -fill white -draw "rectangle 55,58 1134,2481" \) \
  -compose Multiply -composite -morphology Erode "Diamond:2" "$WORK/hole-clip.png"
$IMG scripts/device-frame/pixel-9-mask.webp -crop "82x82+499+44" +repage "$WORK/dot.png"

frame_pixel9() {
  local tmp
  tmp="$(mktemp -d)"
  $IMG "$1" -resize "1096x2440^" -gravity center -extent "1096x2440" "$tmp/screen.png"
  $IMG -size "1198x2531" xc:none "$tmp/screen.png" -geometry "+47+50" -composite "$tmp/canvas.png"
  $IMG "$tmp/canvas.png" -alpha off "$WORK/hole-clip.png" -compose CopyOpacity -composite "$tmp/clipped.png"
  $IMG "$tmp/clipped.png" scripts/device-frame/pixel-9-back.webp -geometry +0+0 -composite "$tmp/framed.png"
  $IMG "$tmp/framed.png" "$WORK/dot.png" -geometry "+554+102" -composite "$tmp/device.png"
  $IMG "$tmp/device.png" \( +clone -background black -shadow 80x24+0+18 \) +swap -background none -layers merge +repage "$2"
  rm -rf "$tmp"
}

PREFIX="org.appdevncsu.foodfinder_ReadmeScreenshotsTest"
SRC="app/src/test/snapshots/images"
frame_pixel9 "$SRC/${PREFIX}_homeLight.png" screenshots/home-light.png
frame_pixel9 "$SRC/${PREFIX}_homeDark.png" screenshots/home-dark.png
frame_pixel9 "$SRC/${PREFIX}_menuListLight.png" screenshots/menu-list-light.png
frame_pixel9 "$SRC/${PREFIX}_menuListDark.png" screenshots/menu-list-dark.png
frame_pixel9 "$SRC/${PREFIX}_menuLight.png" screenshots/menu-light.png
frame_pixel9 "$SRC/${PREFIX}_menuDark.png" screenshots/menu-dark.png
