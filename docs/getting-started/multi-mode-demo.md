# Multi-Mode Gameplay Demo

Rift's shared demo now contains two scenes that use different movement and physics configurations while sharing one `GameState`.

## Try it

- Start in top-down mode. Move with WASD or the arrow keys.
- Press TAB to switch to side-view platforming.
- Move with A/D or the arrow keys; jump with SPACE, W, or UP.
- Press TAB again to return to top-down mode.

The side-view scene reads the same `mode-switches` progression value written by the top-down scene. The state belongs to the `SceneManager`, not either individual scene, so it survives the mode transition.

## How it works

Each scene owns its own:
- movement profile and input bindings;
- physics world and body configuration;
- camera and screen-space UI;
- scene-specific objects and presentation.

Both scenes use the same `GameObject`, `PhysicsWorld`, `InputActionMap`, and `GameState` APIs. A scene requests a transition with:

```kotlin
requestSceneChange(PlatformerDemoScene())
```

The manager queues the request and applies it after the current callback completes. It attaches the destination scene to the same shared state and platform services. Held keys are synchronized without generating fake just-pressed events in the new scene.

This is a small proof of the mode-composition architecture, not a full editor or a complete action-RPG kit. The demo does not yet include virtual touch controls, a combat framework, or a full tilemap renderer.
