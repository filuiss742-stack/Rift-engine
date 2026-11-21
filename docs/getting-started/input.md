# Handle Input

Rift routes physical key events from the platform host to the active scene. A scene can bind those keys to named gameplay actions, so game logic does not need to depend on specific physical keys.

## Bind named actions

Bind one or more keys to an action, usually when the scene is created:

```kotlin
import com.rift.engine.input.EngineKey

init {
    input.bindKey("move.left", EngineKey.A)
    input.bindKey("move.left", EngineKey.LEFT)
    input.bindKey("move.right", EngineKey.D)
    input.bindKey("move.right", EngineKey.RIGHT)
    input.bindKey("jump", EngineKey.SPACE)
}
```

Use the action state during update:

```kotlin
override fun update(deltaSeconds: Float) {
    val horizontal = when {
        input.isDown("move.left") -> -1f
        input.isDown("move.right") -> 1f
        else -> 0f
    }

    if (input.wasPressed("jump")) {
        // Start one jump; do not trigger this every frame.
    }
}
```

Available queries:
- isDown(action): at least one bound keyboard key or pointer button is held.
- wasPressed(action): a bound key began the action during this update frame.
- wasReleased(action): the last held key for the action was released during this update frame.

The runtime clears one-frame pressed/released edges after the scene update. Held state persists. If a key remains held during a scene transition, the new scene receives the held-key state after its onEnter callback.

## Raw key events

For low-level cases, a scene can still override onKey:

```kotlin
override fun onKey(keyCode: Int, pressed: Boolean) {
    // Handle a device-independent EngineKey directly.
}
```

The current EngineKey set includes WASD, arrow keys, Space, Enter, Escape, Tab, E, Q, F, R, Shift, Ctrl, and Alt. Android hardware keyboards and Windows keyboard input are forwarded by their platform adapters.

## Pointer and touch input

Mouse buttons and touch contacts can also drive named actions:

```kotlin
import com.rift.engine.input.PointerButtons

init {
    input.bindPointerButton("attack", PointerButtons.PRIMARY) // desktop mouse
    input.bindPointerButton("touch", PointerButtons.TOUCH)    // Android touch
}

override fun onPointer(event: PointerEvent) {
    if (event.type == PointerEventType.DOWN && input.wasPressed("attack")) {
        val worldX = camera.screenToWorldX(event.x)
        val worldY = camera.screenToWorldY(event.y)
        // Aim or interact in world coordinates.
    }
}
```

`PointerEvent` reports pointer ID, event type (DOWN/MOVE/UP/CANCEL), coordinates, and viewport size. Android forwards simultaneous touch pointers; desktop forwards mouse press, release, drag, and movement. `input.pointerX/Y` and `pointerDeltaX/Y` expose the most recent pointer position and movement. Focus loss clears held input so keys or buttons cannot remain stuck.

The legacy `onTouch(x, y, width, height)` callback remains available and receives non-cancel pointer events. New projects should prefer `onPointer` when they need press/release semantics.

## Current limitations

- Named action mapping supports keyboard and pointer buttons; gamepad bindings, saved remapping, and input-profile UI are not implemented.
- Virtual joystick and virtual button widgets are not implemented yet.
- Pointer coordinates are view/screen coordinates. Use Camera.screenToWorldX/Y for the current top-left anchored camera; automatic viewport scaling and rotated-camera conversion are not implemented.

See [the API map](../api-map.md), [movement guide](movement.md), and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
