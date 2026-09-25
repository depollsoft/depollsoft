#!/usr/bin/env python3
"""Compare two directories of screen captures pixel by pixel.

Usage: snapshot_diff.py <reference-dir> <candidate-dir> [--out <diff-dir>] [--only <substr>]

For every PNG present in both directories prints the number and share of
pixels that differ at all, the largest per-channel difference, and the
bounding box of the changes. With --out, writes <name>.diff.png: the
reference dimmed to 25% with every changed pixel painted red, so drift is
easy to locate. Exits 1 when any shared capture differs or is missing.

The SwiftUI port (docs/ios-swiftui.md) used this against captures of the
UIKit screens it replaced. There is deliberately no tolerance: a one-level
difference is still reported, and judged by eye from the diff image.
"""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops


def compare(reference: Path, candidate: Path, out: Path | None):
    a = Image.open(reference).convert("RGB")
    b = Image.open(candidate).convert("RGB")
    if a.size != b.size:
        return {"size": (a.size, b.size)}
    diff = ImageChops.difference(a, b)
    bbox = diff.getbbox()
    if bbox is None:
        return {"changed": 0, "total": a.size[0] * a.size[1]}
    # Per-pixel max channel difference.
    r, g, bl = diff.split()
    peak = ImageChops.lighter(ImageChops.lighter(r, g), bl)
    histogram = peak.histogram()
    changed = sum(histogram[1:])
    largest = max(i for i, count in enumerate(histogram) if count)
    if out is not None:
        mask = peak.point(lambda v: 255 if v else 0)
        base = Image.blend(Image.new("RGB", a.size, "white"), a, 0.25)
        base.paste(Image.new("RGB", a.size, (255, 0, 0)), mask=mask)
        base.save(out / f"{reference.stem}.diff.png")
    return {"changed": changed, "total": a.size[0] * a.size[1], "largest": largest, "bbox": bbox}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("reference", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--only")
    args = parser.parse_args()
    if args.out:
        args.out.mkdir(parents=True, exist_ok=True)

    names = sorted({p.name for p in args.reference.glob("*.png")} | {p.name for p in args.candidate.glob("*.png")})
    if args.only:
        names = [n for n in names if args.only in n]
    failed = False
    for name in names:
        ref, cand = args.reference / name, args.candidate / name
        if not ref.exists() or not cand.exists():
            print(f"MISSING  {name} ({'reference' if not ref.exists() else 'candidate'})")
            failed = True
            continue
        result = compare(ref, cand, args.out)
        if "size" in result:
            print(f"SIZE     {name} {result['size'][0]} vs {result['size'][1]}")
            failed = True
        elif result["changed"] == 0:
            print(f"SAME     {name}")
        else:
            failed = True
            share = 100.0 * result["changed"] / result["total"]
            print(f"DIFF     {name} {result['changed']} px ({share:.3f}%), max {result['largest']}, bbox {result['bbox']}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
