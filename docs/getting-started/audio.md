# Audio

GameEngine owns one AudioManager and exposes it to active scenes through Scene.audio. The host provides the platform-specific AudioLoader.

## Load and play sound effects

```kotlin
val audioManager = requireNotNull(audio)

audioManager.loadSound("ui.click", "audio/click.wav")
audioManager.playSound("ui.click")
```

## Background music

```kotlin
audioManager.loadMusic("music.town", "music/town.wav")
audioManager.playMusic("music.town", looping = true)

// Later:
audioManager.pauseMusic()
audioManager.resumeMusic()
audioManager.stopMusic()
```

Music and sound IDs use the same stable lowercase naming rules as texture IDs. Paths must be relative to the host's configured asset root and cannot contain parent-directory traversal.

## Volume and ownership

- setMusicVolume(value) changes the volume of all loaded music resources.
- setSoundVolume(id, value) changes one sound resource.
- unloadSound/unloadMusic release a single resource.
- GameEngine.close() releases all audio and texture resources.

Volume values are normalized from 0.0 (silent) to 1.0 (full volume).

## Current limitations

- Loading and decoding are synchronous; load resources during scene setup/loading screens, not during latency-sensitive gameplay.
- Android currently uses MediaPlayer for both effects and music. This is functional but not a low-latency pooled sound-effect backend.
- The desktop backend currently supports formats available through Java Sound; WAV/PCM is the recommended shared format.
- No audio buses, spatial audio, mixer graph, cross-fades, streaming decoder abstraction, or per-scene automatic music policy yet.
- Device smoke tests with real speakers/headphones are still required.

See the [assets and animation guide](assets-and-animation.md) and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
