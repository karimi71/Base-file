#!/usr/bin/env python3
"""Split GitHub-oversized Maven files and restore them losslessly for offline use."""
from __future__ import annotations

import argparse
import csv
import hashlib
import os
from pathlib import Path

LIMIT = 90 * 1024 * 1024
PART_SIZE = 64 * 1024 * 1024
MANIFEST_NAME = "BASE_FILE_SPLIT_ARTIFACTS.tsv"
PART_MARKER = ".basefile-part-"


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def read_manifest(repository: Path) -> list[dict[str, str]]:
    manifest = repository / MANIFEST_NAME
    if not manifest.is_file():
        return []
    with manifest.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source, delimiter="\t"))


def split(repository: Path) -> None:
    candidates = sorted(
        path
        for path in repository.rglob("*")
        if path.is_file()
        and path.name != MANIFEST_NAME
        and PART_MARKER not in path.name
        and path.stat().st_size > LIMIT
    )
    rows: list[dict[str, str]] = []
    for source in candidates:
        relative = source.relative_to(repository).as_posix()
        parts: list[str] = []
        with source.open("rb") as stream:
            index = 0
            while True:
                data = stream.read(PART_SIZE)
                if not data:
                    break
                part = source.with_name(f"{source.name}{PART_MARKER}{index:03d}")
                part.write_bytes(data)
                parts.append(part.relative_to(repository).as_posix())
                index += 1
        rows.append(
            {
                "path": relative,
                "size": str(source.stat().st_size),
                "sha256": digest(source),
                "parts": ",".join(parts),
            }
        )
        source.unlink()
        print(f"Split {relative} into {len(parts)} Git-safe parts")

    manifest = repository / MANIFEST_NAME
    with manifest.open("w", encoding="utf-8", newline="\n") as output:
        writer = csv.DictWriter(
            output,
            fieldnames=("path", "size", "sha256", "parts"),
            delimiter="\t",
            lineterminator="\n",
        )
        writer.writeheader()
        writer.writerows(rows)
    print(f"Split-artifact manifest: {len(rows)} oversized Maven files")


def restore(repository: Path) -> None:
    for row in read_manifest(repository):
        target = repository / row["path"]
        expected_size = int(row["size"])
        expected_digest = row["sha256"]
        if target.is_file():
            if target.stat().st_size != expected_size or digest(target) != expected_digest:
                raise SystemExit(f"Reconstructed Maven artifact is corrupt: {target}")
            continue
        parts = [repository / value for value in row["parts"].split(",") if value]
        if not parts or any(not part.is_file() for part in parts):
            raise SystemExit(f"Missing split part for {target}")
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_name(f".{target.name}.basefile-restoring-{os.getpid()}")
        try:
            with temporary.open("wb") as output:
                for part in parts:
                    with part.open("rb") as source:
                        for chunk in iter(lambda: source.read(1024 * 1024), b""):
                            output.write(chunk)
            if temporary.stat().st_size != expected_size or digest(temporary) != expected_digest:
                raise SystemExit(f"Split Maven artifact failed SHA-256 validation: {target}")
            temporary.replace(target)
        finally:
            temporary.unlink(missing_ok=True)
        print(f"Restored {row['path']} ({expected_size} bytes, SHA-256 verified)")


def clean(repository: Path) -> None:
    for row in read_manifest(repository):
        target = repository / row["path"]
        if not target.exists():
            continue
        if target.stat().st_size != int(row["size"]) or digest(target) != row["sha256"]:
            raise SystemExit(f"Refusing to remove corrupt reconstructed artifact: {target}")
        target.unlink()
        print(f"Removed reconstructed working-tree artifact {row['path']}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("split", "restore", "clean"))
    parser.add_argument("repository", type=Path)
    arguments = parser.parse_args()
    repository = arguments.repository.resolve()
    if not repository.is_dir():
        raise SystemExit(f"Maven repository is absent: {repository}")
    {"split": split, "restore": restore, "clean": clean}[arguments.mode](repository)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
