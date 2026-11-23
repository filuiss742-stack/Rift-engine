# Hybrid Prologue — Current Build and Acceptance Notes

Status: first 3D geometry slice wired into the Android prologue entry point. This is an experimental build for user testing, not a final art-complete prologue.

## Current sequence

1. Short rift/title opening with skip/tap support.
2. Modern apartment: move with WASD/arrows or touch joystick; interact with bed, desk, fridge, sink, phone and exit.
3. Street: separate world geometry, street props, lamps, trees and buildings; complete two generic errands and return home.
4. Return to apartment and interact with the bed.
5. Fantasy wake-up: ruined 3D environment and transition prompt.

Dialogue remains neutral placeholder text. The author must provide canonical dialogue before it is treated as final narrative.

## Rendering implementation

- The prologue now uses Software3DRenderer and actual world-space triangle meshes.
- Room floor, walls, bed, desk, fridge, sink, door, street buildings, lamps, trees and fantasy ruins are independent 3D cuboid/plane meshes with perspective projection.
- The heroine remains a 2D billboard sprite placed by projecting her world-space ground position.
- Filled triangle support is implemented by Android Canvas and desktop Java2D render contexts.
- Current renderer uses CPU projection and painter sorting. It does not yet have a per-pixel depth buffer, GPU acceleration, imported GLB models, UV texture sampling, normal maps, physically based materials, shadows, or occlusion-correct sprite depth. This is a real geometry prototype, not a finished modern 3D renderer.

## Art pipeline — hard requirements

- Reject the existing legacy heroine_idle.png as a final master if it fails native-resolution review.
- Each sprite source is one PNG, one character, one view, one pose/frame. Never generate contact sheets.
- Build the modern heroine master first; then author each direction and animation frame separately. Keep the fantasy alternate body separate.
- Every frame must be inspected at native resolution for anatomy, clothing consistency, crop, halo, and genuine alpha. Rejected files never enter runtime assets.
- Every texture is a separate source image for one material/map. No texture contact sheets or multi-material atlases as source art.
- Texture tiles must be high-resolution, seamless where tiling is expected, and independently checked for edge continuity, compression and mipmap quality.
- Mesh geometry and textures are separate validated assets. A front image does not define unseen 3D surfaces.

## Known limitations before calling the prologue finished

1. Replace flat-colour materials with individually approved high-resolution texture maps.
2. Replace legacy heroine sprite with approved transparent high-resolution directional sprite set.
3. Add animation playback and directional sprite selection.
4. Replace painter sorting with a depth-buffered backend and proper sprite/world occlusion.
5. Add actual 3D colliders/raycast interaction, robust navigation and door transitions.
6. Add authored dialogue, sound, save checkpoints and scene polish.
7. Test Android performance and input on a physical device; run Windows smoke test too.

Do not describe the current build as texture-complete or production-ready.
