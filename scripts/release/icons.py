#!/usr/bin/env python3
"""Render Pitch Perfect's Play Store icon from the launcher icon it ships with.

The launcher icon is adaptive: a plate colour behind a foreground PNG on a
108dp canvas, of which launchers reveal the central 72dp. The production
build's foreground is the white notation; the private build's foreground
(``src/private``) adds the blue beta band and β. The watch reuses the phone's
foregrounds, so there is exactly one drawing per build type.

The store icon is that same composition, cropped to the central window and
scaled to Play's 512px square, so the listing and the launcher always match.
"""
import argparse
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PHONE = ROOT / 'Android/PitchPerfect/src'
FOREGROUNDS = {
    'production': PHONE / 'main/res/mipmap-xxxhdpi/ic_launcher_foreground.png',
    'private': PHONE / 'private/res/mipmap-xxxhdpi/ic_launcher_foreground.png',
}
PLATE = (0x47, 0x47, 0x47)  # ic_launcher_background in both modules
CANVAS_DP = 108
# Play shows the icon nearly edge to edge inside rounded corners, launchers
# show 72dp of the 108dp canvas; 80dp keeps the notation the same size as on
# a home screen with a little more plate around it.
WINDOW_DP = 80


def store(variant, size=512):
    """The opaque store icon for a build variant ('production' or 'private')."""
    foreground = Image.open(FOREGROUNDS[variant]).convert('RGBA')
    if foreground.width != foreground.height:
        raise ValueError(f'Adaptive foreground must be square: {foreground.size}')
    canvas = Image.new('RGBA', foreground.size, PLATE + (255,))
    canvas.alpha_composite(foreground)
    px_per_dp = foreground.width / CANVAS_DP
    window = round(WINDOW_DP * px_per_dp)
    offset = (foreground.width - window) // 2
    cropped = canvas.crop((offset, offset, offset + window, offset + window))
    return cropped.resize((size, size), Image.LANCZOS).convert('RGB')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--variant', choices=sorted(FOREGROUNDS), default='production')
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    store(args.variant).save(args.output, optimize=True)
    print(args.output)


if __name__ == '__main__':
    main()
