#!/usr/bin/env python3
"""Validate one independently-authored Rift character image or a set of such files.

This checks measurable image properties. It cannot certify anatomy, identity, artistic
quality, or that the image depicts exactly one subject; those require visual review.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image

PROFILES = {
    "portrait": {"min": (256, 256), "max": (2048, 2048), "ratio": (0.72, 1.28)},
    "bust": {"min": (256, 320), "max": (2048, 3072), "ratio": (0.45, 1.05)},
    "full_body": {"min": (256, 512), "max": (2048, 4096), "ratio": (0.22, 0.85)},
    "sprite_frame": {"min": (256, 256), "max": (2048, 2048), "ratio": (0.35, 1.45)},
    "face_expression": {"min": (256, 256), "max": (2048, 2048), "ratio": (0.72, 1.28)},
    "face_component": {"min": (64, 64), "max": (1024, 1024), "ratio": (0.25, 4.0)},
    "icon": {"min": (64, 64), "max": (1024, 1024), "ratio": (0.5, 2.0)},
    "vfx": {"min": (128, 128), "max": (2048, 2048), "ratio": (0.25, 4.0)},
}

@dataclass
class ImageReport:
    path: str
    profile: str
    width: int
    height: int
    mode: str
    alpha_bounds: tuple[int, int, int, int] | None
    alpha_occupancy: float
    sha256: str
    errors: list[str]
    warnings: list[str]


def validate_image(path: Path, profile: str, padding: int = 2, require_alpha: bool = True) -> ImageReport:
    if profile not in PROFILES:
        raise ValueError(f"Unsupported profile '{profile}'. Choose: {', '.join(PROFILES)}")
    errors: list[str] = []
    warnings: list[str] = []
    try:
        raw = Image.open(path)
        raw.verify()
        image = Image.open(path)
        image.load()
    except Exception as exc:
        return ImageReport(str(path), profile, 0, 0, "unknown", None, 0.0, "", [f"Unreadable image: {exc}"], [])

    if path.suffix.lower() != ".png":
        errors.append("Source must be PNG; keep JPEG/WebP exports out of the source-art pipeline")
    width, height = image.size
    spec = PROFILES[profile]
    min_w, min_h = spec["min"]
    max_w, max_h = spec["max"]
    if width < min_w or height < min_h:
        errors.append(f"Image too small: {width}x{height}; minimum is {min_w}x{min_h}")
    if width > max_w or height > max_h:
        errors.append(f"Image exceeds configured maximum: {width}x{height}; maximum is {max_w}x{max_h}")
    ratio = width / max(height, 1)
    low, high = spec["ratio"]
    if not low <= ratio <= high:
        errors.append(f"Aspect ratio {ratio:.3f} is outside profile range {low:.2f}..{high:.2f}")

    if "A" not in image.getbands():
        if require_alpha:
            errors.append("PNG has no alpha channel; transparent character art is required")
        alpha = Image.new("L", image.size, 255)
    else:
        alpha = image.getchannel("A")
    alpha_min, alpha_max = alpha.getextrema()
    if require_alpha and alpha_min == 255:
        errors.append("Image is fully opaque; expected transparent background")
    bounds = alpha.getbbox()
    if bounds is None:
        errors.append("Image is fully transparent")
        occupancy = 0.0
    else:
        left, top, right, bottom = bounds
        occupancy = ((right - left) * (bottom - top)) / max(width * height, 1)
        if left < padding or top < padding or right > width - padding or bottom > height - padding:
            errors.append(f"Visible pixels touch safety padding ({padding}px); clipping risk, alpha bounds={bounds}")
        if occupancy < 0.08:
            warnings.append(f"Subject occupies only {occupancy:.1%} of the canvas; may be too small in-game")
        if occupancy > 0.94:
            warnings.append(f"Subject occupies {occupancy:.1%} of the canvas; little room remains for animation")
    if alpha_max == 0:
        errors.append("Image contains no visible pixels")

    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return ImageReport(str(path), profile, width, height, image.mode, bounds, occupancy, digest, errors, warnings)


def validate_many(paths: Iterable[Path], profile: str, padding: int, require_alpha: bool) -> dict:
    reports = [validate_image(p, profile, padding, require_alpha) for p in paths]
    seen: dict[str, str] = {}
    for report in reports:
        if report.sha256:
            if report.sha256 in seen:
                report.errors.append(f"Exact duplicate of {seen[report.sha256]}")
            else:
                seen[report.sha256] = report.path
    return {
        "schemaVersion": 1,
        "profile": profile,
        "passed": all(not r.errors for r in reports),
        "summary": {
            "files": len(reports),
            "passed": sum(not r.errors for r in reports),
            "failed": sum(bool(r.errors) for r in reports),
            "warnings": sum(len(r.warnings) for r in reports),
        },
        "images": [asdict(r) for r in reports],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("images", nargs="+", type=Path, help="Individual PNG files; do not pass a contact sheet")
    parser.add_argument("--profile", required=True, choices=sorted(PROFILES))
    parser.add_argument("--padding", type=int, default=2)
    parser.add_argument("--allow-opaque", action="store_true")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    if args.padding < 0:
        parser.error("--padding must be >= 0")
    result = validate_many(args.images, args.profile, args.padding, not args.allow_opaque)
    rendered = json.dumps(result, ensure_ascii=False, indent=2)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    print(rendered)
    return 0 if result["passed"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
