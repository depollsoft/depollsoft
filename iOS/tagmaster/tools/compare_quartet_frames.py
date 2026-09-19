#!/usr/bin/env python3
"""Compare exported native C7 frames. Requires Pillow; never regenerates UI.

Run the two native frame producers first. The Android one is opt-in, so it is
skipped unless its instrumentation argument is passed:

    ./gradlew :TagMaster:connectedDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.quartetFrames=true \
        -Pandroid.testInstrumentationRunnerArguments.class=\
depollsoft.tagmaster.TagLoadingRegressionTest#controlled_pending_to_loaded_capture

and then TMLoadingRegressionTests.testMatchedQuartetNativeFramesAndGlassPendingJourney
on iOS. Interiors must match exactly. Raster coverage may differ only at a
one-pixel boundary, including the 127/128 half-covered one-pixel ledger.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops as C, ImageFilter as F


def binary(image):
    return image.point(lambda value: 255 if value else 0)


def any_difference(image):
    result = Image.new('L', image.size, 0)
    for channel in image.split():
        result = C.lighter(result, channel)
    return binary(result)


def stable_interior(image):
    result = Image.new('L', image.size, 255)
    for channel in image.split():
        result = C.darker(result, C.invert(binary(C.difference(
            channel.filter(F.MaxFilter(3)), channel.filter(F.MinFilter(3))))))
    return C.darker(result, binary(image.getchannel('A')))


def count(mask):
    return mask.histogram()[255]


def compare(a, b):
    assert a.size == b.size
    interior = C.darker(stable_interior(a), stable_interior(b))
    mismatch = count(C.darker(interior, any_difference(C.difference(a, b))))
    outside = []
    half_coverage = 0
    for threshold in [10, 127]:
        ma = a.getchannel('A').point(lambda value: 255 if value > threshold else 0)
        mb = b.getchannel('A').point(lambda value: 255 if value > threshold else 0)
        unmatched = C.lighter(C.subtract(ma, mb.filter(F.MaxFilter(3))),
                              C.subtract(mb, ma.filter(F.MaxFilter(3))))
        failures = count(unmatched)
        if threshold == 127 and failures:
            for y in range(a.height):
                for x in range(a.width):
                    if not unmatched.getpixel((x, y)):
                        continue
                    pa, pb = a.getpixel((x, y)), b.getpixel((x, y))
                    # This precise AA cutoff is not missing geometry. Both
                    # renderers cover the same boundary pixel at half opacity.
                    if {pa[3], pb[3]} == {127, 128} and all(abs(ca - cb) <= 2 for ca, cb in zip(pa, pb)) and not interior.getpixel((x, y)):
                        failures -= 1
                        half_coverage += 1
        outside.append(failures)
    return dict(interior=count(interior), interiorMismatches=mismatch,
                outsideOnePixel=outside, halfCoverageBoundaryPixels=half_coverage)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--android', type=Path, required=True)
    parser.add_argument('--ios', type=Path, required=True)
    parser.add_argument('--device', default='phone', choices=['phone', 'ipad'])
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    results = []
    for theme in ['light', 'dark']:
        for scale in [1, 10]:
            frames = {}
            for phase in ['0', '0.125', '0.25', '0.5', '0.75', '1', 'still']:
                a = Image.open(args.android / f'android-{theme}-{scale}-{phase}.png').convert('RGBA')
                b = Image.open(args.ios / f'ios-{args.device}-{theme}-{scale}-{phase}.png').convert('RGBA')
                frames[phase] = (a, b)
                results.append(dict(theme=theme, scale=scale, phase=phase, **compare(a, b)))
            for platform in [0, 1]:
                for phase in ['1', 'still']:
                    assert not any_difference(C.difference(frames['0'][platform], frames[phase][platform])).getbbox(), 'Loop/still drift'
    args.report.write_text(json.dumps(results, indent=2) + '\n')
    failures = [result for result in results if result['interiorMismatches'] or any(result['outsideOnePixel'])]
    print(f'{len(results)} comparisons; {len(failures)} failures; exact loop/still endpoints')
    assert not failures, failures


if __name__ == '__main__':
    main()
