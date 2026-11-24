# 3D Collision Queries — Initial Public Contract

Status: implemented baseline; linear-scan broad phase only.

## Purpose

Provide platform-neutral world-space ray queries for interaction, targeting, inspection tools, and gameplay. This is a query layer, not a full rigid-body physics solver.

## Types

- `Ray3`: world-space origin and normalized direction.
- `AabbCollider3D`: axis-aligned box proxy.
- `SphereCollider3D`: sphere proxy.
- `RaycastHit3D`: collider ID, world distance, hit point, and outward normal.
- `CollisionWorld3D`: owns colliders and exposes nearest/all-hit queries.

## Query rules

- Collider IDs are unique within a world.
- Disabled colliders are ignored.
- A query includes a collider only when its layer mask intersects the query mask.
- `maxDistance` is measured in world units because `Ray3` normalizes direction.
- Results from `raycastAll` are sorted nearest-first.
- A ray starting inside a box or sphere returns the exit surface.
- Visual mesh geometry is not automatically used as collision geometry; game code supplies simple proxies intentionally.

## Current limitations

- Broad phase is O(number of colliders); spatial acceleration is a later optimization.
- Only AABB and sphere colliders are implemented.
- No continuous collision detection, rigid-body response, contacts, or overlap solver.
- OBB, capsule, convex mesh, and triangle mesh queries are future additions.
- Query calls are synchronous and expected to run on the owning game thread unless a project defines stronger synchronization.

## Acceptance tests

The engine test suite covers AABB/sphere hit positions, normals, rays starting inside, max distance, layer masks, disabled colliders, sorted hits, and removal.
