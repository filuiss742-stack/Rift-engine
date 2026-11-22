# Generated Sprite Workflow

This is the default asset-authoring workflow for Rift Engine and every game built with it.

## Core rule

**One image generation produces one PNG containing one character in one pose, or one frame of one animation.** Never generate a multi-pose character sheet as the primary source. The atlas is assembled later by deterministic tooling.

## Character approval sequence

1. Establish a character reference: front/side orientation, proportions, costume, palette, line/render style, and transparent canvas.
2. Approve one neutral standing pose first. This becomes the visual anchor for later images.
3. Generate each expression, pose, and animation frame separately. Reuse the approved reference every time. For a continuous animation, use the previous accepted frame as an additional reference where supported.
4. Review each PNG at native resolution and against the reference. Check anatomy, limb count, silhouette, costume details, lighting, transparency, and clipping.
5. Reject flawed images. Do not patch over major generation defects with atlas packing or code.

## File layout and naming

Keep source PNGs independent and preserve them after atlas generation.

```text
assets/
  characters/
    heroine_01/
      reference/
        heroine_01_neutral.png
      portraits/
        heroine_01_portrait_neutral.png
        heroine_01_portrait_happy.png
        heroine_01_portrait_worried.png
      idle/
        heroine_01_idle_000.png
        heroine_01_idle_001.png
      walk/
        heroine_01_walk_000.png
        heroine_01_walk_001.png
        heroine_01_walk_002.png
        heroine_01_walk_003.png
      interact/
        heroine_01_interact_000.png
```

Use zero-padded frame numbers. Do not use ambiguous names such as `final2.png` or `new_sprite.png`.

## Consistency requirements

- Use the same canvas width and height for every frame in an animation.
- Keep character scale, camera angle, facing direction, palette, rendering style, and costume stable.
- Keep a consistent pivot/ground-contact point. Record the pivot in metadata.
- Preserve transparent padding; never crop a limb or accessory to make a frame fit.
- Do not independently stretch frames to force them to match.
- Use separate files for different characters, outfits, and major visual variants.

## Atlas and animation metadata

Atlas packing is an import/build operation performed only after source frames are approved. It must not replace or delete source PNGs.

A future importer should emit a manifest equivalent to:

```json
{
  "schemaVersion": 1,
  "character": "heroine_01",
  "canvas": { "width": 128, "height": 192 },
  "pivot": { "x": 64, "y": 184 },
  "animations": {
    "idle": {
      "loop": true,
      "fps": 4,
      "frames": [
        { "file": "idle/heroine_01_idle_000.png", "durationMs": 250 },
        { "file": "idle/heroine_01_idle_001.png", "durationMs": 250 }
      ]
    }
  }
}
```

The actual atlas manifest may add packed texture coordinates, but must retain source-file identity, frame duration, loop state, canvas dimensions, and pivot.

## Validation checklist

An importer/CI validator should report:

- missing or duplicate frame numbers;
- inconsistent canvas dimensions within an animation;
- non-PNG or unreadable source files;
- unexpected fully opaque backgrounds when transparency is required;
- transparent pixels or cropping that cut into the subject unexpectedly;
- invalid frame durations, FPS, pivots, or atlas rectangles;
- duplicate source references or atlas regions outside texture bounds.

Diagnostics must distinguish confirmed failures from warnings. Never silently skip an invalid frame.

## Status

This document defines the required workflow. It does **not** claim that a fully automatic atlas importer, metadata generator, or art-quality validator is already implemented. Until those tools exist, agents must follow the workflow manually and report which checks were actually performed.
