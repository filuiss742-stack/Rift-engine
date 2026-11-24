# 2D Object Transforms

Every GameObject has a Transform2D. Its position remains the object's world x/y; Transform2D supplies local rotation and scale.

```kotlin
player.x = 240f
player.y = 180f
player.rotationDegrees = 30f
player.scaleX = 1.25f
player.scaleY = 1.25f

// Flip horizontally without changing the logical position.
player.scaleX = -1f
```

Rotation is expressed in degrees. Scale values must be finite; negative values are allowed for flipping. The current transform origin is the object's top-left local origin. A dedicated configurable pivot is not implemented yet.

## Rendering

GameObject rendering pushes renderer state, applies translation, rotation, and scale, draws its local rectangle, then renders attached components in the same local coordinate space. The state is restored even if a component render callback fails.

RenderContext also exposes pushState/popState, translate, rotate, scale, and clipRect for custom drawing. Every pushState must be paired with popState, preferably in a try/finally block.

## Bounds

GameObject.bounds() returns an axis-aligned world-space bounding box of the transformed rectangle. Rotated objects therefore return a larger enclosing AABB when appropriate. This is a broad-phase-friendly bound, not an exact rotated polygon collision test.

## Current limitations

- No configurable transform pivot or parent-child transform hierarchy.
- No sprite/image drawing or animation system yet.
- No camera-follow or viewport-to-world input conversion yet.
- Rotated AABBs are not a replacement for a future shape-aware physics system.

See the [API map](../api-map.md) and [1.0.0 release gates](../roadmap/1.0.0-release-gates.md).
