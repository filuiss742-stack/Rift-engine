# Rift Engine Cookbook

Small recipes based on APIs that exist in the current branch. For full explanations, follow the linked guides.

## Start a runtime with a scene

```kotlin
val engine = GameEngine()
engine.scenes.setInitialScene(FirstScene())
```

The host owns the game loop and calls `update` and `render`. See [Create a Scene](getting-started/create-a-scene.md).

## Add a basic entity

```kotlin
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
```

Only a Scene subclass can call its protected `add` helper. See [Create a Scene](getting-started/create-a-scene.md).

## Change movement mode

```kotlin
player.movementProfile = TopDownMovementProfile(speed = 180f)
player.movementInput = MovementInput(horizontal = 1f, vertical = 0f)
```

For side-view movement, use `PlatformerMovementProfile`. Ground contact remains externally supplied. See [Movement](getting-started/movement.md).

## Read a key press

```kotlin
override fun onKey(keyCode: Int, pressed: Boolean) {
    if (pressed) pressedKeys.add(keyCode) else pressedKeys.remove(keyCode)
}
```

Track held keys and move using `deltaSeconds`; do not use OS key-repeat frequency as a movement clock. See [Input](getting-started/input.md).

## Save a value

```kotlin
engine.saves.putFloat("player.x", player.x)
val x = engine.saves.getFloat("player.x", 0f)
```

The default `GameEngine()` uses in-memory storage. Persistent storage is supplied by a platform host. See [Saves and Plugins](getting-started/saves-plugins.md).

## Install a plugin

```kotlin
engine.plugins.install(ExamplePlugin())
```

Give every plugin a stable non-empty ID and release plugin-owned resources in `onUninstall`. See [Saves and Plugins](getting-started/saves-plugins.md).

## Inspect diagnostics

```kotlin
val recentEvents = engine.diagnostics.snapshot()
val latestError = engine.diagnostics.latestError()
```

Fix the underlying cause before clearing diagnostic history. See [Rift Diagnostics](diagnostics.md).

## Draw with the shared renderer

```kotlin
override fun render(context: RenderContext) {
    context.clear(0xFF202838.toInt())
    context.drawRect(20f, 20f, 100f, 100f, 0xFFFFAA50.toInt())
    super.render(context)
}
```

The concrete renderer is provided by the platform host. See [the API map](api-map.md).
