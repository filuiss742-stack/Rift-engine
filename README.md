# Rift Engine

**Rift Engine** is an experimental, modular 2D game engine for Android, written in Kotlin.

It is an independent, **AI-first** engine. Games made with it live in separate projects. The main design goal is a small, explicit API that both people and AI coding agents can understand and modify safely.

## AI contributors

Before changing code, read:

- [AGENTS.md](AGENTS.md) — short mandatory instructions for AI agents.
- [AI_RULES.md](AI_RULES.md) — full architecture, coding, change and testing rules.

Keep changes small. Inspect callers before editing. Do not rewrite unrelated systems. Update docs with API changes and report only builds/tests that actually ran.

## Current foundation (0.1.0)

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

## Build

Requires JDK 17, Android SDK 35 and Gradle 8.9:

```bash
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

- `core/` — engine runtime, game loop and scene manager
- `scene/` — scene base class
- `entity/` — game objects
- `graphics/` — camera helpers
- `physics/` — collision helpers
- `plugin/` — extension API
- `storage/` — save data
- `demo/` — engine test scene

## Roadmap

1. Resource and sprite/animation pipeline
2. Input mapping and virtual touch controls
3. Camera integration and world transforms
4. Collision world and physics stepping
5. Scene serialization and richer save slots
6. Editor and asset pipeline

## License

MIT. See [LICENSE](LICENSE).
