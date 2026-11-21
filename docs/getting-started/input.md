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
- isDown(action): at least one bound key is held.
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

## Touch input

Override onTouch for pointer/tap behavior:

```kotlin
override fun onTouch(x: Float, y: Float, width: Float, height: Float) {
    player.x = x - player.width / 2f
    player.y = y - player.height / 2f
}
```

The platform adapter translates the device event and forwards it through GameEngine to the active scene.

## Current limitations

- Named action mapping currently supports key bindings; pointer buttons, gamepad bindings, persistence/remapping UI, and input profiles are not implemented.
- Touch events are delivered as scene pointer callbacks; virtual joystick and virtual button widgets are not implemented.
- Pointer coordinates are currently screen/view coordinates; camera/viewport conversion is a later rendering milestone.

See [the API map](../api-map.md), [movement guide](movement.md), and [the 1.0 release gates](../roadmap/1.0.0-release-gates.md).
