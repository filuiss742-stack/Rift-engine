# Android and Windows Platform Architecture

Supported release targets:
- Android APK: touch, Android Canvas and SharedPreferences.
- Windows desktop app: keyboard/mouse, AWT rendering and file-backed saves.

Linux and macOS are not current targets. The shared JVM core may technically run elsewhere, but those platforms are not promised, tested or packaged.

Dependency direction: game hosts depend on platform adapters and shared game code; adapters depend on `:engine`; the core never imports Android, AWT or Swing.

Shared contracts include `RenderContext` for drawing and `SaveStore` for persistence. Scenes, entities, movement, plugins, diagnostics and fixed-step timing remain in the shared runtime. Android owns SurfaceView lifecycle and touch translation. Windows owns the desktop window and keyboard/mouse translation.

Windows packaging is planned with `jpackage`, producing an application image with a bundled JVM and an EXE launcher, not a native-compiled single-file executable.
