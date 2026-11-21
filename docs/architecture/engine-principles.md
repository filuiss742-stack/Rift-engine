# Rift Engine Architecture Principles

Rift Engine is a modular 2D engine for mixed-style games. Top-down exploration, side-view platforming, action combat and isometric presentation should share world state and progression.

The first targets are Android and Windows. Linux/macOS are not current release targets.

## Platform boundaries
- `:engine` is pure Kotlin/JVM: no Android, AWT or Swing imports.
- `:platform-android` owns Android rendering, touch and storage.
- `:platform-desktop` owns Windows rendering, keyboard/mouse and storage.
- `:demo-game` is shared by both application hosts.
- Platform modules depend on core; core never depends on a platform.

## Composition
Keep world state, presentation, movement, physics, combat, input and scene flow independent. Prefer small composable systems over genre-specific class hierarchies or controllers full of mode flags.

## Extensibility and diagnostics
Optional systems must not become mandatory core dependencies. Plugins require complete docs in `docs/plugins/`. Diagnostics distinguish confirmed errors, warnings and hypotheses, provide actionable context, and never silently rewrite code or claim to detect every bug.

Build small, testable layers. Update docs/examples with API changes and never describe planned features as implemented.
