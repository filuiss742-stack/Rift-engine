# Directional Heroine Runtime Asset Contract

The prologue loads independently authored heroine idle views from these project-relative paths:

```text
prologue/characters/heroine/source/<direction>/idle/heroine_<direction>_idle_000.png
```

The eight directory and filename direction tokens are:

`front`, `front_left`, `left`, `back_left`, `back`, `back_right`, `right`, `front_right`.

Each path is loaded independently through the project asset manager. Missing directions are not mirrored, rotated, or filled with duplicate copies. The renderer may use the explicit front fallback when a camera-facing direction is not yet authored; the runtime animation result marks that fallback so callers can surface diagnostics.

The old `prologue/characters/heroine/heroine_idle.png` is retained only as a temporary front-view fallback. It is not treated as an eight-direction production set.

## Current limitation

This integration loads one idle frame per available direction. It does not yet consume the generated atlas JSON manifest or load complete action clips. The atlas builder and this runtime contract are intentionally separate until a versioned manifest reader is implemented and covered by engine tests. Do not describe the heroine as fully animated or eight-direction complete until all eight independently authored views and the required action clips are present.
