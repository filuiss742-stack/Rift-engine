# AI Rules for Rift Engine

Rift Engine is an AI-first 2D engine targeting Android and Windows. One game may combine top-down, side-view, action and isometric modes while retaining shared state.

## Platform architecture
- `:engine` is pure Kotlin/JVM; no Android, AWT or Swing imports.
- `:platform-android` owns Android Canvas, SurfaceView, touch and SharedPreferences.
- `:platform-desktop` owns Windows windowing, AWT, keyboard/mouse and file saves.
- `:demo-game` contains shared game code; `:app` builds APK; `:desktop-app` is the Windows host.
- Dependencies flow toward core. Core APIs use engine-owned contracts, never platform types.
- Linux/macOS are not currently supported or packaged.

## Engineering rules
Inspect before editing. Make small changes. Do not invent APIs or silently break behavior. Explain new dependencies. Keep responsibilities focused and avoid circular dependencies. Use logical pixels and seconds. Prefer composition. Update docs/examples. Never claim tests pass unless run. Distinguish errors, warnings and hypotheses.

## Plugins
Every new or materially upgraded plugin needs docs covering purpose, behavior, setup, configuration, usage, API, dependencies, compatibility, limitations, troubleshooting and migration.

## Diagnostics
Keep bounded thread-safe history; report stable codes and useful context; detect known conflicts where practical; never claim complete bug detection or silently rewrite user code.

## Change protocol
Review changed files, update docs, run available tests/builds and report verified results. Do not merge PRs or change protected branches without owner approval.
