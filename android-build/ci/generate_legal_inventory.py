#!/usr/bin/env python3
"""Generate a coordinate/license inventory and aggregate embedded NOTICE files."""
from __future__ import annotations

import csv
import hashlib
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


def pom_text(root: ET.Element, name: str) -> str:
    node = root.find(f"{{*}}{name}")
    return (node.text or "").strip() if node is not None else ""


def licenses_from_pom(path: Path) -> str:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return ""
    values: list[str] = []
    for license_node in root.findall(".//{*}licenses/{*}license"):
        name = pom_text(license_node, "name")
        url = pom_text(license_node, "url")
        value = name
        if url:
            value += f" ({url})" if value else url
        if value and value not in values:
            values.append(value)
    return "; ".join(values)


def main() -> int:
    if len(sys.argv) != 4:
        print(f"usage: {sys.argv[0]} MAVEN_REPO INVENTORY_TSV NOTICES_TXT", file=sys.stderr)
        return 2
    maven = Path(sys.argv[1]).resolve()
    inventory_path = Path(sys.argv[2]).resolve()
    notices_path = Path(sys.argv[3]).resolve()
    coordinate_manifest = maven / "BASE_FILE_COORDINATES.tsv"
    rows: list[dict[str, str]] = []
    notices: dict[str, tuple[str, str]] = {}

    with coordinate_manifest.open(encoding="utf-8") as source:
        reader = csv.DictReader(source, delimiter="\t")
        for row in reader:
            group, artifact, version = row["group"], row["artifact"], row["version"]
            directory = maven.joinpath(*group.split("."), artifact, version)
            poms = sorted(directory.glob("*.pom"))
            license_text = next((licenses_from_pom(p) for p in poms if licenses_from_pom(p)), "")
            if not license_text:
                # AndroidX, Kotlin, AGP, and most of this graph use Apache-2.0;
                # UNKNOWN is intentional when upstream omitted POM license metadata.
                license_text = "UNKNOWN - inspect embedded metadata/NOTICE"
            files = [f for f in sorted(directory.iterdir()) if f.is_file()]
            binary_files = [f.name for f in files if f.suffix in {".jar", ".aar", ".klib", ".so"}]
            rows.append(
                {
                    "group": group,
                    "artifact": artifact,
                    "version": version,
                    "license": license_text,
                    "binary_files": ",".join(binary_files),
                }
            )
            coordinate = f"{group}:{artifact}:{version}"
            for archive in files:
                if archive.suffix not in {".jar", ".aar", ".zip"}:
                    continue
                try:
                    with zipfile.ZipFile(archive) as zipped:
                        for member in zipped.namelist():
                            normalized = member.upper().rstrip("/")
                            basename = normalized.rsplit("/", 1)[-1]
                            if not (basename == "NOTICE" or basename.startswith("NOTICE.")):
                                continue
                            data = zipped.read(member)
                            if len(data) > 2 * 1024 * 1024:
                                continue
                            text = data.decode("utf-8", errors="replace").strip()
                            if not text:
                                continue
                            key = hashlib.sha256(data).hexdigest()
                            notices.setdefault(key, (coordinate, text))
                except zipfile.BadZipFile:
                    pass

    inventory_path.parent.mkdir(parents=True, exist_ok=True)
    with inventory_path.open("w", encoding="utf-8", newline="\n") as out:
        out.write("group\tartifact\tversion\tlicense\tbinary_files\n")
        for row in sorted(rows, key=lambda r: (r["group"], r["artifact"], r["version"])):
            out.write("\t".join(row.values()) + "\n")

    notices_path.parent.mkdir(parents=True, exist_ok=True)
    with notices_path.open("w", encoding="utf-8", newline="\n") as out:
        out.write("THIRD-PARTY NOTICES EXTRACTED FROM VENDORED MAVEN ARTIFACTS\n")
        out.write("Generated without modifying upstream notice text.\n\n")
        for key, (coordinate, text) in sorted(notices.items(), key=lambda item: item[1][0]):
            out.write("=" * 78 + "\n")
            out.write(f"Artifact: {coordinate}\nEmbedded NOTICE SHA-256: {key}\n")
            out.write("-" * 78 + "\n")
            out.write(text + "\n\n")

    print(f"Legal inventory: {len(rows)} coordinates, {len(notices)} unique NOTICE texts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
