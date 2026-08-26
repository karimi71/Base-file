#!/usr/bin/env python3
"""Convert Gradle's exact files-2.1 set into a conventional local Maven repo."""
from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    if len(sys.argv) != 3:
        print(f"usage: {sys.argv[0]} GRADLE_FILES_2_1 MAVEN_DEST", file=sys.stderr)
        return 2
    source = Path(sys.argv[1]).resolve()
    destination = Path(sys.argv[2]).resolve()
    if not source.is_dir():
        raise SystemExit(f"Gradle module cache not found: {source}")
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True)

    coordinates: dict[tuple[str, str, str], set[str]] = {}
    copied = 0
    copied_bytes = 0
    # Layout: group.id/artifact/version/content-hash/file
    for group_dir in sorted(p for p in source.iterdir() if p.is_dir()):
        for artifact_dir in sorted(p for p in group_dir.iterdir() if p.is_dir()):
            for version_dir in sorted(p for p in artifact_dir.iterdir() if p.is_dir()):
                coordinate = (group_dir.name, artifact_dir.name, version_dir.name)
                target_dir = destination.joinpath(
                    *group_dir.name.split("."), artifact_dir.name, version_dir.name
                )
                for cache_hash_dir in sorted(p for p in version_dir.iterdir() if p.is_dir()):
                    for item in sorted(p for p in cache_hash_dir.iterdir() if p.is_file()):
                        target = target_dir / item.name
                        if target.exists():
                            if digest(target) != digest(item):
                                raise RuntimeError(
                                    f"conflicting files for {coordinate}: {item.name}"
                                )
                            continue
                        target_dir.mkdir(parents=True, exist_ok=True)
                        shutil.copyfile(item, target)
                        coordinates.setdefault(coordinate, set()).add(item.name)
                        copied += 1
                        copied_bytes += item.stat().st_size

    manifest = destination / "BASE_FILE_COORDINATES.tsv"
    with manifest.open("w", encoding="utf-8", newline="\n") as out:
        out.write("group\tartifact\tversion\tfiles\n")
        for coordinate, files in sorted(coordinates.items()):
            out.write("\t".join((*coordinate, ",".join(sorted(files)))) + "\n")

    print(
        f"Local Maven repository: {len(coordinates)} coordinates, "
        f"{copied} files, {copied_bytes / (1024 * 1024):.1f} MiB"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
