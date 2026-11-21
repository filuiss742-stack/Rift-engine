# Current API Map

This page lists APIs verified in the current development branch. It is a navigation aid, not a promise that every class is a finalized stable API.

## Runtime and scenes

| API | Package | Purpose |
|---|---|---|
| `GameEngine` | `com.rift.engine.core` | Shared runtime entry point; exposes `scenes`, `plugins`, `saves`, and `diagnostics`; accepts input events and update/render calls. |
| `SceneManager` | `com.rift.engine.core` | Sets the first scene and switches the active scene. |
| `Scene` | `com.rift.engine.scene` | Base class for game scenes; subclasses implement lifecycle, update, render, and input callbacks. |
| `FixedStepAccumulator` | `com.rift.engine.core` | Runs update callbacks using a fixed time step. |

## Entities and movement

| API | Package | Purpose |
|---|---|---|
| `GameObject` | `com.rift.engine.entity` | Basic entity with position, size, color, active flag, movement body, and optional movement profile. |
| `MovementBody` | `com.rift.engine.movement` | Position, velocity, and externally supplied grounded state. |
| `MovementInput` | `com.rift.engine.movement` | Device-independent horizontal/vertical intent and one-update jump press. |
| `MovementProfile` | `com.rift.engine.movement` | Contract for converting movement intent into body velocity. |
| `TopDownMovementProfile` | `com.rift.engine.movement` | Normalized free 2D movement. |
| `PlatformerMovementProfile` | `com.rift.engine.movement` | Basic horizontal movement, gravity, and jump velocity; does not perform collision detection. |
| `Aabb` | `com.rift.engine.physics` | Axis-aligned bounds value used by `GameObject.bounds()`. |

## Rendering and input

| API | Package | Purpose |
|---|---|---|
| `RenderContext` | `com.rift.engine.graphics` | Platform-neutral clear/draw/text/measurement contract. |
| `Camera` | `com.rift.engine.graphics` | Basic world/screen coordinate offset conversions; not yet integrated into scene rendering. |
| `EngineKey` | `com.rift.engine.input` | Small set of key identifiers used by the current demo; not an action-mapping system. |

## Storage, plugins, diagnostics

| API | Package | Purpose |
|---|---|---|
| `SaveStore` | `com.rift.engine.storage` | Platform-neutral string/float key-value save contract. |
| `InMemorySaveStore` | `com.rift.engine.storage` | In-memory implementation, useful for tests and examples. |
| `EnginePlugin` | `com.rift.engine.plugin` | Optional plugin lifecycle/update contract. |
| `PluginManager` | `com.rift.engine.plugin` | Installs, updates, lists, and uninstalls plugins by ID. |
| `DiagnosticReporter` | `com.rift.engine.diagnostics` | Bounded thread-safe diagnostic history and severity queries. |
| `DiagnosticEntry` / `DiagnosticSeverity` | `com.rift.engine.diagnostics` | Diagnostic data and INFO/WARNING/ERROR/FATAL levels. |

## Ownership and lifecycle notes

- A `GameEngine` owns its `SceneManager` and `PluginManager`; it receives a `SaveStore` and `DiagnosticReporter` through its constructor.
- `SceneManager.setInitialScene(scene)` sets the first scene once. Use `changeScene(scene)` to replace it.
- A `Scene` owns objects added through its protected `add`/ `remove` helpers. Mutations requested during update/render traversal are applied after traversal.
- A `GameObject` updates its movement profile and then integrates position. Avoid also applying the same movement manually in the same update.
- Platform hosts own the actual render loop and concrete `RenderContext` / `SaveStore` implementations.
- The current desktop host uses Swing scheduling; do not assume simulation already runs on a dedicated game thread.

## Not present yet

Do not invent or use these as existing APIs: action-based input maps, collision world/ground detection, camera-follow integration, render layers, animation/assets manager, combat framework, mode-transition framework, or scene serialization. They are future work, not part of this API map.
