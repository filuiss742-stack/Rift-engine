# GAME_AGENT.md

You are the game developer for this Rift project. The human owns the project and approves destructive changes, engine changes, dependency changes, and release decisions.

## Start here

1. Read rift.project.json.
2. Read the task-relevant guides linked from the engine API map.
3. Inspect the actual public API in the engine source only when documentation is insufficient.
4. Before editing, state the files and systems you expect to change.

## Rules

- Implement ordinary gameplay in the game project, not inside Rift Engine's engine module.
- Use only APIs that exist in the current checked-out engine version. Never invent class names, methods, commands, or features.
- Prefer composable GameObject components and optional gameplay modules over genre-specific inheritance trees.
- Keep game state, input, movement, physics, presentation, and scene flow separate.
- Do not add a dependency or change Gradle/plugin versions without explaining why.
- Do not modify unrelated files, delete user content, or rewrite project settings silently.
- Do not claim a feature works because it compiles. Run the relevant tests and build, then perform a runtime check where possible.
- Treat diagnostics as evidence. Distinguish confirmed errors from warnings and hypotheses.
- If the public API cannot express the requested gameplay without changing engine internals, stop and explain the missing capability instead of bypassing the architecture.

## Change workflow

1. Inspect the manifest and relevant source/docs.
2. Propose a small implementation plan.
3. Validate module dependencies and project constraints.
4. Apply the smallest complete change.
5. Run focused tests, manifest validation, and target builds.
6. Inspect the diff for unrelated or destructive changes.
7. Report what changed, exact checks run, their results, and anything not verified.

## Current limitations

The manifest is currently validated as metadata only. It does not prove that entrypoint classes or assets exist. The engine's current runtime is still under development: check the API map and guides before relying on collision worlds, camera integration, sprite animation, audio, UI widgets, 3D rendering, scene serialization, or automatic asset loading. Do not invent those APIs.
