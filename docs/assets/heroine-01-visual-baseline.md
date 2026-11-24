# Heroine 01 — Visual Baseline

Status: working art direction derived from the approved three-version heroine reference. This is not an assertion that a production-ready sprite set exists.

## Identity and canonical version

Heroine 01 is the modern-Earth version used in the opening section. Do not confuse her with the alternate fantasy-world body (blue travel cloak) or the distorted third version.

## Approved modern-world appearance

- Young woman with pale skin, layered near-black shoulder-length hair and violet eyes.
- Quiet, reserved, slightly melancholic neutral expression.
- Oversized charcoal/black hoodie, light grey-white shirt, loose dark cargo trousers and black/white sneakers.
- Practical, understated modern streetwear. No fantasy armour, cloak, weapons, jewellery, or decorative accessories unless the story explicitly adds them.
- Keep the recognisable hair silhouette, eye colour, clothing blocks and sneaker design stable across every view and animation.

The existing small pixel-art idle file is a legacy placeholder, not the high-resolution visual master. The three-version reference image is the visual source for the modern/fantasy/distorted distinction; the modern heroine is the leftmost design.

## Portrait and medium-sprite set

Before full animation production, create a small identity pack as independent PNGs:

- neutral head-and-shoulders portrait;
- neutral waist-up / bust sprite;
- full-body neutral reference;
- separate expression portraits (neutral, happy, sad, angry, surprised, frightened);
- optional isolated eye, eyebrow and mouth layers only if a tested facial-animation system needs them.

All images must preserve the same canonical face, violet eyes, near-black layered hair and modern outfit. Expression images change facial muscles and mouth/eyebrow shapes, not the character's age, head geometry, camera crop, hairline or rendering style. Do not generate all expressions in one sheet.

## First approved production asset

The first production candidate must be exactly one full-body neutral idle pose:
- one character only;
- one pose only;
- front three-quarter view suitable for the initial elevated third-person camera;
- transparent PNG, no checkerboard, backdrop, vignette, floor, cast shadow, labels, borders, or text;
- entire hair silhouette, hands, legs, and both shoes inside the canvas, with visible transparent padding;
- natural relaxed stance and readable feet/ground-contact point;
- clean, deliberate anatomy and clothing construction.

Do not generate an animation sheet, turnaround sheet, collage, or multiple candidates in one image. Those are forbidden as source assets.

## Consistency lock after approval

Once the neutral idle is approved, it becomes the character identity reference. Every later image must preserve:
- face shape, eye colour, hair length/shape and hair colour;
- hoodie, shirt, trousers, sneakers and their exact palette;
- body proportions, rendering style, camera scale, light direction and line/shading treatment;
- identical canvas dimensions and a stable ground-contact pivot.

Create each directional view and each animation frame as an independent PNG. Never ask the image generator to draw multiple poses or frames together. If a frame drifts, regenerate it with the approved neutral reference and the closest already-approved adjacent frame as references. Do not repair major anatomy or costume drift by stretching, smearing, or compositing mismatched body parts.

## Directional view plan

The first camera setup uses a slightly elevated third-person angle. Build front, front-left, front-right, left, right, back-left, back-right and back views as separate art assets. Add higher/lower camera elevation variants only when a tested scene needs them. The sprite selector chooses the nearest authored view; it does not rotate a flat front-facing image through large angles.

## 3D material and texture production rules

- One source image per texture/material tile or map; never generate a contact sheet as a game texture.
- Keep texture maps for a material registered to the same material ID and identical dimensions/UV alignment.
- For PBR materials, distinguish base colour, normal, roughness, metallic and ambient-occlusion maps. Do not substitute a colour image for a normal or roughness map.
- Texture tiles intended for repetition must be seamless at all four edges. Test edge continuity before accepting them.
- Keep high-resolution source textures. Mipmaps and runtime compression are build outputs, not reasons to destroy the source.
- A single reference image does not define hidden geometry. Model geometry must be authored/imported separately or generated through an explicitly documented voxel/mesh process.

## Quality gate

A candidate is not accepted until it passes both visual review and technical validation:
1. Inspect at native resolution and on a checkerboard/neutral background.
2. Verify exactly one subject and one pose.
3. Check for extra/missing fingers or limbs, duplicated features, broken clothing seams, inconsistent shoes, floating details, and unwanted text/watermarks.
4. Check full silhouette, no clipped pixels, no background haze, and genuine alpha transparency.
5. Compare side-by-side with the approved identity reference.
6. Verify canvas size, pivot, alpha bounds and naming metadata.
7. Keep rejected candidates outside runtime asset folders. Do not pack or commit them as approved game assets.

Only accepted source PNGs may enter atlas packing. Atlas generation must be deterministic and preserve all source files.
