# Screen-Space UI

Every Scene owns a UiCanvas. It is rendered after the camera-transformed world, so its coordinates remain in screen space and do not move when the camera scrolls or zooms.

## Add widgets

```kotlin
ui.add(UiPanel(12f, 12f, 220f, 72f))
ui.add(UiLabel(24f, 20f, 190f, 28f, "Health: 100", fontSize = 18f))

ui.add(
    UiButton(
        x = 24f,
        y = 90f,
        width = 160f,
        height = 48f,
        text = "Open inventory",
        onClick = { gameState.putBoolean("inventory.open", true) }
    )
)
```

Available widgets:
- UiPanel draws a rounded background.
- UiLabel draws a line of text.
- UiButton captures pointer presses and fires its callback only when released inside.
- UiImage draws a Sprite into the widget bounds.

Set zIndex to control draw order. Hit testing uses the reverse visual order so the topmost interactive widget receives the pointer. Pointer events not consumed by a UI widget continue to the Scene.onPointer callback.

## Touch and mouse

UiButton supports pointer IDs, so simultaneous touch contacts are tracked independently. Cancel events and widget removal clear pressed state. Android touch and desktop mouse input are routed through the same UI API.

## Current limitations

- No automatic layout engine, containers, responsive constraints, scrolling, clipping hierarchy, or skin/theme system.
- No keyboard/gamepad focus navigation or accessibility bridge.
- Labels are single-line and use the platform's default sans-serif font.
- UI is screen-space only; world-space labels should be rendered by GameObjects.
- Virtual joystick and virtual button controls are not included yet.

See the [input guide](input.md), [assets and animation](assets-and-animation.md), and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).


## Nested panels and layout

`UiPanel` can contain child widgets. Child coordinates are local to the panel. The panel clips child rendering and routes pointer events into the local coordinate space.

```kotlin
val menu = UiPanel(24f, 24f, 320f, 240f).apply {
    layout = UiLayout.VERTICAL
    padding = 16f
    gap = 10f
    add(UiLabel(0f, 0f, 280f, 24f, "Pause"))
    add(UiButton(0f, 0f, 280f, 44f, "Resume") { resumeGame() })
    add(UiButton(0f, 0f, 280f, 44f, "Settings") { openSettings() })
}
ui.add(menu)
```

Available layouts are `ABSOLUTE`, `VERTICAL`, and `HORIZONTAL`. Vertical and horizontal layouts use each child's requested size, panel padding, and gap; they do not resize children automatically.

## Keyboard focus

Buttons are focusable by default. TAB cycles through visible, enabled buttons; ENTER or SPACE activates the focused button. Focus is visible through a highlight outline. Keyboard events consumed by the UI are not forwarded to gameplay action bindings.
