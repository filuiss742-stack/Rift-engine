# Rift Engine Architecture Principles

Rift Engine is a modular, composable game engine. Its long-term direction is hybrid 2D/3D gameplay: top-down exploration, side-view platforming, real-time action combat, first-person sequences, racing, mech battles, and other modes should be combinable within one game and share persistent world state and progression.

The first release targets are Android and Windows. Linux/macOS are not current release targets.

## Platform boundaries
- `:engine` is pure Kotlin/JVM: no Android, AWT or Swing imports.
- `:platform-android` owns Android rendering, touch and storage.
- `:platform-desktop` owns Windows rendering, keyboard/mouse and storage.
- `:demo-game` is shared by both application hosts.
- Platform modules depend on core; core never depends on a platform.

## Composition
A genre is not a permanent project template. Game modes are assembled from optional capabilities and systems. Keep world state, presentation, movement, physics, combat, input, camera, and scene flow independent. Prefer composition and replaceable implementations over genre-specific class hierarchies or controllers full of mode flags.

A module pack is not automatically a complete genre implementation. Packs declare stable IDs, dependencies, conflicts, and capabilities. Games compose their actual behavior from installed modules and game-specific systems.

## Extensibility and diagnostics
Optional systems must not become mandatory core dependencies. Module installation, replacement, and removal must be explicit. Validate declared dependencies and conflicts before activation, prevent removal of required dependencies, and report actionable diagnostics. Metadata validation does not prove behavioral compatibility.

Diagnostics distinguish confirmed errors, warnings and hypotheses, provide actionable context, and never silently rewrite code or claim to detect every bug.

## Rendering direction
The current platform renderers are basic 2D implementations. A hybrid 2D/3D renderer is a long-term architectural goal, not a current feature. Avoid locking the public rendering API to Canvas or AWT; introduce explicit render capabilities and backend boundaries before claiming 3D support.

## Development rule
Build small, working, testable systems. Each subsystem should unlock a real game-development capability. Update docs and examples with public API changes, and never describe planned features as implemented.
