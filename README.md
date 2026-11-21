# Rift Engine

Rift Engine is an AI-first, modular Kotlin game engine targeting **Android (APK)** and **Windows (desktop application image with an EXE launcher)**. Linux and macOS are not current targets.

Its long-term direction is a hybrid 2D/3D gameplay constructor: one game can combine top-down exploration, side-view platforming, real-time action combat, first-person sequences, racing, mech battles, branching stories, and original modes without duplicating game state. These are design goals, not claims that every renderer or gameplay pack already exists.

The same shared runtime is used by both platform hosts. Platform modules supply rendering, input, persistence, audio, and application hosting. Optional gameplay modules can be installed, replaced, and removed through an explicit runtime registry.

## Start here

- [AI agent onboarding](docs/ai-onboarding.md) — short route for coding agents with limited context.
- [AI-native architecture](docs/architecture/ai-native-architecture.md) — target project model, agent workflow, safe transactions, diagnostics, and implementation sequence.
- [Current API map](docs/api-map.md) — real APIs and their current limitations.
- [Kotlin game template](templates/kotlin-game/README.md) — minimal game-owned project with its own Gradle build.
- [Create your first scene](docs/getting-started/create-a-scene.md).
- [GameObject components](docs/getting-started/components.md) — compose reusable behavior without genre-specific inheritance.
- [2D transforms](docs/getting-started/transforms.md) — object rotation, scale, render state, and transformed bounds.
- [2D camera](docs/getting-started/camera.md) — zoom, world/screen conversion, and screen-space overlays.
- [Input guide](docs/getting-started/input.md).
- [Movement guide](docs/getting-started/movement.md).
- [2D physics](docs/getting-started/physics.md) — bodies, collision filtering, sensors, contacts, queries, and raycasts.
- [Assets and animation](docs/getting-started/assets-and-animation.md) — texture loading, sprites, sprite sheets, and frame animation.
- [Screen-space UI](docs/getting-started/ui.md) — panels, labels, buttons, images, and pointer routing.
- [Audio](docs/getting-started/audio.md) — sound effects, background music, volume, and resource lifetime.\n- [Rift Diagnostics](docs/getting-started/diagnostics.md) — structured errors, suggestions, and failure isolation.
- [Gameplay modules](docs/getting-started/gameplay-modules.md) — installable and replaceable gameplay packs.
- [Shared game state](docs/getting-started/shared-game-state.md) — retain progression across scenes and gameplay modes.
- [Dialogue and choices](docs/getting-started/dialogue-and-choices.md) — branching conversations and story consequences.
- [Saves and plugins](docs/getting-started/saves-plugins.md).
- [Project manifest format](docs/project-manifest.md) — machine-readable project model and validation.
- [0.5.0 release gates](docs/roadmap/0.5.0-release-gates.md) — scope and acceptance criteria for the first usable 2D game-engine milestone.
- [1.0.0 long-term gates](docs/roadmap/1.0.0-release-gates.md) — future stable-release requirements.
- [Practical cookbook](docs/cookbook.md).
- [Change matrix](docs/architecture/change-matrix.md) — module choice and validation.
- [Full AI contributor rules](AI_RULES.md).

## Modules

- :engine — pure Kotlin/JVM runtime and platform-neutral contracts.
- :platform-android — Android SurfaceView/Canvas, touch input, storage, textures, and audio.
- :platform-desktop — Windows desktop windowing, AWT rendering, keyboard/mouse, file saves, textures, and audio.
- :demo-game — shared demo scene.
- :app — Android APK host.
- :desktop-app — Windows desktop host.
- :tools:project-validator — command-line validator for Rift project manifests.

The core must not import Android, AWT, or Swing. Platform adapters depend on the core, never the other way around.

## Build and test

- Android: `gradle :app:assembleDebug` (JDK 17, Android SDK 35, Gradle 8.9).
- Desktop development: `gradle :desktop-app:run` (JDK 17).
- Shared JVM tests: `gradle :engine:test :demo-game:test :platform-desktop:test :tools:project-validator:test`.
- Manifest validator: `gradle :tools:project-validator:run --args="examples/ai-game/rift.project.json"`.
- Kotlin template test: run `gradle test` from `templates/kotlin-game`.
- Architecture boundary check: `python scripts/check_engine_boundaries.py`.

The Windows CI workflow uses jpackage to bundle a Java runtime and an EXE launcher. The result is an application image, not native compilation or a single-file executable. Launching the packaged artifact manually remains a separate smoke-test step.

The current Kotlin template is buildable inside this repository using a Gradle composite build against the local engine source. Publishing the engine artifact and producing independent Android/Windows launcher templates are still release gates; do not describe the template as fully standalone yet.

## License

MIT. See [LICENSE](LICENSE).
