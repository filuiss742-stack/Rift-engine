# Rift Kotlin Game Template

A game-owned Kotlin project demonstrating RiftGame, Scene, GameObject, named input actions, physics, screen-space UI, and multi-mode composition.

## Build

Requirements: JDK 17 and Gradle 8.9.

First publish the engine artifact to the local Maven repository from a Rift Engine checkout:

```shell
gradle :engine:publishToMavenLocal
```

Then, from this template directory:

```shell
gradle test
gradle build
```

The template consumes `com.rift.engine:engine:0.5.0-SNAPSHOT` as a Maven dependency. It does not include the engine source tree or depend on `:demo-game`. A future public distribution workflow will publish stable engine artifacts to a remote Maven repository; until then, the local Maven publish step is required.

## Project files

- `rift.project.json` — project metadata and declared targets.
- `GAME_AGENT.md` — safe instructions for an unfamiliar coding agent.
- `src/main/kotlin` — game-owned Kotlin code.
- `assets` — add project-owned images and audio here.

This is a shared game-logic starter, not yet a complete Android/Windows launcher project. See [the 0.5.0 release gates](../../docs/roadmap/0.5.0-release-gates.md) for remaining requirements.
