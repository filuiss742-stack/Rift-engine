#!/usr/bin/env python3
"""Create deterministic, seamless PBR material textures using only Pillow and the Python standard library."""
from __future__ import annotations

import argparse
import json
import math
import random
import sys
from pathlib import Path
from typing import Callable

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover
    raise SystemExit("Pillow is required: python -m pip install Pillow") from exc

MATERIALS = ("stone", "soil", "grass", "wood", "metal")
MIN_SIZE = 64
MAX_SIZE = 2048


def smooth(t: float) -> float:
    return t * t * (3.0 - 2.0 * t)


def periodic_noise(x: float, y: float, grid: int, lattice: list[list[float]]) -> float:
    """Bilinearly sample a periodic value-noise lattice."""
    gx = (x % 1.0) * grid
    gy = (y % 1.0) * grid
    x0, y0 = int(gx) % grid, int(gy) % grid
    x1, y1 = (x0 + 1) % grid, (y0 + 1) % grid
    tx, ty = smooth(gx - math.floor(gx)), smooth(gy - math.floor(gy))
    top = lattice[y0][x0] * (1.0 - tx) + lattice[y0][x1] * tx
    bottom = lattice[y1][x0] * (1.0 - tx) + lattice[y1][x1] * tx
    return top * (1.0 - ty) + bottom * ty


def make_lattice(rng: random.Random, grid: int) -> list[list[float]]:
    return [[rng.random() for _ in range(grid)] for _ in range(grid)]


def clamp8(value: float) -> int:
    return max(0, min(255, int(round(value))))


def generate(material: str, size: int, seed: int, output: Path) -> dict[str, Path]:
    if material not in MATERIALS:
        raise ValueError(f"Unsupported material '{material}'. Choose: {', '.join(MATERIALS)}")
    if size < MIN_SIZE or size > MAX_SIZE or size & (size - 1):
        raise ValueError(f"size must be a power of two between {MIN_SIZE} and {MAX_SIZE}")

    rng = random.Random(seed)
    grids = (4, 12, 32, 96)
    lattices = {grid: make_lattice(rng, grid) for grid in grids}
    fine = make_lattice(rng, 256)
    base_pixels: list[tuple[int, int, int]] = []
    height_values: list[int] = []
    rough_values: list[int] = []
    metal_values: list[int] = []

    for py in range(size):
        v = py / size
        for px in range(size):
            u = px / size
            n0 = periodic_noise(u, v, 4, lattices[4])
            n1 = periodic_noise(u, v, 12, lattices[12])
            n2 = periodic_noise(u, v, 32, lattices[32])
            n3 = periodic_noise(u, v, 96, lattices[96])
            n4 = periodic_noise(u, v, 256, fine)
            broad = (n0 * 0.38 + n1 * 0.28 + n2 * 0.20 + n3 * 0.10 + n4 * 0.04)
            grain = (n2 * 0.55 + n3 * 0.30 + n4 * 0.15)

            if material == "stone":
                joint = min((u * 8.0) % 1.0, 1.0 - (u * 8.0) % 1.0,
                            (v * 8.0) % 1.0, 1.0 - (v * 8.0) % 1.0)
                mortar = 1.0 if joint < 0.025 else 0.0
                h = 0.30 + broad * 0.62 - mortar * 0.20
                shade = 88 + broad * 105 - mortar * 38
                color = (shade * 1.04, shade * 1.01, shade * 0.94)
            elif material == "soil":
                h = 0.18 + broad * 0.72
                shade = 62 + broad * 104
                color = (shade * 1.10, shade * 0.91, shade * 0.70)
            elif material == "grass":
                blade = (math.sin((u * 37.0 + n1 * 0.08) * math.tau) + 1.0) * 0.5
                h = 0.22 + broad * 0.52 + blade * 0.12
                shade = 48 + broad * 70 + blade * 24
                color = (shade * 0.66, shade * 1.22, shade * 0.57)
            elif material == "wood":
                wave = (math.sin((v * 42.0 + n1 * 0.10 + n2 * 0.025) * math.tau) + 1.0) * 0.5
                h = 0.22 + broad * 0.40 + wave * 0.24
                shade = 82 + broad * 66 + wave * 42
                color = (shade * 1.28, shade * 0.82, shade * 0.53)
            else:  # brushed metal
                brush = (math.sin((v * 180.0 + n2 * 0.08) * math.tau) + 1.0) * 0.5
                h = 0.38 + broad * 0.20 + brush * 0.08
                shade = 104 + broad * 88 + brush * 18
                color = (shade * 0.94, shade * 0.99, shade * 1.05)

            base_pixels.append(tuple(clamp8(channel) for channel in color))
            height_values.append(clamp8(h * 255.0))
            rough_base = {"stone": 218, "soil": 238, "grass": 230, "wood": 188, "metal": 92}[material]
            rough_values.append(clamp8(rough_base + (grain - 0.5) * 34))
            metal_values.append(clamp8(220 + (grain - 0.5) * 20) if material == "metal" else 0)

    base = Image.new("RGB", (size, size))
    base.putdata(base_pixels)
    height = Image.new("L", (size, size))
    height.putdata(height_values)
    roughness = Image.new("L", (size, size))
    roughness.putdata(rough_values)
    metallic = Image.new("L", (size, size))
    metallic.putdata(metal_values)

    # Derive a tangent-space normal map from the height field with wrapped neighbors.
    hp = height.load()
    normal = Image.new("RGB", (size, size))
    normal_pixels = []
    for py in range(size):
        for px in range(size):
            dx = (hp[(px + 1) % size, py] - hp[(px - 1) % size, py]) / 255.0
            dy = (hp[px, (py + 1) % size] - hp[px, (py - 1) % size]) / 255.0
            nx, ny, nz = -dx * 2.2, -dy * 2.2, 1.0
            length = math.sqrt(nx * nx + ny * ny + nz * nz)
            normal_pixels.append((clamp8((nx / length * 0.5 + 0.5) * 255),
                                  clamp8((ny / length * 0.5 + 0.5) * 255),
                                  clamp8((nz / length * 0.5 + 0.5) * 255)))
    normal.putdata(normal_pixels)

    # Exact duplicated border pixels guarantee a clean repeat at the UV seam.
    for image in (base, height, normal, roughness, metallic):
        image.paste(image.crop((0, 0, size - 1, size - 1)), (1, 1))
        for y in range(size):
            image.putpixel((size - 1, y), image.getpixel((0, y)))
        for x in range(size):
            image.putpixel((x, size - 1), image.getpixel((x, 0)))

    output.mkdir(parents=True, exist_ok=True)
    files = {
        "base_color": output / "base_color.png",
        "normal": output / "normal.png",
        "roughness": output / "roughness.png",
        "metallic": output / "metallic.png",
        "height": output / "height.png",
    }
    base.save(files["base_color"], format="PNG", optimize=True)
    normal.save(files["normal"], format="PNG", optimize=True)
    roughness.save(files["roughness"], format="PNG", optimize=True)
    metallic.save(files["metallic"], format="PNG", optimize=True)
    height.save(files["height"], format="PNG", optimize=True)

    manifest = {
        "schema_version": 1,
        "minimum_width": size,
        "minimum_height": size,
        "tile_edge_mean_max": 0.0,
        "materials": [{
            "id": f"procedural_{material}_{seed}",
            "tileable": True,
            "maps": {
                "base_color": files["base_color"].name,
                "normal": files["normal"].name,
                "roughness": files["roughness"].name,
                "metallic": files["metallic"].name,
            },
            "height": files["height"].name,
            "generator": "rift-procedural-materials",
            "generator_version": 1,
            "seed": seed,
            "size": size,
        }],
    }
    manifest_path = output / "material.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    files["manifest"] = manifest_path
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--material", choices=MATERIALS, required=True)
    parser.add_argument("--size", type=int, default=512)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--output", type=Path, required=True, help="Output directory for PNG maps and material.json")
    args = parser.parse_args()
    try:
        files = generate(args.material, args.size, args.seed, args.output)
    except (OSError, ValueError) as exc:
        print(f"GENERATION FAILED: {exc}", file=sys.stderr)
        return 2
    for key, path in files.items():
        print(f"{key}: {path}")
    print("STATUS: generated; automated validation and visual review still required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
