# Rift Diagnostics

Rift Diagnostics is a platform-neutral runtime reporting layer and an evolving
foundation for configuration and dependency diagnostics.

## Purpose

It records known runtime failures and configuration conflicts, keeps a bounded
history, and can display a fallback screen through the shared render contract.
It does not claim to detect every possible bug and never rewrites project code.

## How it works

- `DiagnosticReporter` stores up to 100 recent entries by default.
- Entries contain severity, stable code, subsystem, message, optional cause,
  and the first available stack-frame location.
- Exact duplicate reports are coalesced.
- The reporter is thread-safe.
- Engine boundaries record caught exceptions.
- Plugin ID conflicts and blank IDs are reported by `PluginManager`.
- Recoverable errors use a compact overlay that leaves the game visible; only FATAL errors use a blocking diagnostics screen.
- Failing scene objects and UI elements can be quarantined and explicitly retried after the underlying issue is fixed.
- The overlay uses `RenderContext`; it does not depend on Android UI, AWT, game textures, or other platform-specific APIs.

## Severity

- `INFO`: informational event.
- `WARNING`: suspicious or conflicting configuration that may still run.
- `ERROR`: an operation failed. The event is visible, but does not by itself
  stop the engine. A failing optional plugin should not automatically stop the
  rest of the game.
- `FATAL`: the engine has explicitly been marked unable to continue safely.
  Fatal diagnostics pause further input dispatch, plugin updates, and scene
  updates until the diagnostic state is cleared.

The current implementation does not automatically promote every exception to
`FATAL`. Runtime exceptions caught at subsystem boundaries are reported as
`ERROR`; callers must explicitly report a fatal condition when continuing
would be unsafe.

## Usage

Read recent events:

```kotlin
val events = engine.diagnostics.snapshot()
val latestError = engine.diagnostics.latestError()
val hasErrors = engine.diagnostics.hasErrors()
val mustStop = engine.diagnostics.hasFatalErrors()
```

After the underlying cause has been fixed, a developer may clear diagnostics:

```kotlin
engine.diagnostics.clear()
```

Clearing diagnostics does not fix the underlying problem. Only clear after the
cause has been corrected.

## Current limitations

- It catches Kotlin/Java `Exception` failures at engine boundaries, not every
  possible JVM `Error`, native crash, or operating-system process failure.
- It cannot prove the cause of every visual or gameplay bug. It reports
  available evidence, not a guaranteed root cause.
- Static code analysis, asset validation, scene/camera validation, dependency
  graph validation, performance profiling, and automated repair suggestions
  are future work.
- A crash before a platform host is created cannot be displayed by the
  in-game overlay.

## Troubleshooting

1. Read the severity, code, subsystem, message, and location.
2. Treat warnings and hypotheses as leads, not confirmed causes.
3. Fix the underlying source or configuration issue.
4. Re-run the relevant tests and platform build.
5. Clear diagnostic history only after the cause has been fixed.

## Dependencies and compatibility

- Engine version: current development branch.
- Kotlin/JVM standard library only.
- No platform-specific or third-party dependency.
