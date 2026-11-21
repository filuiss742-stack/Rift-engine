# AI Agent Onboarding

This is the short route for an AI agent with limited context. Read only the task-relevant subsystem guide after this page.

## 1. Read in this order

1. AGENTS.md — mandatory entry rules.
2. AI_RULES.md — architecture and change rules.
3. docs/api-map.md — find the real public entry points.
4. The matching guide under docs/getting-started/.
5. The implementation and its callers only when the guide/API map is insufficient.

Do not load every document or every source file by default.

## 2. Choose the right module

| Task | Module |
|---|---|
| Shared runtime, scenes, entities, movement, narrative runtime, diagnostics, plugin contracts | :engine |
| Shared example gameplay and story content | :demo-game |
| Android lifecycle, Canvas, touch, SharedPreferences | :platform-android |
| Windows window, AWT, keyboard/mouse, file saves | :platform-desktop |
| Android application entry point and APK | :app |
| Windows application entry point and packaging | :desktop-app |

Keep dependencies flowing toward :engine. Never put Android, AWT, or Swing imports in it.

## 3. Before editing

1. Identify the smallest set of files needed.
2. Read the relevant API and current callers.
3. Confirm that the requested API exists. Do not invent methods or describe roadmap items as implemented.
4. Prefer a small change in a game module over a core change when the behavior is game-specific.
5. If a public API changes, update its guide and example in the same change.

## 4. After editing

Run the narrowest relevant checks first:

- Shared runtime/demo/desktop tests: gradle :engine:test :demo-game:test :platform-desktop:test
- Android lint and APK: gradle :platform-android:lintDebug :app:lintDebug :app:assembleDebug
- Windows distribution: gradle :desktop-app:installDist

The CI workflows run shared tests, platform checks, and the architecture-boundary script. Report commands that actually ran and their results. Do not claim a Windows package was manually launched unless it was.

## 5. Current capability boundaries

The foundation includes scenes, simple entities, platform-neutral rendering, key/touch forwarding, basic top-down and platformer movement, key/value saves, plugins, diagnostics, and a headless branching-dialogue runtime with story flags, integer values, choice conditions, and choice effects.

The dialogue runtime does not include visual-novel presentation widgets, portrait/background management, typewriter effects, dialogue audio, a visual graph editor, or automatic story-state serialization. Games must implement presentation and persistence using existing game/UI and SaveStore facilities.

The engine also does not yet provide a collision world, automatic ground detection, action-based input mapping, a complete camera/render-layer pipeline, scene serialization, or a full asset/animation pipeline.

When unsure, choose the simpler implementation and state what remains unsupported.
