# Generated Sprite Workflow

This is the default asset-authoring workflow for Rift Engine and every game built with it.

## Asset separation

**Every asset family is produced and stored separately.** A character atlas contains only one character. It must never contain map art, locations, 3D props, UI, portraits, palettes, labels, or another character.

- Character sprites: one atlas per character and outfit/major visual variant.
- Character portraits: separate portrait files/portrait atlas, never mixed into the full-body animation atlas.
- Locations and maps: real 3D scene geometry and scene data, not character sheets or flat background images.
- Props and interactable objects: separate 3D models, collision/interaction data, and materials.
- Materials/textures: separate texture assets per material and map type (base color, normal, roughness, metallic, AO, etc.).

## Character atlas output

The runtime deliverable is **one PNG atlas per character** containing the distinct frames needed for that character's basic actions, plus a separate metadata file. Do not make one tiny atlas per action, and do not put multiple characters into one atlas.

The atlas should include a compact, useful starter set rather than dozens of redundant poses:

- idle: 2–4 visibly different frames;
- walk: 4–6 frames with clear changes in leg/arm position;
- run: 4–6 frames with a distinct faster stride;
- attack: 2–4 frames covering anticipation, action, and recovery;
- dodge: 2–3 frames;
- hit/react: 1–2 frames;
- interact: 1–2 frames.

Only include actions that the character actually needs. Do not pad the atlas with repeated standing poses, duplicate frames, portraits, emotion icons, palette swatches, or decorative symbols.

Each cell must be large enough to preserve readable anatomy and costume detail at the intended in-game resolution. Prefer a consistent cell size based on the approved source frames; never shrink the character merely to fit more cells. Keep transparent gutters between packed frames and preserve alpha.

## Source-frame production and QA

The atlas is assembled from separately generated and individually approved source PNGs. This is a quality-control step, not the final runtime layout.

1. Establish an approved character reference: proportions, costume, palette, line/render style, view direction, and transparent canvas.
2. Approve one neutral standing pose as the visual anchor.
3. Generate each required action frame as its own PNG, reusing the approved reference. Use previous accepted frames as references to preserve identity and proportions.
4. Inspect every source PNG at native resolution. Reject anatomy defects, costume drift, incorrect direction, inconsistent scale, halo/background residue, clipping, or poor transparency.
5. Compare adjacent animation frames. Each frame must make a meaningful motion change; near-identical duplicates are rejected.
6. Only after all frames pass, pack them into the single per-character atlas PNG and emit metadata. Preserve all source PNGs.

Never ask an image generator to create a giant mixed character/art presentation board and treat it as a runtime atlas. If a generated sheet contains extra portraits, icons, labels, props, multiple characters, inconsistent cell sizes, or tiny figures, reject it.

## File layout and naming

Keep source PNGs independent and preserve them after atlas generation.

```text
assets/
  characters/
    heroine_01/
      reference/
        heroine_01_neutral.png
      source/
        idle/heroine_01_idle_000.png
        idle/heroine_01_idle_001.png
        walk/heroine_01_walk_000.png
        walk/heroine_01_walk_001.png
        run/heroine_01_run_000.png
        attack/heroine_01_attack_000.png
        dodge/heroine_01_dodge_000.png
        hit/heroine_01_hit_000.png
        interact/heroine_01_interact_000.png
      portraits/
        heroine_01_portrait_neutral.png
      atlas/
        heroine_01_atlas.png
        heroine_01_atlas.json
  props/
  locations/
  materials/
```

Use zero-padded frame numbers. Do not use ambiguous names such as `final2.png` or `new_sprite.png`.

## Consistency requirements

- Use the same canvas width and height for all frames in a character animation set.
- Keep character scale, camera angle, facing direction, palette, rendering style, and costume stable.
- Keep a consistent pivot/ground-contact point. Record the pivot in metadata.
- Preserve transparent padding; never crop a limb or accessory to make a frame fit.
- Do not independently stretch frames to force them to match.
- Use separate atlases for different characters, outfits, and major visual variants.
- Keep directional views as explicit animation/view variants; do not rotate one flat front-facing sprite and pretend it represents the back or side.

## Atlas and animation metadata

Atlas packing is a deterministic import/build operation performed only after source frames are approved. It must not replace or delete source PNGs.

A future importer should emit a manifest equivalent to:

```json
{
  "schemaVersion": 1,
  "character": "heroine_01",
  "atlas": "atlas/heroine_01_atlas.png",
  "canvas": { "width": 512, "height": 512 },
  "pivot": { "x": 256, "y": 480 },
  "animations": {
    "idle": {
      "loop": true,
      "fps": 4,
      "frames": [
        { "source": "source/idle/heroine_01_idle_000.png", "rect": [0, 0, 128, 192], "durationMs": 250 },
        { "source": "source/idle/heroine_01_idle_001.png", "rect": [128, 0, 128, 192], "durationMs": 250 }
      ]
    }
  }
}
```

The actual atlas manifest must retain source-file identity, packed texture coordinates, frame duration, loop state, canvas dimensions, pivot, action name, and view direction where applicable.

## Validation checklist

An importer/CI validator should report:

- missing or duplicate frame numbers;
- near-duplicate frames within an action;
- inconsistent canvas dimensions or character scale;
- non-PNG or unreadable source files;
- unexpected opaque backgrounds when transparency is required;
- halo residue, clipping, or transparent pixels cutting into the subject;
- invalid frame durations, FPS, pivots, or atlas rectangles;
- mixed character/asset families in one atlas;
- cells too small for the configured runtime sprite resolution;
- duplicate source references or atlas regions outside texture bounds.

Diagnostics must distinguish confirmed failures from warnings. Never silently skip an invalid frame.

## Status

This document defines the required workflow. It does **not** claim that a fully automatic atlas importer, metadata generator, or art-quality validator is already implemented. Until those tools exist, agents must follow the workflow manually and report which checks were actually performed.
