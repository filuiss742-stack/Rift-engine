# Hybrid Prologue — Current Build and Acceptance Notes

Status: playable narrative pass wired into the experimental 3D prologue. This is a story/gameplay prototype for testing, not a final art-complete prologue.

## Part I — Prologue: "The Crack"

1. Black screen and a thin red fracture.
2. The crack opens; a hand reaches through and tears itself free. The figure is identified only as Teia in production narrative notes; no explanation is given.
3. Hard cut to black.
4. Liza wakes on Earth.

## Act I — "A Strange Dream"

### Earth: Liza's ordinary day

- Liza wakes, complains about the alarm and starts her day.
- Phone messages with Anya establish a casual, teasing friendship.
- Breakfast interaction gives Liza a mundane, slightly self-deprecating inner voice.
- The television reports that a research institute has opened a "neutral space". The broadcast says the region does not belong to any known physical area, that researchers are studying it, and that civilians should stay away from the laboratory complex.
- Liza reacts skeptically rather than immediately treating the news as personally important.
- She goes outside, visits a bus stop and a shop, receives a message from Anya, returns home and goes to bed.

### The dream: Mira's body at the ruins

- Liza wakes beside a campfire among the ruins. She remembers going to sleep but does not understand how she arrived.
- Movement tutorial: walk away from the campfire and orient in the ruined space.
- First encounter: a distorted creature emerges from behind a broken column.
- Combat tutorial: close-range attack, visible attack telegraph, and timed dodge.
- First glitch: after the creature is defeated, the image briefly tears red and the words "SIGNAL DOES NOT MATCH" appear. Liza notices that the world seemed to become incorrect for a moment.
- The episode ends after Liza examines the new fracture. No explanation of the connection between Teia, Liza, the neutral space, or Mira is revealed.

## Dialogue tone

Liza is dry, observant and mildly sarcastic in ordinary situations. Her reactions become shorter and more serious after waking in the ruins. Anya's messages are warm but teasing. The newsreader stays factual and does not deliver an exposition dump. The fantasy scene does not introduce a lore lecturer or explain the mystery prematurely.

All dialogue in the current build is authored prototype copy and can be revised by the game's author. Names and casual details such as Anya are provisional story dressing, not immutable world canon.

## Controls

- Earth: WASD / arrows or virtual joystick to move; E / Enter or USE to interact.
- Fantasy: WASD / arrows or virtual joystick to move; Space / HIT to attack; Shift / DODGE to evade; E / USE to inspect the fracture.
- Dialogue: E / Enter / Space or tap to advance.

## Rendering implementation

- The prologue uses Software3DRenderer and actual world-space triangle meshes.
- Room floor, walls, bed, desk, fridge, sink, door, street buildings, lamps, trees and fantasy ruins are independent 3D cuboid/plane meshes with perspective projection.
- The heroine remains a 2D billboard sprite placed by projecting her world-space ground position.
- Filled triangle support is implemented by Android Canvas and desktop Java2D render contexts.
- The current scene still uses flat-color surfaces. It is not yet connected to the individually generated high-resolution materials requested for production.
- Current renderer uses CPU projection and painter sorting. It does not yet have a per-pixel depth buffer, GPU acceleration, imported GLB models, normal-map lighting, physically based materials, shadows, or occlusion-correct sprite depth.

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
6. Replace the current primitive fantasy creature with an approved directional sprite/animation set and implement proper hit feedback/health/damage.
7. Add authored sound, save checkpoints and scene polish.
8. Test Android performance and input on a physical device; run Windows smoke test too.

Do not describe the current build as texture-complete or production-ready.


## Authoritative playable script

The current authored dialogue and scene-by-scene staging for Part I Prologue — “The Crack” and Act I — “A Strange Dream” are maintained in [`docs/game/story/PART_1_PROLOGUE_ACT_1_SCRIPT.md`](story/PART_1_PROLOGUE_ACT_1_SCRIPT.md). The scene implementation should be reconciled against that script as the next integration pass. The script intentionally keeps Teia's escape, the neutral space, and Mira's identity unexplained at this stage.
