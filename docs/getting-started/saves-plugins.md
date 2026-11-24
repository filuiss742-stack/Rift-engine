# Saves and Plugins

## Save data

The shared API is a small key/value contract:

```kotlin
engine.saves.putString("player.name", "Rift")
engine.saves.putFloat("player.x", player.x)

val name = engine.saves.getString("player.name", "Player")
val savedX = engine.saves.getFloat("player.x", 0f)
```

`GameEngine()` defaults to `InMemorySaveStore`, which does not persist data after the process ends. Platform application hosts supply their own persistent `SaveStore` implementation. Use the implementation supplied by the host when persistent saves are required.

The platform SaveStore contract stores strings and floats. Rift also provides a versioned codec for the primitive values in the shared GameState:

```kotlin
engine.scenes.gameState.putInt("coins", 120)
engine.scenes.gameState.putBoolean("quest.bridge.open", true)

engine.saveGameState("slot-1")
engine.loadGameState("slot-1")
```

`GameStateCodec` encodes booleans, integers, longs, finite floats/doubles, and strings. The decoder enforces a format version, size and entry limits, and rejects malformed data without partially replacing the current state. `saveGameState` and `loadGameState` use the host-provided SaveStore. Call them at a safe game-loop boundary.

This is not scene/entity serialization, automatic checkpointing, save migration tooling, or cloud synchronization. The default `InMemorySaveStore` still loses data after the process ends.

## Create a basic plugin

```kotlin
import com.rift.engine.plugin.EnginePlugin

class ExamplePlugin : EnginePlugin {
    override val id: String = "example.plugin"

    override fun onInstall() {
        // Allocate plugin-owned resources here.
    }

    override fun onUpdate(deltaSeconds: Float) {
        // Run small plugin updates here.
    }

    override fun onUninstall() {
        // Release resources owned by this plugin.
    }
}
```

Install it through the runtime:

```kotlin
engine.plugins.install(ExamplePlugin())
engine.plugins.uninstall("example.plugin")
```

IDs must be non-empty and unique. A duplicate ID is ignored and reported. If `onInstall` fails, the plugin is removed and the exception is reported. If `onUpdate` or `onUninstall` fails, the failure is reported through Rift Diagnostics.

The current plugin contract has install, update, and uninstall callbacks. It does not yet provide dependency declarations, configuration schemas, event buses, or hot-reload support. A production plugin should document its purpose, lifecycle, setup, compatibility, API, limitations, and troubleshooting in `docs/plugins/<plugin-id>.md`.

See [the API map](../api-map.md) and [diagnostics guide](../diagnostics.md).
