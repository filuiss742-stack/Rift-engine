# Current API Map

This page lists APIs verified in the current development branch. It is a navigation aid, not a promise that every class is a finalized stable API.

## Runtime and scenes

| API | Package | Purpose |
|---|---|---|
| GameEngine | com.rift.engine.core | Shared runtime entry point; exposes scenes, plugins, gameplay modules, saves, assets, audio, and diagnostics. |
| SceneManager | com.rift.engine.core | Sets the first scene, queues safe transitions, and owns shared GameState, AssetManager, and AudioManager references. |
| Scene | com.rift.engine.scene | Base class for game scenes; owns objects, camera, physics and combat worlds, input map, UI, shared state, assets, and audio; supports enter/exit/dispose lifecycle. |
| FixedStepAccumulator | com.rift.engine.core | Runs update callbacks using a fixed time step. |

## Entities, state, and movement

| API | Package | Purpose |
|---|---|---|
| GameState | com.rift.engine.state | Typed runtime state shared across all scenes managed by one SceneManager. |\n| GameStateCodec | com.rift.engine.state | Bounded versioned codec for primitive state snapshots. |
| GameObject | com.rift.engine.entity | Entity with position, size, color, active flag, transform, render layer, optional movement profile, and lifecycle-managed components. |
| Transform2D | com.rift.engine.entity | Local rotation and scale; transforms local points and computes rotated axis-aligned bounds. |
| GameComponent | com.rift.engine.entity | Optional behavior attached to a GameObject with attach, detach, update, and render callbacks; quarantined components can be inspected and explicitly retried through GameObject. |
| MovementBody | com.rift.engine.movement | Position, velocity, and grounded state. |
| MovementInput | com.rift.engine.movement | Device-independent horizontal/vertical intent, one-update jump press, and held-jump state for variable jump height. |
| MovementProfile | com.rift.engine.movement | Contract for converting input intent into body velocity. |
| TopDownMovementProfile | com.rift.engine.movement | Normalized free 2D movement. |
| PlatformerMovementProfile | com.rift.engine.movement | Basic side-view movement, gravity, and jump velocity; automatically leaves gravity to PhysicsWorld when the body is registered there. |\n| ResponsivePlatformerProfile | com.rift.engine.movement | Acceleration/deceleration, air control, coyote time, jump buffering, and optional variable jump height. |

## Physics

| API | Package | Purpose |
|---|---|---|
| Aabb | com.rift.engine.physics | Validated axis-aligned bounds used by entities, collision tests, and spatial queries. |
| PhysicsWorld | com.rift.engine.physics | Scene-owned 2D box/circle physics with spatial-hash broad phase, dynamic/static/kinematic bodies, gravity, basic collision response, sensors, filters, overlap queries, and raycasts. |
| PhysicsBody / PhysicsBodyType | com.rift.engine.physics | Body configuration and STATIC/KINEMATIC/DYNAMIC behavior. |
| CollisionShape | com.rift.engine.physics | Box and circle collision shapes. |
| CollisionFilter | com.rift.engine.physics | Category/mask and group filtering for collision pairs and queries. |
| CollisionContact | com.rift.engine.physics | Contact pair, normal, penetration, and sensor flag from the latest physics step. |\n| ContactPhase / PhysicsContactEvent | com.rift.engine.physics | Polled BEGIN, STAY, and END contact lifecycle events from the latest step. |
| RaycastHit | com.rift.engine.physics | Nearest segment ray hit with point, normal, and normalized fraction. |

Physics limitations: no polygon shapes, friction, restitution, joints, or continuous collision detection. Contact lifecycle is polled through `contactEvents()` rather than callbacks. Rotated boxes use enclosing AABBs. See the [physics guide](getting-started/physics.md).

## Inventory

| API | Package | Purpose |
|---|---|---|
| InventoryComponent | com.rift.engine.inventory | Slot-limited inventory with registered item definitions, stack limits, partial add acceptance, removal, and snapshots. |
| ItemDefinition / InventoryStack | com.rift.engine.inventory | Stable item metadata and immutable stack snapshots. |

Inventory limitations: no equipment slots, weight, crafting, item effects, UI, or save codec yet.

## Combat

| API | Package | Purpose |
|---|---|---|
| HealthComponent | com.rift.engine.combat | Clamped health, damage, healing, death callback, and explicit reset. |
| CombatWorld | com.rift.engine.combat | Scene-owned AABB hitbox/hurtbox overlap checks, team filtering, one-hit-per-target behavior, and timed hitboxes. |
| CombatHitbox / CombatHurtbox | com.rift.engine.combat | Owner-bound damage volumes and health receivers. |

Combat limitations: hitboxes use the owner's transformed AABB; no independent shapes, knockback, invulnerability frames, attack buffering, combo system, status effects, or rollback networking yet.

## Gameplay modules and narrative

| API | Package | Purpose |
|---|---|---|
| GameplayModule | com.rift.engine.module | Lifecycle contract for an optional, replaceable gameplay pack. |
| GameplayModuleDescriptor | com.rift.engine.module | Stable ID, version, dependencies, conflicts, and provided capabilities. |
| GameplayModuleManager | com.rift.engine.module | Validates installation, replacement, removal, dependencies, conflicts, and capability queries. |
| GameplayModuleContext | com.rift.engine.module | Services exposed to module lifecycle callbacks. |
| DialogueGraph | com.rift.engine.narrative | Validated collection of dialogue nodes and branch targets. |
| DialogueNode / DialogueChoice | com.rift.engine.narrative | Dialogue line data and player-selectable branches. |
| DialogueSession | com.rift.engine.narrative | Runs a conversation, advances lines, and applies selected choice effects. |
| StoryState | com.rift.engine.narrative | Story flags and integer values for quests, relationships, and route conditions. |
| StoryCondition / StoryEffect | com.rift.engine.narrative | Conditions for showing choices and effects applied after selection. |

The gameplay-module registry is a runtime extension point, not a dynamic binary loader. Modules must be compiled into the application. Version strings are informational; semantic version resolution and external distribution are not implemented.

## Rendering and assets

| API | Package | Purpose |
|---|---|---|
| RenderContext | com.rift.engine.graphics | Platform-neutral primitive, text, and texture drawing plus scoped state, transforms, and clipping. |
| Camera | com.rift.engine.graphics | Top-left anchored zoom camera applied to Scene world-object rendering; exposes world/screen coordinate conversions. |\n| CameraRig | com.rift.engine.graphics | Optional camera follow with smoothing, dead zone, world bounds, snap-to-target, and fading shake. |
| TextureResource / TextureLoader | com.rift.engine.graphics | Platform-neutral texture handle and platform-supplied decoding contract. |
| TextureRegion / Sprite / SpriteSheet | com.rift.engine.graphics | Atlas rectangles, drawable sprites, and regular grid indexing. |
| SpriteAnimation / SpriteAnimationComponent | com.rift.engine.animation | Time-based frame animation and reusable GameObject component. |\n| TileMapComponent | com.rift.engine.tilemap | Sprite-sheet-backed tile grid attached to a GameObject; caches frame sprites and skips empty cells. |
| AssetManager | com.rift.engine.assets | Validated ID-based synchronous texture cache with disposal and structured diagnostics. |

Current asset limitations: synchronous texture loading only; no font pipeline, compression, GPU batching, irregular atlas importer, or background streaming. Tile maps currently use regular sprite-sheet frames and do not include an editor or chunk streaming. See the [assets and animation guide](getting-started/assets-and-animation.md).

## UI

| API | Package | Purpose |
|---|---|---|
| UiCanvas | com.rift.engine.ui | Scene-owned screen-space layer with z-order, rendering, update, and pointer routing. |
| UiElement | com.rift.engine.ui | Base bounds, visibility, enabled state, z-index, and pointer lifecycle. |
| UiPanel / UiLabel / UiButton / UiImage | com.rift.engine.ui | Basic screen-space widgets, nested panels with absolute/vertical/horizontal layout, pointer routing, and TAB/ENTER/SPACE keyboard focus. |
| VirtualJoystick / VirtualButton | com.rift.engine.ui | Multi-touch virtual movement axes and press-and-hold touch actions. |

## Input

| API | Package | Purpose |
|---|---|---|
| EngineKey | com.rift.engine.input | Device-independent identifiers for letters, digits, navigation keys, modifiers, and F1–F12 on Android and desktop adapters. |
| PointerEvent / PointerEventType / PointerButtons | com.rift.engine.input | Device-independent mouse/touch pointer lifecycle and button identifiers, including multi-touch pointer IDs. |
| InputActionMap | com.rift.engine.input | Maps named actions to keyboard keys, pointer buttons, and normalized analog axes with dead zones; exposes held/pressed/released state and pointer position/delta. |

## Audio

| API | Package | Purpose |
|---|---|---|
| AudioResource / AudioLoader | com.rift.engine.audio | Platform-neutral playback handle and host-supplied audio decoder. |
| AudioManager | com.rift.engine.audio | ID-based sound/music cache, playback controls, volume, and disposal. |

Current input limitations: Android forwards common gamepad buttons and axes; Windows native XInput/DirectInput enumeration is not bundled. Windows adapters can feed the public GameEngine axis/key entrypoints.\n\nCurrent audio limitations: synchronous loading, Android MediaPlayer for both effects and music, Java Sound formats on Windows, and no mixer buses/spatial audio/cross-fades.

## Storage, plugins, and diagnostics

| API | Package | Purpose |
|---|---|---|
| SaveStore | com.rift.engine.storage | Platform-neutral string/float key-value save contract. |
| InMemorySaveStore | com.rift.engine.storage | In-memory implementation, useful for tests and examples. |
| EnginePlugin | com.rift.engine.plugin | Optional plugin lifecycle/update contract. |
| PluginManager | com.rift.engine.plugin | Installs, updates, lists, and uninstalls plugins by ID. |
| DiagnosticReporter | com.rift.engine.diagnostics | Bounded thread-safe diagnostic history and severity queries. |
| DiagnosticEntry / DiagnosticSeverity | com.rift.engine.diagnostics | Diagnostic data and INFO/WARNING/ERROR/FATAL levels. |

## Ownership and lifecycle notes

- GameEngine owns its SceneManager, PluginManager, GameplayModuleManager, AssetManager, and AudioManager; it receives SaveStore and DiagnosticReporter through its constructor.
- SceneManager.setInitialScene(scene) sets the first scene. changeScene(scene) queues a transition that is applied at a safe runtime boundary after the current update/render/input/lifecycle callback returns.
- Every activated scene receives the manager's shared GameState, AssetManager, and AudioManager.
- A Scene owns objects added through protected add/remove helpers. Mutations during traversal are deferred.
- A GameObject owns components added through addComponent/removeComponent. Component changes during update/render are deferred; component identity, not equality, defines attachment.
- GameObject updates its movement profile and integrates position unless registered with a PhysicsWorld. A Scene steps its PhysicsWorld after entity updates and updates its UI layer.
- Platform hosts own the game loop and concrete RenderContext, SaveStore, TextureLoader, and AudioLoader implementations. GameEngine.close() releases cached textures and audio resources.
- The current desktop host uses Swing scheduling; simulation and rendering currently share the Swing event-dispatch thread.

## Not present yet

Do not invent or use these as existing APIs: gamepad action bindings, collision callbacks, continuous collision detection, polygon collision shapes, font assets, audio buses/spatial audio/cross-fades, advanced UI layout/focus navigation, combat knockback/invulnerability/combo systems, automatic mode-transition effects, scene serialization, visual dialogue editor, or a complete visual-novel presentation layer. A hybrid 2D/3D renderer is a long-term direction, not an implemented capability.
