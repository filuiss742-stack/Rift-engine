# Create Your First Scene

This guide uses only the current shared API. It creates a scene with one movable demo object; it does not require editing the engine core.

## 1. Put game code in a game module

For the repository demo, shared gameplay belongs in `:demo-game`. A real game should use its own game module that depends on `:engine`, rather than placing story or game-specific behavior inside the engine.

## 2. Create a scene

Create a Kotlin class in your game module:

```kotlin
package com.example.game

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.scene.Scene

class FirstScene : Scene() {
    private val player = GameObject(
        x = 80f,
        y = 120f,
        width = 48f,
        height = 48f,
        color = 0xFF66AAFF.toInt()
    )

    init {
        add(player)
    }

    override fun update(deltaSeconds: Float) {
        super.update(deltaSeconds)
        // Put this scene's game-specific logic here.
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF202838.toInt())
        super.render(context)
    }
}
```

The `add` helper is protected by `Scene`; call it from the scene class (for example, in `init`). The base update/render methods update and draw the objects owned by the scene.

## 3. Activate the scene

The shared runtime exposes a `SceneManager` through `GameEngine.scenes`:

```kotlin
val engine = GameEngine()
engine.scenes.setInitialScene(FirstScene())
```

A platform host is responsible for calling `engine.update(deltaSeconds)` and `engine.render(renderContext)`. Do not create a second game loop inside the scene.

To replace the active scene later:

```kotlin
engine.scenes.changeScene(AnotherScene())
```

## 4. Add or remove objects

Use the scene's protected `add(gameObject)` and `remove(gameObject)` helpers from inside your scene. Requests made during object update or render are deferred until traversal finishes.

## 5. Important limitations

- This is a basic scene/object API, not a complete editor or scene serialization system.
- `GameObject` currently draws a rounded rectangle; custom visuals can override `render` and use the supplied `RenderContext`.
- There is no built-in collision world or automatic floor detection yet.
- Keep scene content in your game module. Only reusable, game-independent behavior belongs in `:engine`.

See [the API map](../api-map.md), [movement guide](movement.md), and [input guide](input.md).


## Scene lifecycle and disposal

A scene can implement `onEnter()`, `onExit()`, and `onDispose()`. Transitions call `onExit()` on the outgoing scene and `onEnter()` on the destination. The `SceneManager` retains ownership of scenes it has activated and calls `onDispose()` when the manager closes.

`GameEngine.close()` closes the SceneManager before releasing shared audio and asset resources. Scene disposal clears UI input, physics bodies, combat registrations, and attached GameObject components. Disposal is idempotent; component `onDetach()` and scene `onDispose()` callbacks are not called repeatedly.

Do not close shared AssetManager or AudioManager instances from a scene's `onDispose()`; they belong to the GameEngine and are released after all scenes have been disposed.
