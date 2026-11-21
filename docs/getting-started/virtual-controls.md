# Virtual Touch Controls

Rift provides screen-space controls that work through the same pointer event pipeline as mouse input. They support multiple simultaneous pointers.

## Analog joystick

```kotlin
private val joystick = VirtualJoystick(
    x = 24f,
    y = 420f,
    size = 128f
)

init {
    ui.add(joystick)
}

override fun update(deltaSeconds: Float) {
    player.movementInput = MovementInput(
        horizontal = joystick.axisX,
        vertical = joystick.axisY
    )
    super.update(deltaSeconds)
}
```

The joystick exposes normalized `axisX` and `axisY` values in the range -1..1, applies a configurable dead zone, clamps the pointer to its circular base, and releases its axes on pointer-up, cancellation, or removal. One pointer owns the joystick at a time.

## Press-and-hold button

```kotlin
private var jumpPressedThisFrame = false

private val jumpButton = VirtualButton(
    x = 800f,
    y = 440f,
    width = 120f,
    height = 100f,
    label = "JUMP",
    onPressed = { jumpPressedThisFrame = true }
)

init {
    ui.add(jumpButton)
}

override fun update(deltaSeconds: Float) {
    player.movementInput = MovementInput(
        horizontal = joystick.axisX,
        jumpPressed = input.wasPressed("jump") || jumpPressedThisFrame
    )
    jumpPressedThisFrame = false
    super.update(deltaSeconds)
}
```

The button calls `onPressed` when the first pointer presses it and `onReleased` when the last pointer leaves or is cancelled. Multiple fingers do not cause repeated press/release flicker.

## Coordinate system

UI controls use the game's logical viewport coordinates, not raw device pixels. The Android and Windows hosts map physical pointer coordinates through the same FIT viewport transform used for rendering. Letterbox regions map outside the logical viewport and should not be used for control placement.

See [UI](ui.md), [input](input.md), and the [API map](../api-map.md).
