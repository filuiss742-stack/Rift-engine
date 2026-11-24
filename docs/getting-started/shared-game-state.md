# Shared Game State

SceneManager owns one GameState instance and attaches it to every scene it activates. This lets different gameplay modes share progression without storing it in global singletons.

```kotlin
// In a town scene:
gameState.putInt("coins", gameState.int("coins") + 3)
gameState.putBoolean("quest.bridge_repaired", true)

// In a side-view challenge scene:
val coins = gameState.int("coins")
val bridgeIsRepaired = gameState.boolean("quest.bridge_repaired")
```

Supported values are Boolean, Int, Long, finite Float, finite Double, and String. Keys must not be blank. Getters accept defaults when a key is missing or stores another type. snapshot() returns a copy of the current map.

The state is runtime-only in this milestone. It is not automatically serialized, and it is not a substitute for a save-game schema. Persist selected values explicitly through SaveStore or a game-owned serializer.

A Scene constructed and updated outside SceneManager starts with its own GameState. Once activated by a manager, its gameState reference is replaced with that manager's shared state.

See the [API map](../api-map.md) and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
