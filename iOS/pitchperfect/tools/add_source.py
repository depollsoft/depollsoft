#!/usr/bin/env python3
"""Register a source file with the pitchperfect.xcodeproj (legacy explicit-reference project).

Usage: add_source.py <FileName.swift|.m> [--target app|tests]

The file is placed next to an existing sibling in the same group (SongEditorView.swift for the
app target, PitchPerfectSongManagementTests.swift for the test target) and added to that target's Sources
build phase. Header files (.h) get a file reference only. Safe to re-run: an already registered
file is skipped.
"""
import re
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1] / "pitchperfect.xcodeproj" / "project.pbxproj"
ANCHORS = {"app": "SongEditorView.swift", "tests": "PitchPerfectSongManagementTests.swift"}


def next_ids(text, count):
    used = set(re.findall(r"ABBB0000000000000000B([0-9A-F]{3})", text))
    ids, n = [], 0
    while len(ids) < count:
        candidate = "%03X" % n
        n += 1
        if candidate not in used:
            ids.append("ABBB0000000000000000B" + candidate)
    return ids


def main():
    args = sys.argv[1:]
    target = "app"
    if "--target" in args:
        i = args.index("--target")
        target = args[i + 1]
        del args[i:i + 2]
    if len(args) != 1:
        sys.exit(__doc__)
    name = Path(args[0]).name
    text = PROJECT.read_text()
    if f"/* {name} */" in text:
        print(f"{name}: already in project")
        return
    anchor = ANCHORS[target]
    file_type = {".swift": "sourcecode.swift", ".m": "sourcecode.c.objc", ".h": "sourcecode.c.h"}[Path(name).suffix]
    ref_id, build_id = next_ids(text, 2)
    anchor_ref = re.search(r"(\w+) /\* %s \*/ = \{isa = PBXFileReference" % re.escape(anchor), text).group(1)
    anchor_build = re.search(r"(\w+) /\* %s in Sources \*/ = \{isa = PBXBuildFile" % re.escape(anchor), text).group(1)

    ref_line = f"\t\t{ref_id} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = {file_type}; path = {name}; sourceTree = \"<group>\"; }};\n"
    text = text.replace(f"\t\t{anchor_ref} /* {anchor} */ = {{isa = PBXFileReference", ref_line + f"\t\t{anchor_ref} /* {anchor} */ = {{isa = PBXFileReference", 1)
    text = text.replace(f"\t\t\t\t{anchor_ref} /* {anchor} */,\n", f"\t\t\t\t{anchor_ref} /* {anchor} */,\n\t\t\t\t{ref_id} /* {name} */,\n", 1)
    if file_type != "sourcecode.c.h":
        build_line = f"\t\t{build_id} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {ref_id} /* {name} */; }};\n"
        text = text.replace(f"\t\t{anchor_build} /* {anchor} in Sources */ = {{isa = PBXBuildFile", build_line + f"\t\t{anchor_build} /* {anchor} in Sources */ = {{isa = PBXBuildFile", 1)
        text = text.replace(f"\t\t\t\t{anchor_build} /* {anchor} in Sources */,\n", f"\t\t\t\t{anchor_build} /* {anchor} in Sources */,\n\t\t\t\t{build_id} /* {name} in Sources */,\n", 1)
    PROJECT.write_text(text)
    print(f"{name}: added to {target} target")


if __name__ == "__main__":
    main()
