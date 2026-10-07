#!/usr/bin/env python3

import os
import subprocess
import sys
import unicodedata
from pathlib import Path

SOURCE_SUFFIXES = {
    ".bash",
    ".cjs",
    ".css",
    ".groovy",
    ".html",
    ".java",
    ".js",
    ".jsx",
    ".kt",
    ".kts",
    ".less",
    ".mjs",
    ".properties",
    ".py",
    ".sass",
    ".scala",
    ".scss",
    ".sh",
    ".sql",
    ".ts",
    ".tsx",
    ".xml",
    ".yaml",
    ".yml",
}


def is_source_file(path: Path) -> bool:
    if path.parts[0] == "scripts":
        return path.suffix in SOURCE_SUFFIXES
    return (
        len(path.parts) >= 3
        and path.parts[0] in {"backend", "frontend"}
        and path.parts[1] == "src"
        and path.suffix in SOURCE_SUFFIXES
    )


def is_homoglyph(character: str) -> bool:
    name = unicodedata.name(character, "")
    return "GREEK" in name or "CYRILLIC" in name


def main() -> int:
    root = Path.cwd()
    tracked_and_untracked = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
        cwd=root,
        check=True,
        stdout=subprocess.PIPE,
    ).stdout
    paths = [Path(os.fsdecode(path)) for path in tracked_and_untracked.split(b"\0") if path]
    violations = 0

    for path in paths:
        display_path = ascii(path.as_posix())
        for character in path.as_posix():
            if is_homoglyph(character):
                name = unicodedata.name(character)
                print(f"{display_path}: path contains U+{ord(character):04X} {name}", file=sys.stderr)
                violations += 1

        if not is_source_file(path):
            continue

        try:
            contents = (root / path).read_text(encoding="utf-8")
        except UnicodeDecodeError as error:
            print(
                f"{display_path}: source is not valid UTF-8 at byte {error.start}",
                file=sys.stderr,
            )
            violations += 1
            continue

        for line_number, line in enumerate(contents.splitlines(), start=1):
            for character in line:
                if is_homoglyph(character):
                    name = unicodedata.name(character)
                    print(
                        f"{display_path}:{line_number}: U+{ord(character):04X} {name}",
                        file=sys.stderr,
                    )
                    violations += 1

    if violations:
        print(f"Homoglyph check failed: {violations} violation(s).", file=sys.stderr)
        return 1

    print("Homoglyph check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
