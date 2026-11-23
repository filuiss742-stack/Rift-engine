# Material source validation

The source-art pipeline keeps every material image separate. Do not create one image containing a collection of unrelated textures.

## Manifest format

A material manifest is JSON:

```json
{
  "schema_version": 1,
  "minimum_width": 2048,
  "minimum_height": 2048,
  "tile_edge_mean_max": 12.0,
  "materials": [
    {
      "id": "apt_floor_oak",
      "tileable": false,
      "maps": {
        "base_color": "apt_floor_oak_base_color.png",
        "normal": "apt_floor_oak_normal.png",
        "roughness": "apt_floor_oak_roughness.png",
        "metallic": "apt_floor_oak_metallic.png",
        "ambient_occlusion": "apt_floor_oak_ao.png"
      }
    }
  ]
}
```

Only `base_color` is mandatory in the first pass. Add the other maps when authored. All supplied maps for one material must have identical dimensions and UV alignment. Unique surfaces should use their own material IDs; reusing one image for different IDs is rejected unless the validator is deliberately extended with an explicit shared-material declaration.

## Run validation

```bash
python -m pip install Pillow
python tools/assets/validate_materials.py path/to/materials.json
```

The validator checks:
- valid manifest structure and unique IDs;
- safe relative paths and image decoding;
- minimum source resolution;
- identical dimensions across PBR maps;
- accidental image reuse across different material IDs;
- edge continuity for materials explicitly marked tileable.

The edge test is a first-pass numerical check, not visual approval. A passing image still requires a 3×3 repeat preview and inspection at native resolution and in the game camera. A non-tileable image may be valid for a unique object or a single large UV surface.

## Candidate rejection record

The first generated oak-floor candidate is 1254×1254 and visibly repeats its plank layout in a 3×3 preview. It is not accepted as a production tile: it fails the minimum-resolution gate and has an obvious repeating visual pattern. It remains a reference candidate only and is not connected to the runtime.
