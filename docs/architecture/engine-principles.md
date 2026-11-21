# Rift Engine Architecture Principles

## Product direction

Rift Engine is a modular 2D engine for games that combine multiple presentation and gameplay styles. It is not a top-down RPG engine or a platformer engine. A single game should be able to combine top-down exploration, side-view platforming, side-view action combat, and isometric presentation without maintaining separate copies of its world, character data, or progression.

This is an architectural target, not a claim that every listed feature is implemented today.

## Core rule: separate game data from presentation and controls

Keep these concerns independent:

- **World and game state:** entities, health, stats, inventory, quests, world flags.
- **Presentation:** camera, projection, render ordering, sprites, animation and effects.
- **Movement profile:** top-down movement, platform movement, free movement, scripted movement.
- **Physics profile:** collision rules, gravity, ground detection, impulses and movement constraints.
- **Combat:** attacks, hit detection, abilities, cooldowns, blocking, dodging and combos.
- **Input mapping:** actions such as move, jump, interact, dodge and attack, independent of a specific screen layout.
- **Scene flow:** loading, unloading and transitions between areas or gameplay modes.

A mode change should replace or reconfigure only the systems that need to change. It must not require duplicating character progression or rewriting unrelated gameplay systems.

## Composition over genre-specific inheritance

Prefer small components and explicit profiles that can be combined. Do not create a large class hierarchy for every genre. Avoid one universal controller with dozens of boolean flags.

A component should have one clear responsibility and a small documented API. Conflicting ownership of the same state (for example, two movement systems both writing vertical velocity) should be reported as a diagnostic conflict where the engine can detect it.

## Module boundaries

- The `:engine` module is an Android library and must not depend on demo/game content.
- The `:app` module is a sample host application and may depend on `:engine`.
- The engine receives scenes and game-specific configuration from the host application.
- Game-specific entities, scenes, assets and rules must not be imported into engine packages.

## Extensibility

- A basic game should work with a small set of built-in systems.
- Advanced games can replace or extend individual systems.
- Optional systems must not become mandatory dependencies of the core.
- New plugins and materially upgraded plugins require a complete guide in docs/plugins/.
- Public APIs should remain explicit, stable, and easy for both humans and AI agents to discover.

## AI-first, human-friendly

The engine API should be discoverable through source, examples, and documentation. Prefer compile-time types, validation at boundaries, stable IDs, explicit ownership, and deterministic behavior. Avoid reflection-based magic, hidden global state, and large configuration surfaces.

AI agents and human developers use the same public API. No AI service is required to build or run a game.

## Diagnostics as a core capability

Rift Diagnostics should progressively cover:

1. Runtime exceptions and readable fallback screens.
2. Missing scenes, invalid configuration, and plugin identity conflicts.
3. Resource and asset validation.
4. System ownership and dependency conflicts.
5. Scene, camera, and input configuration checks.
6. Performance warnings and profiling.

Every report should distinguish confirmed errors from warnings and hypotheses. Include a stable code, subsystem, actionable message, and source location when available. Do not claim that all bugs can be detected. Never silently rewrite project code.

## Implementation discipline

Build the engine in small, testable layers. Implement and verify one foundation at a time. Keep game-specific content out of core packages. Update docs and examples with public API changes. Do not introduce abstractions until a real use case needs them.
