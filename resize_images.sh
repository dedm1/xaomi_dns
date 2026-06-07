#!/bin/bash
set -euo pipefail

SOURCE="image.png"

sips -Z 48 "$SOURCE" --out app/src/main/res/mipmap-mdpi/ic_launcher.png
sips -Z 48 "$SOURCE" --out app/src/main/res/mipmap-mdpi/ic_launcher_round.png
sips -Z 72 "$SOURCE" --out app/src/main/res/mipmap-hdpi/ic_launcher.png
sips -Z 72 "$SOURCE" --out app/src/main/res/mipmap-hdpi/ic_launcher_round.png
sips -Z 96 "$SOURCE" --out app/src/main/res/mipmap-xhdpi/ic_launcher.png
sips -Z 96 "$SOURCE" --out app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
sips -Z 144 "$SOURCE" --out app/src/main/res/mipmap-xxhdpi/ic_launcher.png
sips -Z 144 "$SOURCE" --out app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
sips -Z 192 "$SOURCE" --out app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
sips -Z 192 "$SOURCE" --out app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png

python3 - <<'PY'
from PIL import Image

SOURCE = "image.png"
THRESHOLD = 240


def remove_near_white_background(image: Image.Image, threshold: int) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if r >= threshold and g >= threshold and b >= threshold:
                pixels[x, y] = (r, g, b, 0)
    return rgba


source = Image.open(SOURCE)

foreground = remove_near_white_background(source, THRESHOLD)
foreground.resize((432, 432), Image.Resampling.LANCZOS).save(
    "app/src/main/res/drawable/ic_launcher_foreground.png"
)

widget = remove_near_white_background(source, THRESHOLD)
widget.resize((256, 256), Image.Resampling.LANCZOS).save(
    "app/src/main/res/drawable/widget_icon.png"
)
PY
