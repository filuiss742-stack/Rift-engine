# Rift Diagnostics

GameEngine owns a bounded, thread-safe DiagnosticReporter. Systems report structured entries instead of silently swallowing errors.

Each DiagnosticEntry contains:
- severity: INFO, WARNING, ERROR, or FATAL;
- stable diagnostic code and subsystem;
- concise message;
- optional cause and source location;
- optional suggested next step.

## Read diagnostics

```kotlin
val recent = diagnostics.snapshot()
val latestError = diagnostics.latestError()

if (diagnostics.hasFatalErrors()) {
    // The runtime pauses simulation until the fatal condition is addressed.
}
```

Ordinary ERROR entries remain visible but do not automatically stop the whole game. FATAL is reserved for conditions where continuing the runtime is unsafe or impossible.

## Failure isolation

- A GameComponent that throws during update or render is quarantined on its owning GameObject and reported once; other components continue.
- A GameObject update/render failure is reported by its Scene; other objects continue.
- UI, physics, and asset/audio failures are reported through the same diagnostic history where the subsystem can safely recover.
- The diagnostics overlay uses the normal RenderContext and does not depend on game assets.

A quarantined component remains attached but is skipped until it is removed and attached again. This is deliberate: Rift reports the failure and prevents repeated exceptions, but does not silently rewrite or delete game code.

## Important limitations

Diagnostics are evidence, not proof that every bug is detected. Suggestions are best-effort guidance, not automatic fixes. The reporter coalesces repeated identical entries and keeps only a bounded history. It does not inspect arbitrary user code or guarantee recovery from corrupted state.

See the [API map](../api-map.md) and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
