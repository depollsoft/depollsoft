#!/usr/bin/env python3
"""Register (or remove) source files in tagmaster.xcodeproj.

Usage: add_source.py <File.swift|.m|.h> [--target app|tests]
       add_source.py --shared <File.swift> --target app|tests   (a file in iOS/shared)
       add_source.py --remove <File>...

New files are placed beside TMTagLists.swift (app) or TMBehaviorTestSupport.swift (tests).
See ../../tools/xcodeproj_files.py; ids are hash-derived so parallel branches merge.
"""
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent / "tagmaster.xcodeproj" / "project.pbxproj"
TOOL = HERE.parents[1] / "tools" / "xcodeproj_files.py"
ANCHORS = {"app": "TMTagLists.swift", "tests": "TMBehaviorTestSupport.swift"}
SHARED_ANCHOR = "TelemetryConsent.swift"


def run(*args):
    subprocess.run([sys.executable, str(TOOL), str(PROJECT), *args], check=True)


def main():
    args = sys.argv[1:]
    if args[:1] == ["--remove"]:
        run("remove", *args[1:])
        return
    target = "app"
    if "--target" in args:
        i = args.index("--target")
        target = args[i + 1]
        del args[i:i + 2]
    shared = "--shared" in args
    if shared:
        args.remove("--shared")
    if len(args) != 1:
        sys.exit(__doc__)
    name = Path(args[0]).name
    if shared:
        run("add", name, "--anchor", SHARED_ANCHOR, "--anchor-build", ANCHORS[target],
            "--path", f"../shared/{name}")
    else:
        run("add", name, "--anchor", ANCHORS[target])


if __name__ == "__main__":
    main()
