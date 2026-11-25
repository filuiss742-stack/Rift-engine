#!/usr/bin/env python3
"""Pack independently authored 8-direction character frames into one atlas.

Source layout:
  <character>/source/<direction>/<action>/<character>_<direction>_<action>_<NNN>.png

Every direction is authored separately. This tool never mirrors, rotates, or invents views.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

try:
    from PIL import Image
except ImportError as exc:
    raise SystemExit("Pillow is required: python -m pip install Pillow") from exc

DIRECTIONS = ("front", "front_left", "left", "back_left", "back", "back_right", "right", "front_right")
ACTIONS = ("idle", "walk", "run", "attack", "dodge", "hit", "interact")
NAME = re.compile(r"^(?P<char>[a-z0-9_]+)_(?P<direction>front_left|back_left|back_right|front_right|front|back|left|right)_(?P<action>[a-z0-9]+)_(?P<frame>\\d{3})\\.png$", re.I)


class AssetError(Exception):
    pass


def collect(character_dir: Path, require_actions: tuple[str, ...]) -> tuple[list[dict[str, Any]], tuple[int, int]]:
    root = character_dir / "source"
    if not root.is_dir():
        raise AssetError(f"Missing source directory: {root}")
    frames: list[dict[str, Any]] = []
    size: tuple[int, int] | None = None
    groups: dict[tuple[str, str], list[int]] = {}
    for direction_dir in sorted(p for p in root.iterdir() if p.is_dir()):
        direction = direction_dir.name.lower()
        if direction not in DIRECTIONS:
            raise AssetError(f"Unknown direction folder: {direction_dir.name}")
        for action_dir in sorted(p for p in direction_dir.iterdir() if p.is_dir()):
            action = action_dir.name.lower()
            if action not in ACTIONS:
                raise AssetError(f"Unknown action folder: {action_dir.name}")
            for path in sorted(action_dir.glob("*.png")):
                match = NAME.match(path.name)
                if not match:
                    raise AssetError(f"Invalid filename: {path}")
                if match.group("char").lower() != character_dir.name.lower():
                    raise AssetError(f"Mixed character identity in {path.name}")
                if match.group("direction").lower() != direction or match.group("action").lower() != action:
                    raise AssetError(f"Filename/folder mismatch: {path}")
                frame_index = int(match.group("frame"))
                key = (direction, action)
                groups.setdefault(key, []).append(frame_index)
                try:
                    image = Image.open(path).convert("RGBA")
                except Exception as exc:
                    raise AssetError(f"Unreadable PNG {path}: {exc}") from exc
                if size is None:
                    size = image.size
                if image.size != size:
                    raise AssetError(f"Frame {path} is {image.size}; expected consistent canvas {size}")
                alpha = image.getchannel("A")
                if alpha.getextrema()[0] == 255:
                    raise AssetError(f"Opaque background: {path}")
                bounds = alpha.getbbox()
                if bounds is None:
                    raise AssetError(f"Empty frame: {path}")
                l, t, r, b = bounds
                if l < 2 or t < 2 or r > image.width - 2 or b > image.height - 2:
                    raise AssetError(f"Subject touches canvas edge: {path}, bounds={bounds}")
                frames.append({"direction": direction, "action": action, "index": frame_index, "path": path, "image": image})
    if not frames or size is None:
        raise AssetError(f"No PNG frames found in {root}")
    missing_directions = sorted(set(DIRECTIONS) - {d for d, _ in groups})
    if missing_directions:
        raise AssetError("Missing independently authored directions: " + ", ".join(missing_directions))
    actions_present = {a for _, a in groups}
    missing_actions = sorted(set(require_actions) - actions_present)
    if missing_actions:
        raise AssetError("Missing required actions: " + ", ".join(missing_actions))
    for action in actions_present:
        counts = []
        for direction in DIRECTIONS:
            indices = sorted(groups.get((direction, action), []))
            if not indices:
                raise AssetError(f"Action '{action}' is missing direction '{direction}'")
            if indices != list(range(len(indices))):
                raise AssetError(f"Frames for {direction}/{action} must start at 000 and be contiguous: {indices}")
            counts.append(len(indices))
        if len(set(counts)) != 1:
            raise AssetError(f"Frame count differs by direction for action '{action}': {dict(zip(DIRECTIONS, counts))}")
    frames.sort(key=lambda f: (DIRECTIONS.index(f["direction"]), ACTIONS.index(f["action"]), f["index"]))
    return frames, size


def build(character_dir: Path, output_dir: Path, columns: int = 8, gutter: int = 4,
          require_actions: tuple[str, ...] = ("idle", "walk", "attack", "dodge")) -> tuple[Path, Path]:
    if columns < 1 or gutter < 0:
        raise AssetError("columns must be positive and gutter non-negative")
    frames, (cw, ch) = collect(character_dir, require_actions)
    columns = min(columns, len(frames))
    rows = (len(frames) + columns - 1) // columns
    atlas = Image.new("RGBA", (columns*cw+(columns+1)*gutter, rows*ch+(rows+1)*gutter), (0,0,0,0))
    manifest: dict[str, Any] = {
        "schemaVersion": 2, "character": character_dir.name,
        "atlas": f"{character_dir.name}_directional_atlas.png",
        "canvas": {"width": cw, "height": ch},
        "pivot": {"x": cw//2, "y": ch-8},
        "directions": list(DIRECTIONS), "animations": {}
    }
    for slot, frame in enumerate(frames):
        col, row = slot % columns, slot // columns
        x, y = gutter + col*(cw+gutter), gutter + row*(ch+gutter)
        atlas.alpha_composite(frame["image"], (x,y))
        clip = manifest["animations"].setdefault(frame["action"], {}).setdefault(frame["direction"], {
            "loop": frame["action"] in ("idle","walk","run"),
            "fps": {"idle":4,"walk":8,"run":10}.get(frame["action"],8),
            "frames": []
        })
        clip["frames"].append({"index":frame["index"],"rect":[x,y,cw,ch],
                               "source":frame["path"].relative_to(character_dir).as_posix()})
    output_dir.mkdir(parents=True, exist_ok=True)
    atlas_path = output_dir / f"{character_dir.name}_directional_atlas.png"
    manifest_path = output_dir / f"{character_dir.name}_directional_atlas.json"
    atlas.save(atlas_path, "PNG", optimize=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\\n", encoding="utf-8")
    return atlas_path, manifest_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("character_dir", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--columns", type=int, default=8)
    parser.add_argument("--gutter", type=int, default=4)
    parser.add_argument("--require-actions", default="idle,walk,attack,dodge")
    args = parser.parse_args()
    required = tuple(x.strip() for x in args.require_actions.split(",") if x.strip())
    if any(x not in ACTIONS for x in required):
        parser.error(f"Unknown action; choose from {', '.join(ACTIONS)}")
    try:
        atlas, manifest = build(args.character_dir, args.output or args.character_dir/"atlas",
                                args.columns, args.gutter, required)
    except AssetError as exc:
        print(f"DIRECTIONAL ASSET VALIDATION FAILED: {exc}", file=sys.stderr)
        return 2
    print(f"DIRECTIONAL ASSET VALIDATION PASSED: {atlas}")
    print(f"MANIFEST: {manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
