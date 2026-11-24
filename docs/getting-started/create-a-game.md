# Create a Game with Rift

A game project supplies a `RiftGame` implementation. The platform host owns platform lifecycle, rendering, input forwarding, storage, and the `GameEngine` instance. The game owns its scenes and gameplay content.

## 1. Implement the game entrypoint

```kotlin
import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

class MyGame : RiftGame {
    override fun createInitialScene(): Scene = WorldScene()
}
```

Create the first scene inside the game project. Do not put game-specific scene construction in a platform adapter.

## 2. Select the game in the application composition root

The Android or Windows application chooses which game to launch:

```kotlin
RiftSurfaceView(context, MyGame())
```

For Windows:

```kotlin
DesktopGameWindow(engine, MyGame())
```

The platform host asks the supplied game for its initial scene and installs that scene in the runtime. The host does not need to import or know the concrete scene class.

## 3. Keep ownership clear

- `:engine` defines the platform-neutral `RiftGame` entrypoint contract and runtime.
- `:platform-android` and `:platform-desktop` implement platform hosting.
- Your game module implements `RiftGame` and owns game-specific scenes, assets, and rules.
- The application module selects the game implementation to launch.

The current `:demo-game` module is an example game, not a required dependency for third-party games. A standalone external game template and machine-readable project manifest are planned follow-up work; they are not included yet.
