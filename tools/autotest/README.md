# Rift AutoTest

This is a CI runtime smoke-test layer for the current Android and Windows launchers.

## Android emulator

The Android workflow builds the debug APK, boots an API 35 x86_64 emulator, installs and launches the app through an instrumentation test, leaves the game loop running for 12 seconds, and captures:

- Instrumentation test results and failure output.
- A screenshot from the running app.
- Logcat for crash and runtime diagnostics.
- `dumpsys gfxinfo` frame statistics and `dumpsys meminfo` for the app process.
- Activity-window frame duration samples (average FPS, p95 frame time, and frames above 16.7 ms) when Android exposes them.

The Activity-window frame metrics are not guaranteed to include every frame drawn by the custom SurfaceView/software renderer. The gfxinfo dump is retained as a second source, but emulator performance is not a substitute for a physical-device run.

## Windows app image

The Windows workflow launches the packaged `RiftEngine.exe`, verifies that the process remains alive for 15 seconds, records process CPU time and working-set memory, captures the desktop, and then closes the test process. A launch failure fails the workflow.

## Current coverage boundary

This first version verifies packaging, process startup, app survival, and visible rendering evidence. It does **not** yet play through the narrative or assert combat outcomes. The prologue currently has no stable automation/test-control interface for deterministic scene navigation; coordinate-only tapping would be brittle and could report false passes. A later gameplay suite should add explicit debug/test hooks or semantic UI identifiers before asserting apartment tasks, combat, dodge timing, enemy defeat, and rift completion.

CI emulator timings are diagnostic only. Final FPS/thermal acceptance still requires testing the APK on the target physical Android phone.
