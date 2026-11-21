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
val recent = engine.diagnostics.snapshot()
val latestError = engine.diagnostics.latestError()

if (engine.diagnostics.hasFatalErrors()) {
    // The runtime pauses simulation until the fatal condition is addressed.
}
```

Ordinary ERROR entries remain visible but do not automatically stop the whole game. FATAL is reserved for conditions where continuing the runtime is unsafe or impossible.

## Failure isolation

- A GameComponent that throws during update or render is quarantined on its owning GameObject and reported once; other components continue.
- A GameObject update/render failure is reported and that object is quarantined; other objects continue. Call `scene.retryObject(object)` after correcting the cause to resume it.
- Individual UI element failures are quarantined so other widgets continue; use `ui.retryElement(element)` after correcting the cause. Physics and asset/audio failures are reported through the same diagnostic history where the subsystem can safely recover.
- The diagnostics overlay uses the normal RenderContext and does not depend on game assets.

A quarantined component remains attached but is skipped until it is removed and attached again. A quarantined GameObject is skipped for both update and render until `retryObject()` succeeds or the object is removed. This is deliberate: Rift reports the failure and prevents repeated exceptions, but does not silently rewrite or delete game code.

## Important limitations

Diagnostics are evidence, not proof that every bug is detected. Suggestions are best-effort guidance, not automatic fixes. The reporter coalesces repeated identical entries and keeps only a bounded history. It does not inspect arbitrary user code or guarantee recovery from corrupted state.

See the [API map](../api-map.md) and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
