# Rift Engine

**Rift Engine** is an experimental, modular 2D game engine for Android, written in Kotlin.

It is an independent, **AI-first** engine. Games made with it live in separate projects. The main design goal is a small, explicit API that both people and AI coding agents can understand and modify safely.

Rift Engine is designed for multi-style 2D games: a single project should be able to combine top-down exploration, side-view platforming, action combat, and isometric presentation while keeping shared game state and progression. This is an architectural direction; features are implemented incrementally and the roadmap below tracks unfinished systems.

## AI contributors

Before changing code, read:

- [AGENTS.md](AGENTS.md) — short mandatory instructions for AI agents.
- [AI_RULES.md](AI_RULES.md) — full architecture, coding, change and testing rules.
- [Engine architecture principles](docs/architecture/engine-principles.md) — universal gameplay and presentation design.
- [Rift Diagnostics](docs/diagnostics.md) — runtime error reporting and fallback screen.

Keep changes small. Inspect callers before editing. Do not rewrite unrelated systems. Update docs with API changes and report only builds/tests that actually ran.

## Current foundation (0.1.x)

- Android application entry point and real-time game loop
- Scene lifecycle and scene switching
- Canvas-based 2D rendering
- Game objects with position, size, velocity and color
- Touch input
- Camera coordinate conversion helpers
- AABB collision helpers
- Plugin interface and plugin manager
- Persistent key/value save storage
- Interactive sandbox scene
- Runtime diagnostics with structured error entries and a fallback screen
- Detection of duplicate/empty plugin IDs and missing active scenes

## Build

Requires JDK 17, Android SDK 35 and Gradle 8.9:

    gradle assembleDebug

The debug APK is generated at app/build/outputs/apk/debug/app-debug.apk.

## Architecture

- core/ — engine runtime, game loop and scene manager
- scene/ — scene base class
- entity/ — game objects and components
- graphics/ — drawing, camera helpers and presentation
- input/ — input state and touch controls (planned)
- physics/ — collision and movement helpers
- diagnostics/ — runtime reports and fallback screen
- plugin/ — extension API
- storage/ — save data
- demo/ — examples and test scenes only

## Roadmap

1. Automated build and basic runtime smoke tests
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
