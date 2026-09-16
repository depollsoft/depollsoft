#!/usr/bin/env python3
"""Capture Pitch Perfect's actual Wear OS app on a disposable watch emulator."""
import argparse
import hashlib
import math
import os
import subprocess
from pathlib import Path
import time
from PIL import Image
from capture import run, output
from release import ROOT, fingerprint, git, write_json


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--shape', required=True, choices=['round', 'square'])
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    if not args.serial.startswith('emulator-'):
        raise ValueError('Use a disposable Wear OS emulator')
    source_sha, source_fingerprint = git('rev-parse', 'HEAD'), fingerprint()
    adb = ['adb', '-s', args.serial]
    run(*adb, 'wait-for-device')
    deadline = time.monotonic() + 180
    while output(*adb, 'shell', 'getprop', 'sys.boot_completed') != '1':
        if time.monotonic() > deadline:
            raise ValueError('Wear OS did not finish booting')
        time.sleep(2)
    if 'android.hardware.type.watch' not in output(*adb, 'shell', 'pm', 'list', 'features'):
        raise ValueError('Capture requires an actual Wear OS emulator')
    circular = 'FLAG_ROUND' in output(*adb, 'shell', 'dumpsys', 'display')
    if circular != (args.shape == 'round'):
        raise ValueError('Emulator display shape does not match requested watch screenshot')
    run('./gradlew', ':PitchPerfectWear:assembleDebug', cwd=ROOT / 'Android', env=os.environ)
    run(*adb, 'install', '-r', ROOT / 'Android/PitchPerfectWear/build/outputs/apk/debug/PitchPerfectWear-debug.apk')
    run(*adb, 'shell', 'wm', 'size', '384x384')
    run(*adb, 'shell', 'wm', 'density', '192')
    run(*adb, 'shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    run(*adb, 'shell', 'wm', 'dismiss-keyguard')
    run(*adb, 'shell', 'pm', 'clear', 'depollsoft.pitchperfect')
    deadline = time.monotonic() + 120
    while True:
        run(*adb, 'shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
        run(*adb, 'shell', 'wm', 'dismiss-keyguard')
        run(*adb, 'shell', 'am', 'start', '-W', '-n', 'depollsoft.pitchperfect/.PitchPipeActivity')
        time.sleep(3)
        run(*adb, 'shell', 'uiautomator', 'dump', '/sdcard/store-window.xml')
        hierarchy = output(*adb, 'shell', 'cat', '/sdcard/store-window.xml')
        # The instrument is one custom view; its cells are virtual accessibility
        # nodes named after their notes.
        if 'id/pitchInstrument' in hierarchy and 'octave 4' in hierarchy:
            break
        if time.monotonic() > deadline:
            raise ValueError('The Wear pitch pipe is not visible')
    args.output.mkdir(parents=True, exist_ok=True)
    path = args.output / f'{args.shape}.png'
    # Hold the C4 cell (first on the ring, just right of twelve o'clock) so the
    # store shows the instrument sounding: lit cell, name and frequency.
    centre, ring = 192, 384 * 0.395
    angle = math.radians(-90 + 360 / 26)
    x, y = round(centre + ring * math.cos(angle)), round(centre + ring * math.sin(angle))
    hold = subprocess.Popen([*map(str, adb), 'shell', 'input', 'swipe', str(x), str(y), str(x), str(y), '4000'])
    try:
        time.sleep(1.5)
        with path.open('wb') as handle:
            run(*adb, 'exec-out', 'screencap', '-p', stdout=handle)
    finally:
        hold.wait()
    with Image.open(path) as image:
        if image.size != (384, 384):
            raise ValueError(f'Unexpected watch size: {image.size}')
        image.convert('RGB').save(path)
    if (git('rev-parse', 'HEAD'), fingerprint()) != (source_sha, source_fingerprint):
        raise ValueError('Source changed during watch capture; rerun from a stable checkout')
    write_json(args.output / f'{args.shape}.json', {'source_sha': source_sha,
        'source_fingerprint': source_fingerprint, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})


if __name__ == '__main__':
    main()
