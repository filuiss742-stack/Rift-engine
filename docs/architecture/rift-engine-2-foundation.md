# Rift Engine 2 — Foundation and Adoption Plan

Status: **proposed, not yet implemented**  
Working branch: `engine/v2-foundation`  
Baseline: `feature/prologue-procedural-materials`

## 1. Mission

Rift Engine exists to make it easy for AI agents and humans to build games safely. It is not a goal to reimplement every low-level game technology.

Keep the public project identity and the useful Rift-specific systems. Replace infrastructure only when a proven external library reduces maintenance without making the architecture harder to understand.

The engine must remain:
- Kotlin-first for game rules, state, modules, diagnostics, and tools.
- Android + Windows/Desktop first.
- Small, explicit, documented, testable, and easy for coding agents to modify.
- Safe for parallel AI work: narrow ownership, isolated branches, no destructive operations.
- Able to support a 3D world with 2D directional sprite characters and other gameplay modes.

## 2. Current repository facts

The current project already separates:
- `:engine` — Kotlin/JVM game lifecycle, scenes, entities, physics, rendering contracts, diagnostics, plugins.
- `:platform-android` — Android host and Canvas renderer.
- `:platform-desktop` — desktop host and AWT renderer.
- `:demo-game` — shared demo/game logic.
- `:app` and `:desktop-app` — launchers and packaging.

This separation is worth preserving. Do not replace it with a single monolithic module.

The current renderer is a custom software renderer. Reimplementing a mature 3D renderer, asset pipeline, input layer, audio layer, and platform backends is a large ongoing cost. The first goal is to stop expanding those low-level systems unless a concrete Rift-specific requirement demands it.

## 3. Candidate technology decisions

### Primary candidate: libGDX

Use libGDX as the first integration spike because it:
- is Java-based and Kotlin-friendly;
- supports Android and desktop from a shared codebase;
- provides 2D and 3D rendering APIs, asset management, input, audio, and scene UI;
- is distributed under Apache-2.0;
- supports a headless/automated test workflow through its normal Java/Gradle ecosystem.

Official sources:
- https://libgdx.com/
- https://github.com/libgdx/libgdx

**Important:** libGDX is a framework, not a full visual editor like Godot. It does not automatically provide a game editor or a complete authoring workflow. Rift-specific scene composition and AI-facing tools still need to remain small and explicit.

### Physics candidate: Jolt Physics through a maintained Java/Kotlin binding

Jolt is MIT-licensed and has Java/Kotlin bindings and Android native binaries through the Jolt-JNI project.

Official sources:
- https://github.com/jrouwe/JoltPhysics
- https://stephengold.github.io/jolt-jni-docs/

Do not integrate Jolt into the runtime until a small spike proves:
1. Android ARM64 native loading works in CI.
2. Windows x86-64 native loading works in CI.
3. A minimal body, static collider, overlap/query, and fixed-step simulation work.
4. Native library packaging does not create an unacceptable APK-size or maintenance burden.
5. The wrapper is actively maintained and its transitive licenses are recorded.

Until then, keep the existing physics implementation. Do not add two physics engines to production.

### Reference only: Godot

Godot is a useful reference for scene/resource workflows, editor design, import pipelines, and headless export. Do not embed Godot wholesale into the Kotlin runtime in this phase. It would create a second engine boundary and complicate debugging, packaging, and AI ownership.

Official documentation:
- https://docs.godotengine.org/

### Other dependencies

Do not add a dependency merely because it is popular. Each candidate must solve one named problem, have a compatible license, work on both target platforms, and have a test that proves the integration.

Avoid adding a large ECS framework, dependency-injection framework, scripting VM, or plugin marketplace unless a demonstrated need appears. Rift's existing small composition model should remain understandable.

## 4. Target architecture

Keep the Rift-specific public API small:

- **Rift Core (Kotlin):** game lifecycle, game state, scene flow, entity/component contracts, module registry, diagnostics, save contracts.
- **Rift Runtime Adapter:** thin bridge to libGDX application lifecycle, assets, input, audio, rendering, and UI.
- **Platform launchers:** Android and Windows/Desktop launchers only; no duplicated gameplay logic.
- **Rift Game Modules:** combat, dialogue/story, inventory, RTS/warfront, and other optional gameplay features.
- **Rift Tools:** asset validation, sprite-atlas builder, project validator, AI-readable docs, build/test automation.

Dependency direction:
`Game Modules -> Rift Core APIs`  
`Runtime Adapter -> Rift Core APIs`  
`Platform Launchers -> Runtime Adapter`  
Never make Rift Core depend on Android, AWT, Swing, or a platform launcher.

Do not let game modules call libGDX APIs directly. They should use Rift-owned interfaces so a backend can be replaced without rewriting game rules.

## 5. Preserve the valuable Rift-specific work

Keep and adapt:
- eight-direction sprite selection and independent source frames (no mirroring missing directions);
- atlas builder and image validation;
- animation action/priority rules;
- story flags, dialogue, and narrative state;
- real-time combat rules and telegraph timing;
- Rift Diagnostics with INFO/WARNING/ERROR/FATAL and confirmed/warning/hypothesis distinctions;
- plugin/module contracts, AI rules, branch safety, and build automation.

These are product features and AI-workflow features, not low-level engine infrastructure.

## 6. Migration sequence — one verified step at a time

### Phase 0 — freeze and baseline
- Do not merge or rewrite the current prologue PR as part of this experiment.
- Preserve all branches and tags.
- Record the current baseline commit and CI state.
- Keep the old runtime buildable while the new adapter is developed.

### Phase 1 — libGDX proof of integration
Create a minimal isolated sample with:
- one Android launcher;
- one desktop launcher;
- one 3D floor and cube;
- one billboard 2D sprite;
- camera orbit/direction selection;
- touch joystick and keyboard input;
- one static collision and one moving body;
- one dialogue panel.

The sample must compile and run on Android and Windows CI. Do not port the full game yet.

### Phase 2 — compare against current runtime
Measure and document:
- Android APK size;
- cold start and memory;
- frame time on emulator and, later, a physical Redmi Note 11 Pro;
- renderer feature coverage;
- asset-loading complexity;
- AI-agent change size and testability;
- Android/Desktop build reliability.

Do not claim device performance gains from CI emulator results.

### Phase 3 — adopt selectively
Only after Phase 1 and Phase 2 pass:
- route rendering, asset loading, input, and audio through libGDX;
- retain Rift Core game-state and module APIs;
- retain current physics temporarily unless Jolt independently passes its spike;
- move old renderer behind a temporary compatibility boundary, then remove it only after parity tests pass.

### Phase 4 — gameplay migration
Migrate one feature at a time with regression tests. The game project itself is postponed until the engine foundation is stable.

## 7. Non-negotiable simplicity rules

1. One production renderer backend.
2. One production physics backend.
3. One source of truth for game state.
4. No global service locator or hidden dependency injection.
5. No reflection-based plugin discovery.
6. No giant manager classes or mode-flag god objects.
7. No duplicate Android/Desktop gameplay logic.
8. Every dependency has a pinned version, license record, platform matrix, and smoke test.
9. Every migration step is reversible and isolated in a branch/PR.
10. Never report a build, test, device run, or performance result unless it actually ran.

## 8. Exit criteria

Rift Engine 2 foundation is not considered complete until:
- Android and Windows/Desktop builds pass from clean CI environments;
- the 3D + 2D billboard sample runs on both targets;
- sprite direction and animation tests pass;
- input, collision, and dialogue smoke tests pass;
- no existing branch or tag was deleted;
- third-party notices and dependency versions are committed;
- AI onboarding explains the real APIs and exact build/test commands;
- the migration decision is supported by measured results.

The first implementation task is Phase 1, not a full rewrite.
