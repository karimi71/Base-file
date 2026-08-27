#!/usr/bin/env python3
"""Audit future-stack coordinates, native classifiers, transitives, and lock pins."""
from __future__ import annotations

import csv
import re
import sys
from pathlib import Path


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source, delimiter="\t"))


def expected_version(group: str, artifact: str) -> str | None:
    exact_groups = {
        "androidx.sqlite": "2.5.2",
        "androidx.datastore": "1.1.7",
        "io.mockk": "1.13.13",
        "org.junit.jupiter": "5.11.4",
        "org.junit.vintage": "5.11.4",
        "org.junit.platform": "1.11.4",
        "io.github.takahirom.roborazzi": "1.39.0",
        "io.coil-kt.coil3": "3.1.0",
        "com.google.crypto.tink": "1.15.0",
        "androidx.security": "1.1.0-alpha06",
        "com.squareup.moshi": "1.15.2",
        "androidx.work": "2.10.0",
        "androidx.room": "2.7.2",
        "io.kotest": "5.9.1",
        "org.jetbrains.kotlinx": None,
    }
    if group == "org.jetbrains.kotlinx" and artifact.startswith("kotlinx-datetime"):
        return "0.6.1"
    if group == "com.google.protobuf":
        if artifact in {"protobuf-gradle-plugin", "com.google.protobuf.gradle.plugin"}:
            return "0.9.4"
        if artifact.startswith("protobuf-") or artifact == "protoc":
            return "4.29.3"
    if group == "de.mannodermaus.gradle.plugins" or group == "de.mannodermaus.android-junit5":
        return "1.11.2.0"
    if group == "com.google.android.apps.common.testing.accessibility.framework":
        return "4.1.1"
    if group == "androidx.test.espresso" and artifact == "espresso-accessibility":
        return "3.6.1"
    if group == "com.tom-roush" and artifact == "pdfbox-android":
        return "2.0.27.0"
    if group == "com.google.code.gson" and artifact == "gson":
        return "2.11.0"
    if group == "androidx.compose.material" and artifact.startswith("material-icons-extended"):
        return "1.7.6"
    if group == "net.jqwik" and artifact.startswith("jqwik"):
        return "1.9.2"
    if group == "com.github.jk1" or group == "com.github.jk1.dependency-license-report":
        return "2.9"
    return exact_groups.get(group)


def main() -> int:
    if len(sys.argv) < 6:
        print(
            "usage: verify_future_coordinates.py REQUESTED NATIVE MANIFEST MAVEN LOCKFILE...",
            file=sys.stderr,
        )
        return 2
    requested_path, native_path, manifest_path, repository_path = map(Path, sys.argv[1:5])
    lock_paths = [Path(value) for value in sys.argv[5:]]

    requested = rows(requested_path)
    manifest = rows(manifest_path)
    available = {(row["group"], row["artifact"], row["version"]): row for row in manifest}
    missing = [
        f"{row['group']}:{row['artifact']}:{row['version']}"
        for row in requested
        if (row["group"], row["artifact"], row["version"]) not in available
    ]
    if missing:
        raise SystemExit("Missing future coordinates:\n  " + "\n  ".join(missing))

    classifiers = rows(native_path)
    for row in classifiers:
        filename = (
            f"{row['artifact']}-{row['version']}-{row['classifier']}.{row['extension']}"
        )
        target = (
            repository_path
            / Path(*row["group"].split("."))
            / row["artifact"]
            / row["version"]
            / filename
        )
        if not target.is_file() or target.stat().st_size < 1_000_000:
            raise SystemExit(f"Native classifier is absent or implausibly small: {target}")

    required_transitives = {
        ("net.bytebuddy", "byte-buddy"),
        ("net.bytebuddy", "byte-buddy-agent"),
        ("org.objenesis", "objenesis"),
        ("com.squareup.okhttp3", "okhttp"),
        ("com.squareup.okio", "okio"),
        ("com.squareup.okio", "okio-jvm"),
    }
    available_ga = {(row["group"], row["artifact"]) for row in manifest}
    absent_transitives = sorted(required_transitives - available_ga)
    if absent_transitives:
        raise SystemExit(
            "Required selected transitives missing: "
            + ", ".join(f"{group}:{artifact}" for group, artifact in absent_transitives)
        )

    lock_pattern = re.compile(r"^([^:#]+):([^:]+):([^=]+)=")
    checked_locks = 0
    mismatches: list[str] = []
    for lock_path in lock_paths:
        if not lock_path.is_file():
            raise SystemExit(f"Expected dependency lockfile is missing: {lock_path}")
        for line in lock_path.read_text(encoding="utf-8").splitlines():
            match = lock_pattern.match(line)
            if not match:
                continue
            group, artifact, selected = match.groups()
            expected = expected_version(group, artifact)
            if expected is not None and selected != expected:
                mismatches.append(
                    f"{lock_path}: {group}:{artifact}:{selected}, expected {expected}"
                )
            if expected is not None:
                checked_locks += 1
    if mismatches:
        raise SystemExit("Mixed future-family versions:\n  " + "\n  ".join(mismatches))

    print(
        f"Future coordinate audit: {len(requested)} requested coordinates, "
        f"{len(classifiers)} native classifiers, {len(required_transitives)} required "
        f"transitives, and {checked_locks} family-pinned lock entries verified"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
