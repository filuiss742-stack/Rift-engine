# Rift Diagnostics

Rift Diagnostics is the first runtime safety and error-reporting layer in Rift Engine.

## Purpose

It replaces an unexplained empty/black game view after a caught engine exception with a readable diagnostic screen. It also keeps a short, thread-safe history of recent events for developers and AI coding agents.

## How it works

- `DiagnosticReporter` stores up to 100 recent entries.
- Each entry contains severity, stable error code, subsystem, message, optional cause, and first stack-frame location.
- Exact duplicate reports are not added repeatedly.
- `GameEngine` records exceptions from update, render, and input operations.
- When an error exists, `DiagnosticsOverlay` draws a fallback screen using Android Canvas only; it does not depend on game scenes, textures, fonts, or other game assets.
- Plugin ID conflicts and invalid blank IDs are reported by `PluginManager`.
- Diagnostics never edits or repairs project source code automatically.

## Usage

Read recent events:

```kotlin
val events = engine.diagnostics.snapshot()
val latestError = engine.diagnostics.latestError()
val hasErrors = engine.diagnostics.hasErrors()
```

After fixing the cause, a developer may clear the current diagnostic state:

```kotlin
engine.diagnostics.clear()
```

Clearing diagnostics does not fix the underlying problem. Only clear after the cause has been corrected.

## Severity

- `INFO`: informational event.
- `WARNING`: suspicious or conflicting configuration that may still run.
- `ERROR`: an operation failed and normal game rendering is paused behind the diagnostic screen.

## Current limitations

- It catches Kotlin/Java `Exception` failures at engine boundaries, not every possible JVM `Error` or Android process failure.
- It cannot prove the cause of every visual or gameplay bug. It reports the exception and its first stack frame when available.
- Static code analysis, asset validation, scene/camera validation, performance profiling, and automated repair suggestions are future work.
- A crash that occurs before the Android Activity or SurfaceView can be created cannot be shown by this in-game overlay.

## Troubleshooting

1. Read the error code, subsystem, message, and location.
2. Fix the reported source issue.
3. Re-run the project and its tests.
4. Call `engine.diagnostics.clear()` only after the cause has been fixed.

## Dependencies and compatibility

- Engine version: current development branch (0.1.x).
- Android Canvas and Kotlin standard library only.
- No third-party dependency.
