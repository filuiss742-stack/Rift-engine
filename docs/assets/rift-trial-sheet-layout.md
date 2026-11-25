# Rift Trial Sprite Atlas Layout

The current trial atlas is a single-character-family pixel-art sheet at:

- Android: `app/src/main/assets/sprites/rift_trial_sheet.png`
- Desktop: `desktop-app/src/main/resources/sprites/rift_trial_sheet.png`

## Grid

- Canvas: 256 × 128 pixels
- Grid: 4 columns × 2 rows
- Cell size: 64 × 64 pixels
- Frame order: row-major, left to right

| Atlas indices | Row | Current use |
|---|---|---|
| 0–3 | Top | Legacy humanoid trial frames; not used by the prologue heroine |
| 4–7 | Bottom | Prologue enemy / distorted creature frames |

The prologue selects indices 4–7 and resolves them through the engine's validated `spriteAtlasFrameUv` helper. The monster is rendered as a camera-facing 2D sprite in the 3D ruin scene. Its position, collision/attack logic, and telegraph remain world-space gameplay data; the art is never converted into a 3D character mesh.

## Limitations

This is a temporary combat creature asset, not the final monster design. The four lower cells provide a short loop but do not yet represent a complete action set (idle, anticipation, attack, recovery, hit, defeat) or eight independently authored views. Do not treat this atlas as production-complete character art.

Future character art must follow [the generated sprite workflow](generated-sprite-workflow.md): each source frame is created and reviewed independently, and only approved frames are packed into an atlas.
