# Rift Engine

Rift Engine is a modular Kotlin 2D game engine targeting **Android (APK)** and **Windows (desktop EXE application image)**. Linux and macOS are not current targets.

The same game logic, scenes, entities, movement profiles, plugins and save API are shared. Platform modules supply rendering, input, persistence and application hosting. The architecture is intended for mixed-style games combining top-down exploration, side-view platforming, action combat and isometric presentation without duplicating game state.

## Modules

- `:engine`: pure Kotlin/JVM runtime and platform-neutral contracts.
- `:platform-android`: Android SurfaceView/Canvas, touch input and SharedPreferences.
- `:platform-desktop`: Windows Swing/AWT, keyboard/mouse and file saves.
- `:demo-game`: shared demo scene.
- `:app`: Android APK host.
- `:desktop-app`: Windows desktop host.

The core must not import Android, AWT or Swing. Platform adapters depend on the core, never the other way around.

## Build

Android: `gradle :app:assembleDebug` (JDK 17, Android SDK 35, Gradle 8.9).
Desktop: `gradle :desktop-app:run` (JDK 17).
Windows packaging will use `jpackage` to bundle a Java runtime and an EXE launcher; this is not native compilation or a single-file executable.

## License

MIT. See [LICENSE](LICENSE).
