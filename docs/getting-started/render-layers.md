# 2D Render Layers

Every `GameObject` has an integer `renderLayer`. Lower values are drawn first; higher values appear in front. Objects with the same layer preserve the order in which they were added to the scene.

```kotlin
background.renderLayer = -100
worldDecoration.renderLayer = 0
player.renderLayer = 10
foregroundEffects.renderLayer = 20
```

Render layers affect drawing only. They do not change update order, physics, collision filtering, or input priority. Scene-space objects are drawn through the camera; the scene's `UiCanvas` is drawn afterward in screen space.

This is a simple ordered layer contract, not yet a full render graph with cameras per layer, blend modes, post-processing, or GPU batching.
