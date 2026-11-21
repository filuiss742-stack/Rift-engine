# Handle Input

Rift routes keyboard, pointer, touch, and analog-axis events through the active scene's `InputActionMap`. Bind device-independent named actions so game logic does not depend on physical key names.

## Bind keyboard and pointer actions

```kotlin
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons

init {
    input.bindKey("move.left", EngineKey.A)
    input.bindKey("move.left", EngineKey.LEFT)
    input.bindKey("move.right", EngineKey.D)
    input.bindKey("move.right", EngineKey.RIGHT)
    input.bindKey("jump", EngineKey.SPACE)
    input.bindPointerButton("attack", PointerButtons.PRIMARY)
}
```

Available queries:
- `isDown(action)`: at least one bound key, pointer button, or analog axis is active.
- `wasPressed(action)`: an action crossed from inactive to active during this frame.
- `wasReleased(action)`: an action crossed from active to inactive during this frame.
- `axisValue(action)`: combined normalized analog value in the range -1..1.

One-frame edges are cleared after the scene update. Held state persists. If a key remains held during a scene transition, the new scene receives its held state without a synthetic just-pressed event.

## Analog axes and gamepads

Bind analog axes to actions:

```kotlin
input.bindAxis(
    actionId = "move.horizontal",
    axisId = "gamepad.left.x",
    deadZone = 0.18f
)
input.bindAxis(
    actionId = "move.vertical",
    axisId = "gamepad.left.y",
    deadZone = 0.18f
)

override fun update(deltaSeconds: Float) {
    player.movementInput = MovementInput(
        horizontal = input.axisValue("move.horizontal"),
        vertical = input.axisValue("move.vertical")
    )
    super.update(deltaSeconds)
}
```

The Android host forwards common joystick axes under these IDs:
- `gamepad.left.x`, `gamepad.left.y`
- `gamepad.right.x`, `gamepad.right.y`
- `gamepad.left.trigger`, `gamepad.right.trigger`
- `gamepad.dpad.x`, `gamepad.dpad.y`

Android gamepad buttons map common A/B/X/Y, shoulder, Start, and Select controls to the device-independent EngineKey set. The Windows host does not currently include native XInput/DirectInput enumeration; an external Windows controller adapter can feed `GameEngine.onAxis(axisId, value)` and `GameEngine.onKey(code, pressed)`. Steam Input keyboard emulation also works through the normal key bindings.

Axis values are clamped to -1..1. A configurable dead zone is removed and the remaining range is normalized. Multiple bindings for one action are summed and clamped.

## Virtual touch controls

Use `VirtualJoystick` and `VirtualButton` from the UI package for Android touch controls. See the [virtual controls guide](virtual-controls.md) for complete examples.

## Pointer events

`PointerEvent` reports pointer ID, event type (DOWN/MOVE/UP/CANCEL), coordinates, and viewport size. Android forwards simultaneous touch pointers; desktop forwards mouse press, release, drag, and movement.

```kotlin
override fun onPointer(event: PointerEvent) {
    if (event.type == PointerEventType.DOWN && input.wasPressed("attack")) {
        val worldX = camera.screenToWorldX(event.x)
        val worldY = camera.screenToWorldY(event.y)
        // Aim or interact in world coordinates.
    }
}
```

`input.pointerX/Y` and `pointerDeltaX/Y` expose the most recent pointer position and movement. Focus loss clears held keys, pointer buttons, and analog axes so controls cannot remain stuck.

## Coordinate system

Pointer coordinates are transformed by the same FIT logical viewport used for rendering. Use the game viewport coordinate system for UI. Use `Camera.screenToWorldX/Y` to convert screen-space coordinates to world coordinates.

See [the API map](../api-map.md), [movement guide](movement.md), and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
