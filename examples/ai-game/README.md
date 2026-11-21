# AI Game Example

This folder contains a sample version-1 project manifest, a concise GAME_AGENT.md, and minimal Kotlin source demonstrating the RiftGame entrypoint, Scene, GameObject, and named keyboard actions.

It is a documentation and validator fixture at this stage, not yet a standalone copy-and-build template. The standalone template and target-specific Gradle setup are separate 1.0 release gates.

Validate the manifest from the repository root:

```shell
gradle :tools:project-validator:run --args="examples/ai-game/rift.project.json"
```

The validator checks manifest metadata and declared module relationships. It does not compile the sample source or prove that the game runs on Android or Windows.
