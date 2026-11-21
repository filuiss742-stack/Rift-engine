# AI Rules for Rift Engine

These rules are mandatory for every AI agent and human contributor working on this repository.

## 1. Main goal

Rift Engine is an AI-first, modular Kotlin game engine targeting Android and Windows. Its long-term direction is a hybrid 2D/3D gameplay constructor: a single game may combine top-down exploration, platforming, real-time action combat, first-person sequences, racing, mech battles, and original modes while sharing world state and progression. These are design goals; do not claim unimplemented renderer or gameplay features exist.

The engine must be easy for people and AI coding agents to understand, change safely, and verify. Prefer a small, explicit, predictable API over clever abstractions or unnecessary features. Linux and macOS are not current release targets.

## 2. AI-native collaboration contract

AI agents are first-class development clients, but they do not have permission to bypass project safety or ownership boundaries.

1. Treat the human as project owner and decision-maker. Explain material trade-offs; never merge a PR or make destructive changes without explicit approval.
2. Inspect the smallest relevant context. Do not load or rewrite the entire repository for a focused task.
3. Use real, discoverable APIs. Never invent tools, commands, classes, or capabilities.
4. Keep every change scoped, reviewable, and reversible. Show the changed files, diagnostics, and checks actually executed.
5. Do not silently modify unrelated assets, scenes, settings, or source files.
6. Separate implemented capabilities from design goals and planned tooling.
7. Prefer typed, structured operations over brittle text manipulation when engine tooling supports them.
8. Never treat generated code or third-party modules as trusted without validation.

See [AI-Native Architecture](docs/architecture/ai-native-architecture.md) for the target project model, agent operations, transaction workflow, observability requirements, and implementation sequence. That document is a roadmap; it does not claim that the planned agent bridge or editor tooling already exists.

## 3. Golden rules

1. Read this file and the relevant source files before changing code.
2. Make the smallest complete change that solves the requested task.
3. Do not rewrite, rename, move, or replace working systems unless the task explicitly requires it.
4. Do not invent APIs. Check actual classes and callers before using them.
5. Keep public APIs consistent. Prefer a few obvious methods with clear names.
6. Avoid hidden behavior, reflection, runtime code generation, global mutable state, and complex dependency injection.
7. Do not add a third-party dependency without explaining why it is needed and keeping the dependency footprint small.
8. Preserve existing behavior unless the task explicitly asks to change it.
9. If a change affects public API, update relevant docs and examples in the same change.
10. Never claim code builds or tests pass unless a build/test actually ran successfully.
11. Avoid tangled responsibilities, circular dependencies, unexplained shared state, and long methods that mix unrelated tasks.
12. Every new or upgraded plugin must ship with clear, current documentation and usage instructions.
13. Prefer universal, composable systems over genre-locked class hierarchies or duplicated implementations.
14. Classify detected problems honestly as confirmed errors, warnings, and hypotheses. Never present a guess as a proven cause.

## 4. Kotlin style and maintainability

- Use Kotlin and the existing package structure.
- Use descriptive names such as `player`, `sceneManager`, and `deltaSeconds`.
- Keep functions short and focused; extract helpers when they clarify the flow.
- Prefer explicit types for public APIs and non-obvious values.
- Avoid clever one-line expressions when a normal block is easier to read.
- Avoid unnecessary generics, inheritance layers, DSLs, annotations, and operator overloading.
- Keep each class and function responsible for one clear job.
- Separate input handling, game logic, rendering, persistence, and platform integration.
- Prefer composition over deep inheritance.
- Keep dependencies flowing in one direction. Core systems must not depend on demo code or game-specific features.
- Avoid circular dependencies between packages or modules.
- Do not create giant manager classes, god objects, or utility files that collect unrelated behavior.
- Comment public behavior and non-obvious decisions, not every line.
- Format Kotlin consistently. Do not compress multiple declarations or statements onto one line.

## 5. Engine and platform architecture

Keep the architecture separated:

- `:engine`: pure Kotlin/JVM lifecycle, scenes, entities, movement, physics, input contracts, diagnostics, gameplay-module contracts, plugin contracts, and storage/rendering abstractions.
- `:platform-android`: Android lifecycle, current Canvas renderer, touch/gamepad integration, and SharedPreferences.
- `:platform-desktop`: Windows windowing, current AWT renderer, keyboard/mouse/gamepad integration, and filesystem storage.
- `:demo-game`: shared example game code only.
- `:app`: Android application entry point and APK.
- `:desktop-app`: Windows application entry point and packaging.

Platform dependencies must point toward the shared engine. The engine must not import Android, AWT, Swing, or other host-specific APIs. Shared game code must not depend on either platform adapter.

Keep shared game state separate from presentation, movement, physics, combat, input, camera, assets, and scene flow. A mode transition should change only the systems required for that mode; do not duplicate player progression or create separate engines for each genre. Avoid a giant controller full of mode flags.

## 6. Gameplay module requirements

- Gameplay packs are optional modules, not mandatory genre templates.
- Every module has a stable ID and version and declares module-ID dependencies, conflicts, and provided capabilities.
- Installation, replacement, and removal are explicit operations.
- Validate declared dependencies and conflicts before activating a module.
- Do not remove a module while installed modules depend on it.
- A replacement must retain the same stable ID and should attempt to restore the old implementation if activation fails.
- A module update failure must be diagnosed without automatically stopping unrelated modules.
- Metadata validation does not prove behavioral compatibility. Document limitations honestly.
- Dynamic downloading, arbitrary binary loading, sandboxing, semantic version resolution, and a graphical module hub are not current features.

## 7. Public API rules

- Prefer explicit APIs and verify them against the current source before documenting examples.
- Use consistent units and document them. Positions and sizes are measured in logical pixels unless a future API explicitly says otherwise.
- Use seconds for time values; name them `deltaSeconds`.
- Make object ownership and lifecycle clear.
- Validate invalid inputs at API boundaries and return understandable errors where appropriate.
- Avoid methods that silently do nothing when the caller likely made a mistake.
- Keep defaults safe and documented.
- Do not expose Android, AWT, Swing, or other platform-specific types through shared public APIs.

## 8. Plugin requirements

Every new plugin and every materially upgraded plugin must have its own documentation file, normally `docs/plugins/<plugin-id>.md`. Do not consider the plugin complete until its documentation is updated in the same change.

Each plugin document must explain purpose, lifecycle, setup, configuration, usage, public API/events, dependencies, platform compatibility, limitations, troubleshooting, and migration notes where relevant.

Keep plugins isolated from the engine core where practical. Use stable IDs, define ownership and cleanup, and never silently change existing behavior.

## 9. Change protocol

Before editing:
1. Identify the exact files involved.
2. Read the relevant source and callers.
3. State the intended change.
4. Check whether the change can be made without breaking existing APIs.

After editing:
1. Review every changed file.
2. Search for affected call sites and update them.
3. Add or update a small example when a public API changes.
4. Update docs and examples where needed.
5. Run available builds and tests.
6. Report exactly what changed, what was tested, and what remains unverified.

## 10. Diagnostics requirements

Rift Diagnostics is a core engine capability, not an optional plugin.

- Report runtime exceptions at engine boundaries with stable codes, readable messages, and source locations when available.
- Detect known configuration problems and identity/dependency conflicts as early as practical.
- Distinguish confirmed errors, warnings, and hypotheses.
- Keep a bounded, thread-safe recent-event history.
- Separate recoverable errors from fatal states.
- A failing optional module or plugin must not automatically stop unrelated systems.
- Never silently rewrite project code or claim complete bug detection.

## 11. Testing and build safety

- Android APK and Windows desktop application must continue to build with configured Gradle and JDK versions.
- Add deterministic tests for module lifecycle, dependencies, conflicts, replacement, input, movement, collision, timing, and diagnostics when those systems exist.
- Keep demo scenes usable as manual smoke tests on both supported platforms.
- A successful package build is not proof that the Windows artifact was manually launched.
- Never change Gradle, Kotlin, Android SDK, or plugin versions casually.

## 12. Documentation

When adding or changing a feature, update relevant API documentation, a minimal usage example, platform compatibility notes where relevant, and the roadmap when priorities change.

Documentation must match actual code. Do not document planned features as implemented.
