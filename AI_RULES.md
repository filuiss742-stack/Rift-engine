# AI Rules for Rift Engine

These rules are mandatory for every AI agent and human contributor working on this repository.

## 1. Main goal

Rift Engine is an AI-first, beginner-friendly 2D Android game engine. Prefer a small, explicit, predictable API over clever abstractions or unnecessary features.

The engine must be easy for an AI to understand from the source, change safely, and verify. Do not optimize for maximum feature count.

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

## 3. Kotlin style

- Use Kotlin and the existing package structure.
- Use descriptive names: `player`, `sceneManager`, `deltaSeconds`.
- Keep functions short and focused.
- Prefer explicit types for public API and non-obvious values.
- Avoid clever one-line expressions when a normal block is easier to read.
- Avoid unnecessary generics, inheritance layers, DSLs, annotations, and operator overloading.
- Keep each main class responsible for one clear job.
- Comment public behavior and non-obvious decisions, not every line.

## 4. Engine architecture

Keep the architecture small and separated:

- `core/`: lifecycle, engine loop, scene management.
- `scene/`: scene base class and scene-level behavior.
- `entity/`: game objects and components.
- `graphics/`: drawing, camera, sprites and animation.
- `input/`: input state and touch controls.
- `physics/`: collision and movement helpers.
- `storage/`: save/load APIs.
- `plugin/`: optional extensions.
- `demo/`: examples and test scenes only.

Do not put game-specific story, characters, quests, or content into the engine core. Demo code must demonstrate the API, not become a hidden dependency of it.

## 5. Public API rules

- Prefer explicit calls such as `scene.add(player)`, `sceneManager.changeScene(nextScene)`, and `camera.follow(player)`.
- Use consistent units and document them. Positions and sizes are measured in logical pixels unless a future API explicitly says otherwise.
- Use seconds for time values; name them `deltaSeconds`.
- Make object ownership and lifecycle clear.
- Validate invalid inputs at API boundaries and return understandable errors where appropriate.
- Avoid methods that silently do nothing when the caller likely made a mistake.
- Keep defaults safe and documented.

## 6. Change protocol

Before editing:
1. Identify the exact files involved.
2. Read their current contents and callers.
3. State the intended change in one or two sentences.
4. Check whether the change can be made without breaking existing APIs.

After editing:
1. Review every changed file.
2. Search for affected call sites and update them.
3. Add or update a small example when a public API changes.
4. Run the available build and tests.
5. Report exactly what changed, what was tested, and what remains unverified.

If the requested change is ambiguous or would require a major architecture decision, ask before making a breaking change.

## 7. Testing and build safety

- The Android project must continue to build with the configured Gradle and Android SDK versions.
- Add tests for deterministic logic when practical.
- Keep demo scenes usable as manual smoke tests.
- Do not suppress compiler errors with broad `try/catch`, unchecked casts, or disabled lint/test settings.
- Never change Gradle, Kotlin, Android SDK, or plugin versions casually. Explain the compatibility reason first.

## 8. Documentation

When adding or changing a feature, update:
- the relevant API documentation;
- at least one minimal usage example;
- the roadmap if priorities changed.

Documentation must match the actual code. Do not document planned features as implemented.

## 9. Definition of done

A task is complete only when:
- the requested behavior is implemented;
- existing behavior remains intact;
- code and documentation agree;
- build/tests have been run when available;
- remaining limitations are stated honestly.

When unsure, choose the simpler implementation and explain the trade-off.
