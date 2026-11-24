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

## CameraRig

CameraRig is an optional helper for common follow-camera behavior. It is not forced on every Scene; games can keep direct camera control or use their own camera logic.

```kotlin
val cameraRig = CameraRig(
    camera = camera,
    viewportWidth = 480f,
    viewportHeight = 270f,
    smoothing = 8f,
    deadZoneWidth = 48f,
    deadZoneHeight = 32f,
    worldBounds = Aabb(0f, 0f, 2400f, 1350f)
)

override fun update(deltaSeconds: Float) {
    super.update(deltaSeconds) // updates objects and steps scene physics
    cameraRig.update(player, deltaSeconds)
}
```

Call the rig after the scene update/physics step so it follows the resolved player position. Viewport dimensions are logical screen pixels; the visible world area is divided by camera zoom. smoothing = 0 follows immediately. snapTo(player) is useful for scene entry or teleportation. shake(amplitude, durationSeconds) adds a fading shake; shake may briefly exceed world bounds.

The rig assumes it owns camera position while active. If code changes the camera position externally between rig updates, call snapTo or otherwise coordinate that ownership.

## Current limitations

- No split-screen or multiple viewport manager.
- Input pointer coordinates are not automatically converted to world coordinates.
- Camera rotation and automatic viewport/safe-area integration are not implemented.

See the [2D transform guide](transforms.md) and [1.0.0 release gates](../roadmap/1.0.0-release-gates.md).
