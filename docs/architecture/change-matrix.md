# Change Matrix

Use this table to choose the smallest correct module and validation set.

| Change | Module | Minimum local validation |
|---|---|---|
| Runtime, scene lifecycle, entity, movement contract, diagnostics | `:engine` | `gradle :engine:test` |
| Shared demo/gameplay example | `:demo-game` | `gradle :demo-game:test` and shared JVM tests |
| Android lifecycle, touch, Canvas, Android storage | `:platform-android` | `gradle :platform-android:lintDebug :app:lintDebug :app:assembleDebug` |
| Windows window, keyboard/mouse, AWT, file storage | `:platform-desktop` | `gradle :platform-desktop:test :desktop-app:installDist` |
| Android app entry point or packaging | `:app` | `gradle :app:assembleDebug` |
| Windows app entry point or packaging | `:desktop-app` | `gradle :desktop-app:installDist` |
| Shared API used by both platforms | Relevant shared module | Shared JVM tests plus Android and Windows workflows |

## Always-on CI checks

Both platform workflows run:

- `python scripts/check_engine_boundaries.py` — rejects Android, AWT, or Swing imports in `:engine`.
- `gradle :engine:test :demo-game:test :platform-desktop:test` — shared JVM and desktop tests.

The Android workflow additionally runs lint and builds the debug APK. The Windows workflow builds the desktop distribution and packages a Windows application image.

A successful package build does not prove the packaged app was manually launched. State that distinction in reports.
