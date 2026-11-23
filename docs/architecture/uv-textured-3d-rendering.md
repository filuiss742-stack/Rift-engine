# UV-textured 3D rendering

Status: first CPU/platform implementation; not a production PBR renderer.

## Pipeline

- Mesh triangles may carry a platform-neutral `TextureResource` and normalized UV coordinates.
- `Software3DRenderer` projects world vertices and forwards their UVs with the screen-space triangle.
- Android maps the source bitmap triangle to the destination triangle with an Android Matrix and clips drawing to the destination path.
- Desktop maps the source image triangle with an affine transform and clips to the destination path.
- Textures remain owned by `AssetManager`; mesh triangles only reference the resource and must not close it.

## Mesh builders

- `texturedGroundPlaneMesh` assigns a dedicated texture and configurable repeat UVs to a ground surface.
- `texturedCuboidMesh` accepts six independently assigned face materials. It does not force one atlas or one image across all faces.
- Reuse a texture handle only when surfaces intentionally share the same physical material.

## Known limitations

- The current CPU renderer sorts triangles by average camera depth (painter order). It has no depth buffer, robust near-plane polygon clipping, back-face culling, or GPU batching.
- Platform textured-triangle mapping is affine within each triangle, not perspective-correct. Strong perspective distortion may be visible on large surfaces.
- There is no mipmap generation, anisotropic filtering, PBR shader, normal-map lighting, roughness/metalness evaluation, or automatic texture streaming.
- A missing UV image must not be replaced with a flattened room image or a procedural noise blob and presented as an approved material.
- The prologue's current scene still uses flat-color meshes until approved source textures are added and loaded. The new API is a rendering foundation, not evidence that the scene is already textured.

## Validation

- Unit tests cover six-face material assignment and repeat UVs.
- Desktop rendering test covers textured triangle mapping and clipping.
- Android rendering is compile/lint validated by CI; device screenshot validation is still required.
