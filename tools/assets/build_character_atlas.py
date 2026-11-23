#!/usr/bin/env python3
"""Build one deterministic PNG atlas for exactly one character.

Input:
  assets/characters/<character>/source/<action>/<character>_<action>_<NNN>.png

The script never accepts mixed character directories, portraits, props, or location art.
It validates source frames before packing and preserves all originals.
Requires Pillow: python -m pip install Pillow
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

try:
    from PIL import Image, ImageChops, ImageStat
except ImportError as exc:  # pragma: no cover - exercised by CLI users
    raise SystemExit("Pillow is required. Install it with: python -m pip install Pillow") from exc

ACTIONS = ("idle", "walk", "run", "attack", "dodge", "hit", "interact")
FRAME_RE = re.compile(r"^(?P<char>[a-z0-9_]+)_(?P<action>[a-z0-9]+)_(?P<index>\\d{3})\\.png$", re.I)


class AssetError(Exception):
    pass


def load_sources(character_dir: Path, cell_size: tuple[int, int] | None, require_alpha: bool) -> tuple[list[dict[str, Any]], tuple[int, int]]:
    source_root = character_dir / "source"
    if not source_root.is_dir():
        raise AssetError(f"Missing source directory: {source_root}")

    frames: list[dict[str, Any]] = []
    inferred_size = cell_size
    seen: set[tuple[str, int]] = set()

    for action_dir in sorted(p for p in source_root.iterdir() if p.is_dir()):
        action = action_dir.name.lower()
        if action not in ACTIONS:
            raise AssetError(f"Unsupported action folder '{action_dir.name}'. Allowed: {', '.join(ACTIONS)}")
        files = sorted(action_dir.glob("*.png"))
        if not files:
            continue
        for path in files:
            match = FRAME_RE.match(path.name)
            if not match:
                raise AssetError(f"Invalid frame filename: {path}")
            if match.group("char").lower() != character_dir.name.lower():
                raise AssetError(f"Mixed character asset: {path.name} is not named for '{character_dir.name}'")
            if match.group("action").lower() != action:
                raise AssetError(f"Action name mismatch in filename: {path.name}")
            index = int(match.group("index"))
            key = (action, index)
            if key in seen:
                raise AssetError(f"Duplicate frame index: {action}/{index:03d}")
            seen.add(key)

            try:
                image = Image.open(path).convert("RGBA")
            except Exception as exc:
                raise AssetError(f"Unreadable PNG {path}: {exc}") from exc

            if inferred_size is None:
                inferred_size = image.size
            if image.size != inferred_size:
                raise AssetError(f"Frame {path} has size {image.size}; expected {inferred_size}")
            alpha = image.getchannel("A")
            amin, amax = alpha.getextrema()
            if require_alpha and amin == 255:
                raise AssetError(f"Frame has no transparency: {path}")
            bbox = alpha.getbbox()
            if bbox is None:
                raise AssetError(f"Frame is fully transparent: {path}")
            left, top, right, bottom = bbox
            w, h = image.size
            if left < 2 or top < 2 or right > w - 2 or bottom > h - 2:
                raise AssetError(f"Subject touches the canvas edge (clipping/padding risk): {path}; alpha bounds={bbox}, canvas={image.size}")
            frames.append({"action": action, "index": index, "path": path, "image": image, "alpha_range": [amin, amax]})

    if not frames:
        raise AssetError(f"No source PNG frames found under {source_root}")
    if inferred_size is None:
        raise AssetError("Could not determine frame size")

    frames.sort(key=lambda f: (ACTIONS.index(f["action"]), f["index"]))
    for action in ACTIONS:
        indices = [f["index"] for f in frames if f["action"] == action]
        if indices and indices != list(range(min(indices), max(indices) + 1)):
            raise AssetError(f"Missing frame number in action '{action}': found {indices}")

    # Exact/near duplicate check within each animation. Similarity is a warning threshold,
    # not a claim of semantic motion understanding.
    for action in ACTIONS:
        group = [f for f in frames if f["action"] == action]
        for previous, current in zip(group, group[1:]):
            diff = ImageChops.difference(previous["image"], current["image"])
            mean = sum(ImageStat.Stat(diff).mean) / 4.0
            if mean < 0.35:
                raise AssetError(f"Adjacent frames are near-identical: {previous['path'].name} and {current['path'].name} (mean pixel difference {mean:.3f})")

    return frames, inferred_size


def build(character_dir: Path, output_dir: Path, cell_size: tuple[int, int] | None, columns: int, gutter: int, require_alpha: bool) -> tuple[Path, Path]:
    if columns < 1 or gutter < 0:
        raise AssetError("columns must be >= 1 and gutter must be >= 0")
    character = character_dir.name
    frames, (cell_w, cell_h) = load_sources(character_dir, cell_size, require_alpha)
    columns = min(columns, len(frames))
    rows = (len(frames) + columns - 1) // columns
    atlas_w = columns * cell_w + (columns + 1) * gutter
    atlas_h = rows * cell_h + (rows + 1) * gutter
    atlas = Image.new("RGBA", (atlas_w, atlas_h), (0, 0, 0, 0))

    manifest: dict[str, Any] = {
        "schemaVersion": 1,
        "character": character,
        "atlas": f"{character}_atlas.png",
        "canvas": {"width": cell_w, "height": cell_h},
        "atlasSize": {"width": atlas_w, "height": atlas_h},
        "pivot": {"x": cell_w // 2, "y": cell_h - 8},
        "animations": {},
    }

    for slot, frame in enumerate(frames):
        col, row = slot % columns, slot // columns
        x, y = gutter + col * (cell_w + gutter), gutter + row * (cell_h + gutter)
        atlas.alpha_composite(frame["image"], (x, y))
        animation = manifest["animations"].setdefault(frame["action"], {
            "loop": frame["action"] in ("idle", "walk", "run"),
            "fps": {"idle": 4, "walk": 8, "run": 10}.get(frame["action"], 8),
            "frames": [],
        })
        animation["frames"].append({
            "source": frame["path"].relative_to(character_dir).as_posix(),
            "index": frame["index"],
            "rect": [x, y, cell_w, cell_h],
            "durationMs": round(1000 / animation["fps"]),
        })

    output_dir.mkdir(parents=True, exist_ok=True)
    atlas_path = output_dir / f"{character}_atlas.png"
    manifest_path = output_dir / f"{character}_atlas.json"
    atlas.save(atlas_path, format="PNG", optimize=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return atlas_path, manifest_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("character_dir", type=Path, help="One character folder containing source/<action>/*.png")
    parser.add_argument("--output", type=Path, help="Output directory (default: <character_dir>/atlas)")
    parser.add_argument("--columns", type=int, default=4, help="Maximum atlas columns (default: 4)")
    parser.add_argument("--gutter", type=int, default=8, help="Transparent spacing between cells (default: 8)")
    parser.add_argument("--cell-width", type=int)
    parser.add_argument("--cell-height", type=int)
    parser.add_argument("--allow-opaque", action="store_true", help="Allow source frames without alpha transparency")
    args = parser.parse_args()

    if (args.cell_width is None) != (args.cell_height is None):
        parser.error("--cell-width and --cell-height must be supplied together")
    size = (args.cell_width, args.cell_height) if args.cell_width else None
    output = args.output or (args.character_dir / "atlas")
    try:
        atlas, manifest = build(args.character_dir, output, size, args.columns, args.gutter, not args.allow_opaque)
    except AssetError as exc:
        print(f"ASSET VALIDATION FAILED: {exc}", file=sys.stderr)
        return 2
    print(f"ASSET VALIDATION PASSED: {atlas}")
    print(f"MANIFEST: {manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
