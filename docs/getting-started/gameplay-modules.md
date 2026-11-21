# Gameplay Modules

Gameplay modules are optional, replaceable packs of runtime behavior. A game can combine modules such as top-down movement, platforming, real-time combat, racing, or mech controls without selecting one genre template for the whole project.

The current API is a runtime registry. Module code must already be included in the application's build; dynamic JAR downloading/loading and a public module hub are not implemented.

## Define a module

Implement `GameplayModule` and give it a stable descriptor:

```kotlin
class TopDownPack : GameplayModule {
    override val descriptor = GameplayModuleDescriptor(
        id = "rift.top-down",
        version = "1.0.0",
        dependencies = setOf("rift.input-actions"),
        capabilities = setOf("movement.top-down", "camera.top-down")
    )

    override fun onInstall(context: GameplayModuleContext) {
        // Register systems and resources owned by this pack.
    }

    override fun onUpdate(deltaSeconds: Float) {
        // Update this pack's systems.
    }

    override fun onUninstall() {
        // Unregister callbacks and release owned resources.
    }
}
```

## Install and inspect

```kotlin
val engine = GameEngine()
engine.gameplayModules.install(InputActionsPack())
engine.gameplayModules.install(TopDownPack())

val supportsTopDown =
    engine.gameplayModules.hasCapability("movement.top-down")
```

Installation fails with a diagnostic if the ID is invalid or already installed, a dependency is missing, or a declared conflict is present. The manager never silently replaces modules.

## Replace an implementation

A replacement must retain the same stable module ID:

```kotlin
engine.gameplayModules.replace(
    "rift.top-down",
    CustomTopDownPack()
)
```

The replacement is validated before the current implementation is touched. The old module is uninstalled before the new one is activated. If activation fails, the manager attempts to reactivate the previous implementation and reports a diagnostic if restoration also fails.

## Uninstall safely

```kotlin
engine.gameplayModules.uninstall("rift.top-down")
```

A module cannot be removed while another installed module declares it as a dependency. Remove dependents first.

## Descriptor fields

| Field | Meaning |
|---|---|
| `id` | Stable unique module identity. |
| `version` | Informational module version string. |
| `dependencies` | Module IDs that must already be installed. |
| `conflicts` | Module IDs that cannot coexist; checked in both directions. |
| `capabilities` | Feature identifiers exposed for capability checks. |

## Lifecycle and limitations

- `onInstall` runs after validation and before the module is registered.
- `onUpdate` is called by `GameEngine.update`; a failing module is reported without blocking unrelated modules.
- `onUninstall` should release resources and unregister callbacks owned by the module.
- If `onUninstall` throws, the manager keeps the module registered and reports the failure.
- Lifecycle callbacks should be short and must not block the game loop.
- The manager validates declared metadata; it cannot prove arbitrary module code is behaviorally compatible.
- There is no dynamic code downloading, binary loading, sandboxing, semantic version range resolution, editor integration, or graphical module hub yet.
- A module is not automatically a complete genre implementation. It is a pack of systems that game code can compose with other packs and custom behavior.

## Diagnostics

- `MODULE_DEPENDENCY_MISSING`: install required module IDs first.
- `MODULE_CONFLICT`: remove a conflicting module or revise the descriptor.
- `MODULE_ID_ALREADY_INSTALLED`: use `replace` explicitly to change an implementation.
- `MODULE_REQUIRED_BY_INSTALLED`: uninstall dependent modules before removing this module.
- `MODULE_RESTORE_FAILED`: both replacement activation and restoration failed; inspect lifecycle diagnostics.

## Change notes

This first registry adds dependency/conflict validation, explicit replacement, safe dependent checks, capability queries, lifecycle updates, and focused tests. External module distribution, semantic version compatibility, and a visual module hub remain future work.
