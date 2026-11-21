# AI Rules for Rift Engine

These rules are mandatory for every AI agent and human contributor working on this repository.

## 1. Main goal

Rift Engine is an AI-first, beginner-friendly 2D Android game engine. Prefer a small, explicit, predictable API over clever abstractions or unnecessary features.

The engine must be easy for an AI to understand from the source, change safely, and verify. Do not optimize for maximum feature count. Its long-term direction is universal 2D composition: one game may combine top-down exploration, side-view platforming, action combat, and isometric presentation without duplicating shared game state.

## 2. Golden rules

1. Read this file and the relevant source files before changing code.
2. Make the smallest complete change that solves the requested task.
3. Do not rewrite, rename, move, or replace working systems unless the task explicitly requires it.
4. Do not invent APIs. Check the actual class and method names before using them.
5. Keep public APIs consistent. Prefer a few obvious methods with clear names.
6. Avoid hidden behavior, reflection, runtime code generation, global mutable state, and complex dependency injection.
7. Do not add a third-party dependency without explaining why it is needed and keeping the dependency footprint small.
8. Preserve existing behavior unless the task explicitly asks to change it.
9. If a change affects public API, update the relevant docs and examples in the same change.
10. Never claim that code builds or tests pass unless a build/test actually ran successfully.
11. Avoid spaghetti code: no tangled responsibilities, circular dependencies, unexplained shared state, or long methods that mix unrelated tasks.
12. Every new or upgraded plugin must ship with clear, current documentation and usage instructions.
13. Prefer universal, composable systems over genre-locked controllers or duplicated implementations.
14. Classify detected problems honestly as confirmed errors, warnings, or unverified hypotheses. Never present a guess as a proven cause.

## 3. Kotlin style and maintainability

- Use Kotlin and the existing package structure.
- Use descriptive names: `player`, `sceneManager`, `deltaSeconds`.
- Keep functions short and focused; extract a helper when it makes the flow clearer.
- Prefer explicit types for public API and non-obvious values.
- Avoid clever one-line expressions when a normal block is easier to read.
- Avoid unnecessary generics, inheritance layers, DSLs, annotations, and operator overloading.
- Keep each class and function responsible for one clear job (single responsibility).
- Separate input handling, game logic, rendering, persistence, and platform integration.
- Prefer composition over deep inheritance.
- Keep dependencies flowing in one clear direction. Core systems must not depend on demo code or game-specific features.
- Avoid circular dependencies between packages or modules.
- Do not duplicate logic; extract a small shared helper only when there is a real repeated use case.
- Do not create giant manager classes, god objects, or "utility" files that collect unrelated behavior.
- Comment public behavior and non-obvious decisions, not every line.

## 4. Engine architecture

Keep the architecture small and separated:

- `core/`: lifecycle, engine loop, scene management.
- `scene/`: scene base class and scene-level behavior.
- `entity/`: game objects and components.
- `graphics/`: drawing, camera, sprites and animation.
- `input/`: input state and touch controls.
- `physics/`: collision and movement helpers.
- `diagnostics/`: structured reports, runtime error history, and fallback UI.
- `storage/`: save/load APIs.
- `plugin/`: optional extensions.
- `demo/`: examples and test scenes only.

Keep shared game state separate from presentation, movement, physics, combat, and input profiles. A mode transition should change only the systems required for that mode; do not duplicate player progression or create separate engines for each genre. Avoid a giant controller full of mode flags. See `docs/architecture/engine-principles.md`.

Do not put game-specific story, characters, quests, or content into the engine core. Demo code must demonstrate the API, not become a hidden dependency of it.

## 5. Public API rules

- Prefer explicit calls such as `scene.add(player)`, `sceneManager.changeScene(nextScene)`, and `camera.follow(player)`.
- Use consistent units and document them. Positions and sizes are measured in logical pixels unless a future API explicitly says otherwise.
- Use seconds for time values; name them `deltaSeconds`.
- Make object ownership and lifecycle clear.
- Validate invalid inputs at API boundaries and return understandable errors where appropriate.
- Avoid methods that silently do nothing when the caller likely made a mistake.
- Keep defaults safe and documented.

## 6. Plugin requirements (mandatory)

Every new plugin and every materially upgraded plugin must have its own documentation file, normally `docs/plugins/<plugin-id>.md`. Do not consider the plugin complete until its documentation is updated in the same change.

Each plugin document must explain, in plain language:

1. **Purpose:** what the plugin does and when to use it.
2. **How it works:** its main classes, lifecycle, data flow, and how it interacts with the engine.
3. **Installation/activation:** how to add, register, enable, disable, and remove it.
4. **Configuration:** every supported setting, its default, valid values, and a short example.
5. **Usage instructions:** minimal step-by-step instructions and copyable Kotlin examples based on the real current API.
6. **Dependencies and compatibility:** required engine version, optional/required dependencies, and limitations.
7. **Events and public API:** exposed methods, callbacks, events, and expected behavior.
8. **Troubleshooting:** common mistakes, symptoms, and fixes.
9. **Change notes:** what changed when the plugin is upgraded, including migration steps if behavior or API changed.

Additional plugin rules:

- Keep plugins isolated from the engine core where practical; the core must not depend on optional plugins.
- Use stable, unique plugin IDs.
- Define clear ownership and cleanup behavior when a plugin is disabled or uninstalled.
- Do not silently change existing plugin behavior or public APIs.
- When upgrading a plugin, update its documentation and examples to match the new implementation; remove obsolete instructions.
- If a plugin's API changes, include a migration note and update all affected examples and call sites.
- Never describe planned or unsupported behavior as available.
- If the plugin is too small to need every section, keep the same headings and explicitly write "Not applicable" rather than leaving important ambiguity.

## 7. Change protocol

Before editing:
1. Identify the exact files involved.
2. Read their current contents and callers.
3. State the intended change in one or two sentences.
4. Check whether the change can be made without breaking existing APIs.

After editing:
1. Review every changed file.
2. Search for affected call sites and update them.
3. Add or update a small example when a public API changes.
4. Update plugin documentation whenever a plugin is added or materially changed.
5. Run the available build and tests.
6. Report exactly what changed, what was tested, and what remains unverified.

If the requested change is ambiguous or would require a major architecture decision, ask before making a breaking change.

## 8. Diagnostics requirements

Rift Diagnostics is a core engine capability, not an optional plugin.

- Report runtime exceptions at engine boundaries with a stable code, subsystem, readable message, and source location when available.
- Detect known configuration problems and identity/dependency conflicts as early as practical.
- Distinguish confirmed errors from warnings and hypotheses. Never invent a root cause.
- Keep a bounded, thread-safe recent-event history.
- When rendering fails after a caught exception, show a fallback diagnostics screen that does not depend on game assets.
- Never silently rewrite project code. Provide evidence and suggested next steps instead.
- Do not claim complete bug detection. Document what the current diagnostic layer cannot detect.
- Add focused tests for deterministic diagnostic logic when a test setup exists.

## 9. Testing and build safety

- The Android project must continue to build with the configured Gradle and Android SDK versions.
- Add tests for deterministic logic when practical.
- Keep demo scenes usable as manual smoke tests.
- Do not suppress compiler errors with broad `try/catch`, unchecked casts, or disabled lint/test settings.
- Never change Gradle, Kotlin, Android SDK, or plugin versions casually. Explain the compatibility reason first.

## 10. Documentation

When adding or changing a feature, update:
- the relevant API documentation;
- at least one minimal usage example;
- the roadmap if priorities changed.

Documentation must match the actual code. Do not document planned features as implemented.

## 11. Definition of done

A task is complete only when:
- the requested behavior is implemented;
- existing behavior remains intact;
- code and documentation agree;
- new or upgraded plugins have complete, current usage documentation;
- build/tests have been run when available;
- remaining limitations are stated honestly.

When unsure, choose the simpler implementation and explain the trade-off.
