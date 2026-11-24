# Rift Procedural Asset Generator

`tools/assets/generate_procedural_material.py` creates deterministic, tileable material maps without a hosted image model or GPU. It uses Pillow plus the Python standard library.

## Supported material families

- `stone` — cool grey masonry with block variation
- `soil` — warm granular earth
- `grass` — green ground cover variation
- `wood` — directional grain
- `metal` — cool brushed metal

## Generate

```bash
python -m pip install Pillow
python tools/assets/generate_procedural_material.py   --material stone   --size 512   --seed 42   --output app/src/main/assets/materials/stone
```

Each run writes separate PNGs for `base_color`, `normal`, `roughness`, `metallic`, and `height`, plus `material.json`. Output is deterministic for a fixed material, size, and seed. Supported dimensions are powers of two from 64 through 2048.

## Generate and commit from GitHub Actions

There are two ways to start a generation:

1. Open **Actions → Generate procedural material → Run workflow** on `development/0.7.0`.
2. Create a JSON request under `tools/assets/generation-requests/` with `material`, `size`, and `seed`. A push of that request starts the same workflow, allowing an assistant or automation to queue a generation through the repository.

The workflow generates five PNG maps, validates the PBR manifest, and opens a pull request against `development/0.7.0` with the candidate under:

`app/src/main/assets/generated/material-candidates/<material>-<seed>/`

The development branch is protected, so the workflow does not bypass its rules or push directly to it. Generated candidates remain outside production material directories until they are reviewed and deliberately promoted.

## Validation

Run the existing material validator against the generated manifest:

```bash
python tools/assets/validate_materials.py app/src/main/assets/materials/stone/material.json
```

The generator duplicates border pixels to make the UV repeat seam exact. The manifest records a zero edge-difference threshold; validation still checks the actual PNGs, dimensions, unique map assignments, safe paths, and tile edge continuity.

## Scope and limitations

This is procedural raster generation, not neural illustration. It can create useful stylized material foundations and repeatable PBR map sets; it does not understand artistic composition, model UVs, or scene lighting. The maps are candidate assets and must be visually reviewed in the target renderer. Normal maps are derived from the generated height field; they are not physically scanned surface data. The height map is emitted for future shader/material workflows but is not currently part of the runtime PBR manifest schema.

No generated output is automatically promoted into production game assets. Keep the generator seed and source script so an asset can be reproduced and tuned.
