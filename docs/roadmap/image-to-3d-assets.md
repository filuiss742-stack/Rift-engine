# Image-to-3D and 2D-to-Voxel Asset Pipeline

Status: design proposal only. Rift Engine 0.5.0 does not currently import or render arbitrary 3D meshes.

## Product goal

Let a creator start from a regular image and choose one of two distinct outputs:

1. **Voxel object** — a Minecraft-like block volume for stylized props, terrain pieces, and simple destructible objects.
2. **Textured mesh** — a conventional 3D asset (for example GLB/glTF) for props and characters.

These are different workflows. A voxel conversion quantizes a silhouette/volume into cells; image-to-mesh reconstruction predicts unseen geometry and may invent details that are not present in the source image.

## Proposed pipeline

### A. Voxel conversion (engine-friendly first milestone)

- Preserve the original image as an immutable source asset.
- Let the creator choose a voxel resolution, palette limit, alpha threshold, and depth policy.
- Offer explicit depth modes: flat extrusion, per-pixel height map, and user-painted depth.
- Generate a compact voxel grid plus palette/material table; do not create one heavyweight scene object per voxel.
- Greedy-mesh adjacent equal-material faces into chunk meshes.
- Generate simplified collision proxies separately from render geometry.
- Validate bounds, palette indices, chunk dimensions, and memory budget before import.
- Keep the conversion deterministic: the same source and settings should produce the same voxel data.

### B. Image-to-mesh conversion (optional external tool)

- Run as an opt-in offline/desktop tool, not as an Android runtime dependency.
- Accept a masked image and optional front/side/back references.
- Export a GLB/glTF package with mesh, materials, textures, scale, and source metadata.
- Treat single-image backside geometry as a reconstruction guess. Require preview and creator approval.
- Run mesh validation before the asset can enter a game project: finite coordinates, valid indices, non-zero triangles, normals, UV/material references, texture dimensions, bounds, and configurable triangle limits.
- Produce a separate simplified collision mesh and optional LODs.
- Store model name, version, source URL, model-weight licence, generation settings, and source-image attribution in a sidecar manifest.
- Never bundle third-party model weights or silently download them in the engine.

## Candidate research (not an implementation decision)

- TripoSR is MIT-licensed and is a lightweight single-image mesh-generation candidate; reported hardware needs are around 6 GB VRAM, with vertex-colour output.
- TRELLIS.2 is MIT-licensed and targets higher-quality textured/PBR meshes, but reported GPU memory needs are substantially higher (roughly 16–24 GB).
- Hunyuan3D 2.1 can produce PBR output, but its Tencent Community Licence has conditions that must be reviewed for each intended use; reported memory ranges vary by stage and can be high.
- Stable Fast 3D is another fast UV-textured reconstruction candidate, but its Stability AI Community Licence and model access terms must be checked before adoption.

Model licences, weights, hardware requirements, and commercial terms can change. Re-check the upstream model card and licence at the time of integration; do not infer that an MIT code repository automatically makes its model weights MIT-licensed.

## Runtime integration boundary

The first integration should be an editor/import tool that produces validated, data-only assets. Runtime mesh rendering, glTF materials, skeletal animation, 3D physics, camera projection, and hybrid 2D/3D scene composition are separate engine milestones. Do not advertise them as implemented until they have public APIs, platform adapters, tests, and working Android/Windows examples.

## Required tests

- Deterministic voxel output for fixed input/settings.
- Transparent pixels and palette overflow handled predictably.
- Large images rejected or downscaled under an explicit memory budget.
- Greedy meshing preserves visible boundaries and material IDs.
- Generated collision proxies remain within configured error bounds.
- Invalid GLB/mesh data is rejected with actionable diagnostics.
- Missing textures, invalid UVs, NaN/Infinity vertices, degenerate triangles, and oversized assets are covered.
- Android and Windows consume the same validated asset format; platform-specific loading stays behind adapters.
- Import failures never corrupt the original source asset or project manifest.

## Release sequencing

1. Define the voxel data format and validator.
2. Build a small desktop conversion/import tool and preview.
3. Add tests and an example project that consumes voxel assets through public APIs.
4. Only then evaluate an external image-to-mesh backend.
5. Add runtime mesh rendering as a separately gated 3D milestone, not as part of the 2D 0.5.0 promise.
