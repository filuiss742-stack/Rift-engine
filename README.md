# Rift Engine

Rift Engine is a modular Kotlin 2D game engine targeting **Android (APK)** and **Windows (desktop application image with an EXE launcher)**. Linux and macOS are not current targets.

The same game logic, scenes, entities, movement profiles, plugins and save API are shared. Platform modules supply rendering, input, persistence and application hosting. The architecture is intended for mixed-style games combining top-down exploration, side-view platforming, action combat and isometric presentation without duplicating game state.

## Start here

- [AI agent onboarding](docs/ai-onboarding.md) — short route for coding agents with limited context.
- [Current API map](docs/api-map.md) — real APIs and their current limitations.
- [Create your first scene](docs/getting-started/create-a-scene.md).
- [Input guide](docs/getting-started/input.md).
- [Movement guide](docs/getting-started/movement.md).
- [Saves and plugins](docs/getting-started/saves-plugins.md).
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

The core must not import Android, AWT or Swing. Platform adapters depend on the core, never the other way around.

## Build and test

- Android: `gradle :app:assembleDebug` (JDK 17, Android SDK 35, Gradle 8.9).
- Desktop development: `gradle :desktop-app:run` (JDK 17).
- Shared JVM tests: `gradle :engine:test :demo-game:test :platform-desktop:test`.
- Architecture boundary check: `python scripts/check_engine_boundaries.py`.

The Windows CI workflow uses `jpackage` to bundle a Java runtime and an EXE launcher. The result is an application image, not native compilation or a single-file executable. Launching the packaged artifact manually remains a separate smoke-test step.

## License

MIT. See [LICENSE](LICENSE).
