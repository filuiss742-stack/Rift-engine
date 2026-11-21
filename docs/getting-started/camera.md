# 2D Camera

Each Scene owns a Camera. Its x/y values describe the world-space top-left of the visible area; zoom is uniform and must be greater than zero.

```kotlin
camera.x = player.x - 240f
camera.y = player.y - 135f
camera.zoom = 1.25f
```

The base Scene.render implementation applies the camera only while rendering scene-owned GameObjects. A subclass that calls super.render(context) can draw a HUD or other screen-space overlay afterward without the world camera affecting it.

The camera also provides worldToScreenX/Y and screenToWorldX/Y for coordinate conversion. The current model assumes a top-left anchored viewport and does not yet include rotation, viewport letterboxing, safe-area handling, or automatic pointer conversion.

For custom drawing, Camera.withTransform(context) applies the camera inside a push/pop renderer state and restores that state even if the draw callback fails.

## Current limitations

- No automatic follow target or smoothing.
- No camera bounds or dead-zone configuration.
- No split-screen or multiple viewport manager.
- Input pointer coordinates are not automatically converted to world coordinates.

See the [2D transform guide](transforms.md) and [1.0.0 release gates](../roadmap/1.0.0-release-gates.md).
