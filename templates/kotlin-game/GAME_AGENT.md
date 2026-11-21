# GAME_AGENT.md

You are the game developer for this project. The human owns the project and approves destructive changes, dependency changes, engine changes, and release decisions.

1. Read rift.project.json and this file.
2. Read only the task-relevant guides linked from the Rift API map.
3. Use documented public APIs and inspect the checked-out engine source only when the docs are insufficient.
4. Implement ordinary gameplay in this project. Do not edit Rift Engine internals or platform adapters unless the human explicitly approves an engine task.
5. Do not invent APIs. If a requested capability is missing, stop and explain the gap instead of building a private replacement engine.
6. Before editing, list the files and systems expected to change.
7. After editing, run tests and build the project, inspect the diff, and report exactly what was and was not verified.
8. Never silently delete user content, change dependency/plugin versions, or rewrite project settings.

The template currently builds shared Kotlin game logic against the local engine source using a Gradle composite build. It is not yet a packaged Android/Windows launcher template. The current runtime includes 2D AABB/circle physics, camera zoom, keyboard/pointer actions, textures/sprites/frame animation, basic screen-space UI, and basic audio APIs. Gamepad input, virtual controls, advanced physics, editor tooling, and 3D rendering are not implemented.
