#!/usr/bin/env python3
"""Fail unless every pinned Tikaro direct coordinate exists in the vendored graph."""
from __future__ import annotations

import csv
import sys
from collections import defaultdict
from pathlib import Path


def read_coordinates(path: Path) -> list[tuple[str, str, str]]:
    with path.open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        required = {"group", "artifact", "version"}
        if not reader.fieldnames or not required.issubset(reader.fieldnames):
            raise SystemExit(f"{path}: expected TSV columns {sorted(required)}")
        return [(row["group"], row["artifact"], row["version"]) for row in reader]


def main() -> int:
    if len(sys.argv) != 3:
        print(f"usage: {sys.argv[0]} REQUESTED.tsv BASE_FILE_COORDINATES.tsv", file=sys.stderr)
        return 2

    requested_path = Path(sys.argv[1])
    manifest_path = Path(sys.argv[2])
    requested = read_coordinates(requested_path)
    available = set(read_coordinates(manifest_path))
    available_versions: dict[tuple[str, str], list[str]] = defaultdict(list)
    for group, artifact, version in sorted(available):
        available_versions[(group, artifact)].append(version)

    missing = [coordinate for coordinate in requested if coordinate not in available]
    if missing:
        print("Pinned Tikaro coordinates missing from offline Maven repository:", file=sys.stderr)
        for group, artifact, version in missing:
            alternatives = ", ".join(available_versions[(group, artifact)]) or "none"
            print(
                f"  {group}:{artifact}:{version} (available versions: {alternatives})",
                file=sys.stderr,
            )
        return 1

    required_transitives = {
        ("androidx.sqlite", "sqlite"),
        ("androidx.sqlite", "sqlite-framework"),
    }
    available_gas = {(group, artifact) for group, artifact, _ in available}
    missing_transitives = sorted(required_transitives - available_gas)
    if missing_transitives:
        for group, artifact in missing_transitives:
            print(f"Required selected transitive is missing: {group}:{artifact}", file=sys.stderr)
        return 1

    duplicates = len(requested) - len(set(requested))
    if duplicates:
        print(f"Requested-coordinate list contains {duplicates} duplicate rows", file=sys.stderr)
        return 1

    print(
        f"Tikaro coordinate audit: all {len(requested)} pinned direct coordinates are present "
        f"in a {len(available)}-coordinate offline graph"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
