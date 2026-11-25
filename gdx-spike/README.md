# libGDX Integration Spike

This is an isolated experiment. It is **not** the production Rift Engine runtime and does not replace the current Android or Desktop applications.

## Purpose

Verify that one small Kotlin game module can render:
- a real 3D floor and prop;
- a camera-facing 2D billboard sprite;
- shared scene code on Android and Windows/Desktop.

The sprite is generated as a tiny placeholder in code only to test the billboard path. It is not heroine art and must not be used as a game asset.

## Build

From the repository root:

- `gradle :gdx-spike:core:test`
- `gradle :gdx-spike:desktop:build`
- `gradle :gdx-spike:android:assembleDebug`

Launch the desktop spike with:

`gradle :gdx-spike:desktop:run`

The Android debug APK is written to:

`gdx-spike/android/build/outputs/apk/debug/android-debug.apk`

## Scope limits

No migration of production gameplay, saves, combat, story, physics, or plugin APIs is performed here. No external physics engine is added. The current production runtime remains untouched while the spike is evaluated.
