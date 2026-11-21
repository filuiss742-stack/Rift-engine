# Instructions for AI Agents

Before working in this repository, read `AI_RULES.md`.

Rift Engine is an AI-first, simple 2D Android game engine. Keep changes small, explicit, predictable, modular, and easy to verify. Avoid spaghetti code and unnecessary rewrites.

Mandatory:
- Inspect the relevant source files and their callers before editing.
- Do not rewrite unrelated systems or invent APIs.
- Preserve public APIs unless a breaking change is explicitly requested.
- Prefer straightforward Kotlin over clever abstractions.
- Keep classes and functions focused; avoid tangled dependencies, circular references, giant manager classes, and duplicated logic.
- Keep game-specific content out of the engine core.
- Every new or materially upgraded plugin must include an updated `docs/plugins/<plugin-id>.md` explaining its purpose, internal behavior, setup, configuration, usage, API, dependencies, limitations, troubleshooting, and migration notes where needed.
- Update documentation and examples when public APIs change.
- Run the build/tests when possible and report only verified results.
- If a task requires a major architecture decision or is ambiguous, ask first.

See `AI_RULES.md` for the full contribution, architecture, anti-spaghetti, and plugin documentation rules.
