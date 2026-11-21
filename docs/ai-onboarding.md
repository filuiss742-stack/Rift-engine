# AI Agent Onboarding

This is the short route for an AI agent with limited context. Read only the task-relevant subsystem guide after this page.

## 1. Read in this order

1. AGENTS.md — mandatory entry rules.
2. AI_RULES.md — architecture and change rules.
3. docs/architecture/ai-native-architecture.md — AI-native product principles and planned agent workflow.
4. docs/api-map.md — find the real public entry points.
5. The matching guide under docs/getting-started/.
6. The implementation and its callers only when the guide/API map is insufficient.

Do not load every document or every source file by default.

## 2. AI collaboration rules

- Treat the human as project owner and decision-maker; explain consequential trade-offs before irreversible changes.
- Use the same public APIs and validation rules as a human developer. Never bypass engine invariants for convenience.
- Inspect only the task-relevant context; do not ingest or rewrite the whole repository by default.
- Before editing, state the scope internally and identify the smallest affected file set. For broad changes, present a plan first.
- Keep changes reviewable and reversible. Show exact files changed and commands actually run.
- Never claim an operation, API, editor feature, or validation exists unless it is present in the current branch.
- Do not silently change unrelated project data. Ask before destructive or hard-to-reverse actions.

## 3. Choose the right module

| Task | Module |
|---|---|
| Shared runtime, scenes, entities, movement, narrative runtime, gameplay-module registry, diagnostics | :engine |
| Shared example gameplay and story content | :demo-game |
| Android lifecycle, Canvas, touch, SharedPreferences | :platform-android |
| Windows window, AWT, keyboard/mouse, file saves | :platform-desktop |
| Android application entry point and APK | :app |
| Windows application entry point and packaging | :desktop-app |

Keep dependencies flowing toward :engine. Never put Android, AWT, or Swing imports in it.

## 4. Before editing

1. Identify the smallest set of files needed.
2. Read the relevant API and current callers.
3. Confirm that the requested API exists. Do not invent methods or describe roadmap items as implemented.
4. Prefer a small change in a game module over a core change when the behavior is game-specific.
5. If a public API changes, update its guide and example in the same change.

## 5. After editing

Run the narrowest relevant checks first:

- Shared runtime/demo/desktop tests: gradle :engine:test :demo-game:test :platform-desktop:test
- Android lint and APK: gradle :platform-android:lintDebug :app:lintDebug :app:assembleDebug
- Windows distribution: gradle :desktop-app:installDist

The CI workflows run shared tests, platform checks, and the architecture-boundary script. Report commands that actually ran and their results. Do not claim a Windows package was manually launched unless it was.

## 6. Current capability boundaries

The runtime includes scenes, simple entities, platform-neutral basic rendering, key/touch forwarding, basic top-down and platformer movement, key/value saves, plugins, diagnostics, a replaceable gameplay-module registry, and a headless branching-dialogue runtime.

Gameplay modules declare stable IDs, versions, module dependencies, conflicts, and capabilities. They can be installed, replaced, and removed through GameEngine.gameplayModules. Module code must already be compiled into the application; dynamic downloading/loading and semantic version resolution are not implemented. See docs/getting-started/gameplay-modules.md.

The dialogue runtime does not include visual-novel presentation widgets, portrait/background management, typewriter effects, dialogue audio, a visual graph editor, or automatic story-state serialization. Games must implement presentation and persistence using existing game/UI and SaveStore facilities.

The engine also does not yet provide a collision world, automatic ground detection, action-based input mapping, a complete camera/render-layer pipeline, scene serialization, or a full asset/animation pipeline. A hybrid 2D/3D renderer is a long-term goal, not an existing feature.

When unsure, choose the simpler implementation and state what remains unsupported.
