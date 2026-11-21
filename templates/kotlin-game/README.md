# Rift Kotlin Game Template

A minimal independent game-code project demonstrating RiftGame, Scene, GameObject, named input actions, physics, and screen-space UI.

## Build

From this directory, with Gradle 8.9 and JDK 17 installed:

```shell
gradle test
gradle build
```

The template currently uses a Gradle composite build pointing to ../../engine so it can be validated inside the Rift repository without publishing artifacts. A copied template outside the repository needs the engine artifact published to a Maven repository; the public distribution workflow is still being completed.

## Project files

- rift.project.json — project metadata and declared targets.
- GAME_AGENT.md — safe instructions for an unfamiliar coding agent.
- src/main/kotlin — game-owned Kotlin code.
- assets — add project-owned images and audio here.

This is a shared game-logic starter, not yet a complete Android/Windows launcher project. See docs/roadmap/0.5.0-release-gates.md for the remaining release requirements.
