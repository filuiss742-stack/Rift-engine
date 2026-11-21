# Rift AI Game Acceptance Project

This is an independent Kotlin game project used to exercise Rift's public APIs from outside the engine source tree. It is deliberately small, but it is a playable top-down action prototype rather than a documentation-only fixture.

## Acceptance coverage

- Keyboard movement with named actions.
- Virtual joystick and multi-touch action buttons.
- A physics-controlled player and a camera follow rig.
- A pursuing enemy with 30 health.
- A short-range real-time attack that defeats the enemy.
- A coin dropped on defeat and collected by the player.
- A screen-space HUD.
- Automated tests for movement, enemy pursuit, combat, dodge, touch input, and pickup collection.

## Build and test

Requirements: JDK 17 and Gradle 8.9.

From a Rift Engine checkout, publish the engine artifact to Maven Local:

```shell
gradle :engine:publishToMavenLocal
```

Then, from this directory:

```shell
gradle test
gradle build
```

The project consumes `com.rift.engine:engine:0.5.0-SNAPSHOT` from Maven Local. It does not include engine source code or depend on `:demo-game`.

The acceptance tests validate gameplay logic through the public Scene, GameObject, input, physics, combat, camera, and UI APIs. They do not replace manual runtime smoke tests on an Android device or a packaged Windows application.

The manifest validator checks project metadata and declared paths. It does not prove runtime behavior by itself.
