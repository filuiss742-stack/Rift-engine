# Rift Engine

Rift Engine is an AI-first, modular Kotlin game engine targeting **Android (APK)** and **Windows (desktop application image with an EXE launcher)**. Linux and macOS are not current targets.

Its long-term direction is a hybrid 2D/3D gameplay constructor: one game can combine top-down exploration, side-view platforming, real-time action combat, first-person sequences, racing, mech battles, branching stories, and original modes without duplicating game state. These are design goals, not claims that every renderer or gameplay pack already exists.

The same shared runtime is used by both platform hosts. Platform modules supply rendering, input, persistence and application hosting. Optional gameplay modules can be installed, replaced and removed through an explicit runtime registry.

## Start here

- [AI agent onboarding](docs/ai-onboarding.md) — short route for coding agents with limited context.
- [AI-native architecture](docs/architecture/ai-native-architecture.md) — target project model, agent workflow, safe transactions, diagnostics and implementation sequence.
- [Current API map](docs/api-map.md) — real APIs and their current limitations.
- [Create your first scene](docs/getting-started/create-a-scene.md).
- [GameObject components](docs/getting-started/components.md) — compose reusable behavior without genre-specific inheritance.
- [2D transforms](docs/getting-started/transforms.md) — object rotation, scale, render state, and transformed bounds.
- [Input guide](docs/getting-started/input.md).
- [Movement guide](docs/getting-started/movement.md).
- [Gameplay modules](docs/getting-started/gameplay-modules.md) — installable and replaceable gameplay packs.
- [Dialogue and choices](docs/getting-started/dialogue-and-choices.md) — branching conversations and story consequences.
- [Saves and plugins](docs/getting-started/saves-plugins.md).
- [Project manifest format](docs/project-manifest.md) — machine-readable project model and validation.
- [1.0.0 release gates](docs/roadmap/1.0.0-release-gates.md) — evidence-based definition of a complete release.
- [Practical cookbook](docs/cookbook.md).
- [Change matrix](docs/architecture/change-matrix.md) — module choice and validation.
- [Full AI contributor rules](AI_RULES.md).

## Modules

- `:engine`: pure Kotlin/JVM runtime and platform-neutral contracts.
- `:platform-android`: Android SurfaceView/Canvas, touch input and SharedPreferences.
- `:platform-desktop`: Windows desktop windowing, AWT rendering, keyboard/mouse and file saves.
- `:demo-game`: shared demo scene.
- `:app`: Android APK host.
- `:desktop-app`: Windows desktop host.
- `:tools:project-validator`: command-line validator for Rift project manifests.

The core must not import Android, AWT or Swing. Platform adapters depend on the core, never the other way around.

## Build and test

- Android: `gradle :app:assembleDebug` (JDK 17, Android SDK 35, Gradle 8.9).
- Desktop development: `gradle :desktop-app:run` (JDK 17).
- Shared JVM tests: `gradle :engine:test :demo-game:test :platform-desktop:test`.
- Manifest validator: `gradle :tools:project-validator:run --args="examples/ai-game/rift.project.json"`.
- Architecture boundary check: `python scripts/check_engine_boundaries.py`.

The Windows CI workflow uses `jpackage` to bundle a Java runtime and an EXE launcher. The result is an application image, not native compilation or a single-file executable. Launching the packaged artifact manually remains a separate smoke-test step.

## License

MIT. See [LICENSE](LICENSE).
