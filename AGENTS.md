# Instructions for AI Agents

Before working in this repository, read `AI_RULES.md`.

Rift Engine is an AI-first, modular 2D Android game engine. Its long-term direction is universal multi-style games: top-down, side-view platforming, action combat, and isometric presentation can coexist without duplicating shared game state. Keep changes small, explicit, predictable, modular, and easy to verify.

Mandatory:
- Inspect the relevant source files and their callers before editing.
- Do not rewrite unrelated systems or invent APIs.
- Preserve public APIs unless a breaking change is explicitly requested.
- Prefer straightforward Kotlin over clever abstractions.
- Keep classes and functions focused; avoid tangled dependencies, circular references, giant manager classes, and duplicated logic.
- Separate shared game state from presentation, movement, physics, combat, and input.
- Do not build genre-specific systems that force separate copies of player data or progression.
- Keep game-specific content out of the engine core.
- Use Rift Diagnostics for structured errors; distinguish confirmed errors from warnings and hypotheses. Never silently rewrite code or claim every bug can be detected.
- Every new or materially upgraded plugin must include an updated `docs/plugins/<plugin-id>.md` explaining its purpose, internal behavior, setup, configuration, usage, API, dependencies, limitations, troubleshooting, and migration notes where needed.
- Update documentation and examples when public APIs change.
- Run the build/tests when possible and report only verified results.
- If a task requires a major architecture decision or is ambiguous, ask first.

See `AI_RULES.md`, `docs/architecture/engine-principles.md`, and `docs/diagnostics.md` for the full requirements.
