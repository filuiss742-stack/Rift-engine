# Current API Map

This page lists APIs verified in the current development branch. It is a navigation aid, not a promise that every class is a finalized stable API.

## Runtime and scenes

| API | Package | Purpose |
|---|---|---|
| GameEngine | com.rift.engine.core | Shared runtime entry point; exposes scenes, plugins, gameplay modules, saves, and diagnostics. |
| SceneManager | com.rift.engine.core | Sets the first scene, queues safe transitions, and owns shared GameState. |
| Scene | com.rift.engine.scene | Base class for game scenes; subclasses implement lifecycle, update, render, and input callbacks. |
| FixedStepAccumulator | com.rift.engine.core | Runs update callbacks using a fixed time step. |

## Entities and movement

| API | Package | Purpose |
|---|---|---|
| GameState | com.rift.engine.state | Typed runtime state shared across all scenes managed by one SceneManager; not automatically persisted. |\n| GameObject | com.rift.engine.entity | Basic entity with position, size, color, active flag, movement body, rotation/scale transform, optional movement profile, and lifecycle-managed components. |
| Transform2D | com.rift.engine.entity | Local rotation and scale; transforms local points and computes rotated axis-aligned bounds. |
| GameComponent | com.rift.engine.entity | Optional behavior attached to a GameObject with attach, detach, update, and render callbacks. |
| MovementBody | com.rift.engine.movement | Position, velocity, and externally supplied grounded state. |
| MovementInput | com.rift.engine.movement | Device-independent horizontal/vertical intent and one-update jump press. |
| MovementProfile | com.rift.engine.movement | Contract for converting movement intent into body velocity. |
| TopDownMovementProfile | com.rift.engine.movement | Normalized free 2D movement. |
| PlatformerMovementProfile | com.rift.engine.movement | Basic horizontal movement, gravity, and jump velocity; ground state can now be supplied by a registered PhysicsWorld body. |
| Aabb | com.rift.engine.physics | Validated axis-aligned bounds used by entities, collision tests, and spatial queries. |\n| PhysicsWorld | com.rift.engine.physics | Scene-owned 2D AABB physics with dynamic/static/kinematic bodies, gravity, basic positional collision response, sensors, filters, overlap queries, and raycasts. |\n| PhysicsBody / PhysicsBodyType | com.rift.engine.physics | Body configuration and STATIC/KINEMATIC/DYNAMIC behavior. |\n| CollisionFilter | com.rift.engine.physics | Category/mask and group filtering for collision pairs and queries. |\n| CollisionContact | com.rift.engine.physics | Contact pair, normal, penetration, and sensor flag from the latest physics step. |\n| RaycastHit | com.rift.engine.physics | Nearest AABB ray hit with point, normal, and normalized fraction. |

## Gameplay modules

| API | Package | Purpose |
|---|---|---|
| GameplayModule | com.rift.engine.module | Lifecycle contract for an optional, replaceable gameplay pack. |
| GameplayModuleDescriptor | com.rift.engine.module | Stable ID, version, dependencies, conflicts, and provided capabilities. |
| GameplayModuleManager | com.rift.engine.module | Validates installation, replacement, removal, dependencies, conflicts, and capability queries. |
| GameplayModuleContext | com.rift.engine.module | Services exposed to module lifecycle callbacks. |

The registry is a runtime extension point, not a dynamic binary loader. Modules must be compiled into the application. Version strings are informational; semantic version resolution and external distribution are not implemented.

## Narrative and branching dialogue

| API | Package | Purpose |
|---|---|---|
| DialogueGraph | com.rift.engine.narrative | Validated collection of dialogue nodes and branch targets. |
| DialogueNode / DialogueChoice | com.rift.engine.narrative | Dialogue line data and player-selectable branches. |
| DialogueSession | com.rift.engine.narrative | Runs a conversation, advances lines, and applies selected choice effects. |
| StoryState | com.rift.engine.narrative | Shared story flags and integer values for quests, relationships, and route conditions. |
| StoryCondition | com.rift.engine.narrative | Checks flags or numeric story values before showing a choice. |
| StoryEffect | com.rift.engine.narrative | Changes flags or numeric story values after a choice is selected. |

## Rendering and input

| API | Package | Purpose |
|---|---|---|
| RenderContext | com.rift.engine.graphics | Platform-neutral primitive/text drawing plus scoped state, transforms, and rectangular clipping. |
| Camera | com.rift.engine.graphics | Top-left anchored zoom camera applied automatically to Scene world-object rendering; exposes world/screen coordinate conversions. |
| EngineKey | com.rift.engine.input | Device-independent identifiers for common keyboard keys. |
| InputActionMap | com.rift.engine.input | Maps named actions to one or more keys and exposes held/pressed/released state. Currently keyboard-only. |

## Storage, plugins, diagnostics

| API | Package | Purpose |
|---|---|---|
| SaveStore | com.rift.engine.storage | Platform-neutral string/float key-value save contract. |
| InMemorySaveStore | com.rift.engine.storage | In-memory implementation, useful for tests and examples. |
| EnginePlugin | com.rift.engine.plugin | Optional plugin lifecycle/update contract. |
| PluginManager | com.rift.engine.plugin | Installs, updates, lists, and uninstalls plugins by ID. |
| DiagnosticReporter | com.rift.engine.diagnostics | Bounded thread-safe diagnostic history and severity queries. |
| DiagnosticEntry / DiagnosticSeverity | com.rift.engine.diagnostics | Diagnostic data and INFO/WARNING/ERROR/FATAL levels. |

## Narrative system boundaries

The dialogue runtime is headless: it stores dialogue data, current line, available choices, and story-state effects. It does not draw dialogue boxes, portraits, backgrounds, typewriter text, audio, or transitions. A game implements that presentation using its UI/rendering layer.

StoryState currently contains boolean-like flags and integer values. Save/load serialization for StoryState is not built in; games can map its snapshots to SaveStore keys or add a dedicated serializer later. The dialogue graph is currently authored in Kotlin code; there is no visual editor or JSON importer yet.

## Ownership and lifecycle notes

- GameEngine owns its SceneManager, PluginManager, and GameplayModuleManager; it receives SaveStore and DiagnosticReporter through its constructor.
- SceneManager.setInitialScene(scene) sets the first scene. changeScene(scene) queues a transition that is applied at a safe runtime boundary after the current update/render/input/lifecycle callback returns.
- A Scene owns objects added through protected add/remove helpers. Mutations during traversal are deferred.
- A GameObject owns components added through addComponent/removeComponent. Component changes during update/render are deferred; component identity, not equality, defines attachment. Failed onAttach callbacks trigger rollback and a best-effort onDetach cleanup.
- GameObject updates its movement profile and integrates position unless registered with a PhysicsWorld. A Scene steps its PhysicsWorld after entity updates.
- Platform hosts own the game loop and concrete RenderContext / SaveStore implementations.
- The current desktop host uses Swing scheduling; do not assume simulation already runs on a dedicated game thread.

## Not present yet

Do not invent or use these as existing APIs: gamepad action bindings, virtual touch controls, collision callbacks, continuous collision detection, arbitrary polygon/circle shapes, camera-follow smoothing/bounds, render layers, sprite/texture assets, animation, audio, UI widgets, combat framework, mode-transition framework, scene serialization, visual dialogue editor, or a complete visual-novel presentation layer. A hybrid 2D/3D renderer is a long-term direction, not an implemented capability.
