#!/usr/bin/env python3
"""Validate independently authored PBR texture sets before they enter the game."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - environment dependent
    raise SystemExit("Pillow is required: python -m pip install Pillow") from exc

MAP_KEYS = ("base_color", "normal", "roughness", "metallic", "ambient_occlusion")


def edge_difference(image: Image.Image) -> tuple[float, float]:
    """Return mean absolute RGB difference across opposing horizontal/vertical edges."""
    rgb = image.convert("RGB")
    width, height = rgb.size
    pixels = rgb.load()
    horizontal_total = 0
    vertical_total = 0
    channels = 0
    for y in range(height):
        for channel in range(3):
            horizontal_total += abs(pixels[0, y][channel] - pixels[width - 1, y][channel])
            channels += 1
    for x in range(width):
        for channel in range(3):
            vertical_total += abs(pixels[x, 0][channel] - pixels[x, height - 1][channel])
    return horizontal_total / channels, vertical_total / (width * 3)


def validate_manifest(manifest_path: Path) -> list[str]:
    errors: list[str] = []
    warnings: list[str] = []
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return [f"Cannot read manifest: {exc}"]

    if manifest.get("schema_version") != 1:
        errors.append("schema_version must be 1")
    materials = manifest.get("materials")
    if not isinstance(materials, list) or not materials:
        return errors + ["materials must be a non-empty array"]

    root = manifest_path.parent
    seen_ids: set[str] = set()
    seen_paths: dict[str, str] = {}
    min_width = int(manifest.get("minimum_width", 2048))
    min_height = int(manifest.get("minimum_height", 2048))
    tile_threshold = float(manifest.get("tile_edge_mean_max", 12.0))

    for index, material in enumerate(materials):
        label = f"materials[{index}]"
        if not isinstance(material, dict):
            errors.append(f"{label} must be an object")
            continue
        material_id = material.get("id")
        if not isinstance(material_id, str) or not material_id:
            errors.append(f"{label}.id must be a non-empty string")
            continue
        if material_id in seen_ids:
            errors.append(f"duplicate material id: {material_id}")
        seen_ids.add(material_id)

        maps = material.get("maps")
        if not isinstance(maps, dict) or not isinstance(maps.get("base_color"), str):
            errors.append(f"{material_id}: maps.base_color is required")
            continue

        dimensions: set[tuple[int, int]] = set()
        opened_base: Image.Image | None = None
        for map_name in MAP_KEYS:
            relative = maps.get(map_name)
            if relative is None:
                continue
            if not isinstance(relative, str) or not relative:
                errors.append(f"{material_id}: maps.{map_name} must be a relative path")
                continue
            path = (root / relative).resolve()
            if root.resolve() not in path.parents:
                errors.append(f"{material_id}: maps.{map_name} escapes the manifest directory")
                continue
            if not path.is_file():
                errors.append(f"{material_id}: missing {map_name} image: {relative}")
                continue
            previous = seen_paths.get(str(path))
            if previous and previous != material_id:
                errors.append(
                    f"{material_id}: image {relative} is also assigned to {previous}; "
                    "reuse only when both surfaces intentionally share one physical material"
                )
            seen_paths[str(path)] = material_id
            try:
                with Image.open(path) as source:
                    source.verify()
                with Image.open(path) as source:
                    dimensions.add(source.size)
                    if source.width < min_width or source.height < min_height:
                        errors.append(
                            f"{material_id}: {map_name} is {source.width}x{source.height}; "
                            f"minimum is {min_width}x{min_height}"
                        )
                    if map_name == "base_color":
                        opened_base = source.convert("RGB")
            except Exception as exc:  # Pillow exposes several decoder exceptions
                errors.append(f"{material_id}: cannot decode {map_name} image {relative}: {exc}")

        if len(dimensions) > 1:
            errors.append(f"{material_id}: PBR maps must have identical dimensions, got {sorted(dimensions)}")

        if material.get("tileable") is True and opened_base is not None:
            horizontal, vertical = edge_difference(opened_base)
            if horizontal > tile_threshold or vertical > tile_threshold:
                errors.append(
                    f"{material_id}: tileable base color has visible edge mismatch "
                    f"(horizontal={horizontal:.2f}, vertical={vertical:.2f}, "
                    f"limit={tile_threshold:.2f})"
                )

        if material.get("tileable") not in (True, False):
            errors.append(f"{material_id}: tileable must be explicitly true or false")

    for warning in warnings:
        print(f"WARNING: {warning}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="Path to a material manifest JSON file")
    args = parser.parse_args()
    errors = validate_manifest(args.manifest)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        print(f"Material validation failed: {len(errors)} error(s).", file=sys.stderr)
        return 1
    print("Material manifest validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
