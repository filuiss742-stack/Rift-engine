# Hybrid 3D World and Directional 2D Sprite Rendering

Status: architecture contract and staged implementation plan. This document does not claim that the 3D renderer, 3D physics, model importer, or sprite-view selector are already implemented.

## Target visual model

Rift's target presentation is a real 3D world populated by 2D animated characters and entities.

- Buildings, rooms, floors, walls, ceilings, roads, terrain, furniture, doors, vegetation, and ordinary physical props are 3D geometry.
- These world objects use materials and image textures. A texture is surface data applied to geometry; it is not a flat background image pretending to be a room or object.
- Characters, creatures, monsters, and selected visual entities remain 2D artwork. They are rendered on camera-aware planes inside the 3D world. They do not require skeletal 3D character models.
- UI and portrait/dialogue presentation remain screen-space 2D and must not be forced through the world renderer.

## Directional sprite views

A character is represented by independently authored PNG frames grouped into view directions and animation states.

The sprite renderer selects the closest authored view using the camera azimuth relative to the character's facing direction. For example, an 8-direction set can cover front, back, side, and diagonal views. If the camera pitch changes substantially, a project may provide additional elevation bands such as eye-level, elevated, and overhead views.

The selected image is drawn on a vertical camera-facing or constrained-billboard quad at the character's world position. The ground-contact pivot remains fixed while views change, so the character does not jump or slide when the camera crosses a direction boundary.

Animation and view selection are separate:
- animation chooses idle, walk, run, attack, hurt, and other frame sequences;
- view selection chooses the authored direction/elevation variant;
- the renderer combines both to select one frame.

A missing directional variant must produce a diagnostic and use an explicitly configured fallback view. It must not silently select an unrelated pose.

For the ordinary-world camera, begin with a fixed, slightly elevated third-person angle. Author enough directional views to show the heroine's face, hair, body silhouette, and feet naturally. For more top-down or orbiting scenes, add more view directions/elevation bands rather than creating a 3D character mesh.

## World geometry and image-based asset creation

Rift should support multiple asset paths instead of pretending every picture can become a perfect 3D model:

1. **Textured mesh assets:** import validated glTF 2.0 / GLB models with mesh geometry, UVs, materials, and textures. This is the preferred path for detailed furniture, buildings, and complex props.
2. **Voxel assets:** convert a source image into a deliberately stylized voxel volume using explicit depth rules, then merge adjacent faces into efficient chunk meshes. This is useful for block-like props and stylized environments.
3. **Procedural geometry:** create primitives and custom meshes from vertices/indices for walls, floors, collision proxies, simple props, and generated structures.
4. **Image textures:** apply high-resolution images to model surfaces using UV coordinates. Keep source textures separate from model geometry and preserve mipmaps/filtering choices.

A single front-facing image does not contain the hidden back or side surfaces of an object. Any image-to-3D reconstruction must mark inferred geometry as a generated guess and provide a preview/validation step. It must not be advertised as exact reconstruction.

Minecraft is a useful reference for the principle of building visible geometry and applying texture images, but Rift is not limited to cubes or a one-object-per-block representation. Voxel chunks should be meshed efficiently; conventional props should use imported or procedural meshes.

## 3D world interaction

Every interactive physical object belongs to the world scene with a real transform and a collision representation. Interaction queries operate in 3D:
- camera ray or character interaction ray;
- raycast against registered 3D colliders;
- filtering by interaction layer, visibility, range, and enabled state;
- return the actual world object/component and hit position.

A flat screen-space hotspot may be used for UI, but it must not substitute for a physical world collider.

Render geometry and collision geometry are separate assets. Detailed visual meshes should not automatically become expensive triangle-by-triangle gameplay colliders. Prefer simplified boxes, capsules, spheres, convex shapes, or explicitly authored collision meshes.

## Renderer and module boundaries

Keep the existing 2D renderer API for compatibility and UI. Introduce an independent 3D rendering contract and platform backends. The shared engine core must remain platform-neutral; Android OpenGL ES and the Windows desktop backend implement the same public rendering capabilities.

The 3D scene pipeline needs:
- right-handed world coordinates and documented matrix conventions;
- perspective camera, view/projection matrices, viewport and depth buffer;
- vertex/index buffers, mesh resources, UVs, normals and tangents;
- material and texture resources, alpha modes and mipmaps;
- depth testing, culling, blending, lighting and shadow capability tiers;
- model and world-scene loading with explicit resource ownership;
- 3D colliders, broad-phase spatial queries and raycasts;
- a hybrid draw queue for opaque world meshes, transparent geometry, sprites, and screen-space UI.

Rendering capabilities must be discoverable by the project validator. A project that requires unsupported capabilities should fail validation with a clear diagnostic instead of silently falling back to a 2D scene.

## Performance targets

The mobile renderer must be designed around explicit budgets:
- batch compatible mesh/material draws;
- use chunk meshes for voxel terrain instead of one draw call per voxel;
- cull objects outside the camera frustum;
- use texture atlases where they reduce state changes without sacrificing source quality;
- support mipmaps and appropriate filtering to avoid distant texture shimmer;
- separate render meshes from collision proxies;
- introduce LOD and streaming only when measured scene size requires them.

Quality settings must expose real trade-offs. Do not reduce texture resolution invisibly or claim high-quality lighting when the backend is using an unlit fallback.

## Staged acceptance gates

1. **Math and camera foundation:** tested vectors, matrices, perspective projection, look-at, and camera validation.
2. **3D backend:** Android and Windows implementations render a textured depth-tested mesh with a perspective camera.
3. **Asset pipeline:** load a validated GLB model and high-resolution textures; report malformed geometry and missing resources.
4. **World scenes:** load a real room with floor, walls, ceiling, door, window, and furniture as separate 3D objects.
5. **3D interaction and physics:** collide with room geometry and raycast an actual object to trigger an interaction component.
6. **Directional sprite rendering:** place a transparent animated sprite in the 3D room, select front/back/side views from camera direction, preserve pivot, depth-test and occlude correctly.
7. **Hybrid vertical slice:** run the same world scene with the same game logic on Android and Windows, then profile on a real Android device.

Each gate requires tests, documentation, a runnable example, and diagnostics before the next gate is treated as complete. The 2D renderer remains available throughout the migration.
