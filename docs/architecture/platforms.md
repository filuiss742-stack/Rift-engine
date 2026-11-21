# Android and Windows Platform Architecture

## Supported release targets

- Android APK: touch input, Android Canvas rendering and SharedPreferences.
- Windows desktop application: keyboard/mouse input, AWT rendering and file-backed saves.

Linux and macOS are not current targets. The shared JVM core may technically run elsewhere, but those platforms are not promised, tested or packaged.

## Dependency direction

Application hosts depend on platform adapters and shared game code. Platform adapters depend on `:engine`. The core never imports Android, AWT, Swing, or host-specific APIs.

## Shared contracts

- `RenderContext` defines platform-neutral drawing operations.
- `SaveStore` defines platform-neutral key/value persistence.
- Scenes, entities, movement profiles, plugins, diagnostics and fixed-step timing live in the shared runtime.

Android owns SurfaceView lifecycle and touch translation. Windows owns desktop windowing, AWT rendering and keyboard/mouse translation.

## Windows packaging

The Windows workflow uses `jpackage` to produce an application image with a bundled JVM and an EXE launcher. This is not native compilation and not a single-file executable. A successful package build confirms packaging; a manual launch and input smoke test are still required before calling the desktop host manually verified.

## Current desktop-host limitation

The initial Windows host uses a Swing timer for its fixed-step update and Swing painting for rendering. This is intentionally simple for the current demo. Before adding expensive simulation or large scenes, move simulation to a dedicated game thread and introduce a safe render-state handoff so UI events and rendering remain responsive.
