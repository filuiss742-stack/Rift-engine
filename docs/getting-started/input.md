# Handle Input

The current input API forwards key and touch events to the active scene. It is intentionally small and is not yet a full action-mapping system.

## Receive key events

A scene can override `onKey`:

```kotlin
import com.rift.engine.input.EngineKey

private val pressedKeys = mutableSetOf<Int>()

override fun onKey(keyCode: Int, pressed: Boolean) {
    if (pressed) {
        pressedKeys.add(keyCode)
    } else {
        pressedKeys.remove(keyCode)
    }
}
```

Use the set during `update(deltaSeconds)` to implement continuous movement. Do not rely on operating-system key-repeat events to determine movement speed.

The current identifiers include `EngineKey.A`, `D`, `W`, `S`, and arrow keys.

## Receive touch/click

Override `onTouch`:

```kotlin
override fun onTouch(x: Float, y: Float, width: Float, height: Float) {
    player.x = x - player.width / 2f
    player.y = y - player.height / 2f
}
```

The platform adapter translates its device event and forwards it through `GameEngine` to the active scene.

## Current limitations

- No named actions or configurable key bindings yet.
- No shared virtual-button UI or completed gamepad mapping API.
- Device-specific event translation belongs in platform adapters, not the shared game scene.

See [the API map](../api-map.md) and [movement guide](movement.md).
