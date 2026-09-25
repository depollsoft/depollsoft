#!/usr/bin/env python3
"""Add or remove source files in the explicit-reference Xcode projects.

Usage:
  xcodeproj_files.py <project.pbxproj> add <File.swift|.m|.h> --anchor <Sibling> [--anchor-build <Sibling>] [--path <path>]
  xcodeproj_files.py <project.pbxproj> remove <File>...

`add` places the file reference next to the anchor's reference, in the same
group, and (for sources) its build file next to the anchor's build file, i.e.
in the same target's Sources phase. `--anchor-build` picks a different sibling
for the build phase (a shared file compiled into a test target). `--path`
overrides the reference's path (e.g. ../shared/Name.swift); the display name
stays the file name.

Object ids are derived from a hash of the project, file name and target
anchor, so two branches that each register different files never pick the
same id and merge cleanly. `remove` deletes every reference, build file and
list entry for the named files. Both are idempotent.
"""
import hashlib
import re
import sys
from pathlib import Path

FILE_TYPES = {
    ".swift": "sourcecode.swift",
    ".m": "sourcecode.c.objc",
    ".h": "sourcecode.c.h",
    ".png": "image.png",
    ".json": "text.json",
}


def object_id(*parts):
    return hashlib.sha1("\0".join(parts).encode()).hexdigest()[:24].upper()


def find_ref(text, name):
    match = re.search(r"\t\t(\w{24}) /\* %s \*/ = \{isa = PBXFileReference" % re.escape(name), text)
    return match.group(1) if match else None


def find_build(text, name):
    match = re.search(r"\t\t(\w{24}) /\* %s in (?:Sources|Resources) \*/ = \{isa = PBXBuildFile" % re.escape(name), text)
    return match.group(1) if match else None


def add(project: Path, name: str, anchor: str, anchor_build: str | None, path: str | None):
    text = project.read_text()
    anchor_build = anchor_build or anchor
    file_type = FILE_TYPES[Path(name).suffix]
    ref_id = find_ref(text, name)
    if ref_id is None:
        ref_id = object_id(project.parent.parent.name, name, "ref")
        anchor_ref = find_ref(text, anchor)
        if anchor_ref is None:
            sys.exit(f"anchor {anchor} has no file reference")
        path_part = f"name = {name}; path = {path};" if path else f"path = {name};"
        ref_line = f"\t\t{ref_id} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = {file_type}; {path_part} sourceTree = \"<group>\"; }};\n"
        marker = f"\t\t{anchor_ref} /* {anchor} */ = {{isa = PBXFileReference"
        text = text.replace(marker, ref_line + marker, 1)
        entry = f"\t\t\t\t{anchor_ref} /* {anchor} */,\n"
        if entry not in text:
            sys.exit(f"anchor {anchor} is in no group")
        text = text.replace(entry, entry + f"\t\t\t\t{ref_id} /* {name} */,\n", 1)
        print(f"{name}: reference added beside {anchor}")
    if file_type == "sourcecode.c.h":
        project.write_text(text)
        return
    anchor_build_id = find_build(text, anchor_build)
    if anchor_build_id is None:
        sys.exit(f"anchor {anchor_build} is in no build phase")
    phase = "Resources" if file_type in ("image.png", "text.json") else "Sources"
    build_id = object_id(project.parent.parent.name, name, anchor_build, "build")
    if build_id in text:
        print(f"{name}: already built beside {anchor_build}")
        project.write_text(text)
        return
    build_line = f"\t\t{build_id} /* {name} in {phase} */ = {{isa = PBXBuildFile; fileRef = {ref_id} /* {name} */; }};\n"
    marker = f"\t\t{anchor_build_id} /* {anchor_build} in "
    text = text.replace(marker, build_line + marker, 1)
    entry = re.search(r"\t\t\t\t%s /\* %s in \w+ \*/,\n" % (anchor_build_id, re.escape(anchor_build)), text).group(0)
    text = text.replace(entry, entry + f"\t\t\t\t{build_id} /* {name} in {phase} */,\n", 1)
    project.write_text(text)
    print(f"{name}: built beside {anchor_build}")


def remove(project: Path, names):
    text = project.read_text()
    for name in names:
        ids = set(re.findall(r"\t\t(\w{24}) /\* %s(?: in \w+)? \*/ = \{isa = PBX(?:File|Build)" % re.escape(name), text))
        if not ids:
            print(f"{name}: not in project")
            continue
        kept = [line for line in text.splitlines(keepends=True)
                if not any(re.match(r"\s*%s /\*" % i, line) for i in ids)]
        text = "".join(kept)
        print(f"{name}: removed ({len(ids)} objects)")
    project.write_text(text)


def main():
    args = sys.argv[1:]
    if len(args) < 3:
        sys.exit(__doc__)
    project, command, rest = Path(args[0]), args[1], args[2:]

    def option(flag):
        if flag in rest:
            i = rest.index(flag)
            value = rest[i + 1]
            del rest[i:i + 2]
            return value
        return None

    if command == "add":
        anchor, anchor_build, path = option("--anchor"), option("--anchor-build"), option("--path")
        if anchor is None or len(rest) != 1:
            sys.exit(__doc__)
        add(project, Path(rest[0]).name, anchor, anchor_build, path)
    elif command == "remove":
        remove(project, [Path(n).name for n in rest])
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
