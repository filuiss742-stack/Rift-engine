# Rift Engine

**Rift Engine** is an experimental, modular 2D game engine for Android, written in Kotlin.

It is an independent, **AI-first** engine. Games made with it live in separate projects. The main design goal is a small, explicit API that both people and AI coding agents can understand and modify safely.

Rift Engine is designed for multi-style 2D games: a single project should be able to combine top-down exploration, side-view platforming, action combat, and isometric presentation while keeping shared game state and progression. This is an architectural direction; features are implemented incrementally.

## Project modules

- `:engine` — reusable Android library containing the engine runtime and public systems.
- `:app` — demonstration Android application that depends on `:engine` and contains the sandbox scene.

Game-specific scenes belong to an application/game module, not to the engine library. The engine receives its initial scene from the host application.

## AI contributors

Before changing code, read:

- [AGENTS.md](AGENTS.md)
- [AI_RULES.md](AI_RULES.md)
- [Engine architecture principles](docs/architecture/engine-principles.md)
- [Rift Diagnostics](docs/diagnostics.md)

Keep changes small. Inspect callers before editing. Do not rewrite unrelated systems. Update docs with API changes and report only builds/tests that actually ran.

## Current foundation (0.1.x)

- Android library module for reusable engine code
- Demo application separated from engine implementation
- Scene lifecycle and scene switching
- Canvas-based 2D rendering
- Game objects with position, size, velocity and color
- Queued touch input applied on the game-loop thread
- Camera coordinate conversion helpers
- AABB collision helpers
- Plugin interface and plugin manager
- Persistent key/value save storage
- Runtime diagnostics with structured error entries and a fallback screen

## Build

Requires JDK 17, Android SDK 35 and Gradle 8.9:

    gradle :app:assembleDebug

The debug APK is generated at app/build/outputs/apk/debug/app-debug.apk.

## Architecture

The reusable engine source lives under `engine/src/main/java/com/rift/engine/`:

- core/ — engine runtime, Android surface host and scene manager
- scene/ — scene base class
- entity/ — game objects
- graphics/ — camera helpers and presentation
- physics/ — collision helpers
- diagnostics/ — runtime reports and fallback screen
- plugin/ — extension API
- storage/ — save data

The demo application lives under `app/` and contains `MainActivity` and `SandboxScene`.

## Roadmap

1. Gradle Wrapper, automated tests and stronger CI checks
2. Resource validation and sprite/animation pipeline
3. Input actions and configurable virtual controls
4. Camera integration, render layers and world transforms
5. Composable movement profiles (top-down and platform movement)
6. Physics stepping, gravity, ground detection and collision world
7. Reusable action-combat systems and mode transitions
8. Scene serialization, save slots and asset pipeline
9. Editor and visual diagnostics tools

## License

MIT. See [LICENSE](LICENSE).
