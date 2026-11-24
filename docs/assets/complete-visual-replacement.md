# Complete Visual Asset Replacement

Status: planned migration; no claim that the asset replacement is complete.

## Goal

Remove temporary visual placeholders from the game and replace them with individually authored, reviewed production assets. This includes primitive-looking placeholder meshes, colored debug shapes, crude character figures, flat map backgrounds, and low-resolution placeholder object images.

Do not remove valid gameplay geometry. Floors, walls, roads, collision volumes, modular mesh construction, pivots, sockets, and other structural geometry remain real 3D systems; their visible materials and models are what must be upgraded.

## Asset families in scope

1. Characters: all three heroine versions, NPCs, civilians, allies, enemies, bosses, and RTS infantry.
2. Creatures: monsters, distorted entities, wildlife, and bosses.
3. Environment: apartment, city streets, buildings, interiors, fantasy ruins, fourth-world settlements, battlefields, terrain, vegetation, and props.
4. Interactive objects: doors, windows, beds, desks, phones, refrigerators, furniture, containers, switches, quest objects, keys, and machinery.
5. RTS units: infantry sprites; separately modeled 3D vehicles, artillery, mechs/golems, aircraft, siege engines, and other non-infantry units.
6. Materials: dedicated texture maps for each required material, with consistent UVs and validated tiling where applicable.
7. Effects: rifts, distortion, magic, weapon effects, impacts, smoke, dust, explosions, projectiles, and environmental effects.
8. UI art: portraits, icons, unit/building icons, inventory icons, map markers, and other authored illustrations. Functional UI layout remains UI, not a generated background image.

## Production rules

- One generated image per asset or per individual character pose/frame. Never ask image generation to create a crowded contact sheet as source art.
- Keep source assets separate. Atlas packing is a later deterministic build step, never a substitute for individual source frames.
- For sprites, preserve consistent canvas dimensions, character proportions, palette, costume, lighting, camera/view direction, ground pivot, and transparent background across frames.
- For 3D assets built from images and code, author texture maps individually, define mesh geometry and part hierarchy in code, and specify pivots, sockets, collision shapes, and animation data separately.
- Do not use a flat image plane to impersonate a 3D room, building, road, or physical object.
- Do not use a flat sprite as a physical prop. The established 2D-sprite exception is living characters/creatures and RTS infantry, rendered in the 3D world with depth testing and appropriate view selection.
- Preserve gameplay collision and interaction data when replacing visuals. Visual mesh and collision geometry are related but are not the same asset.
- Retain original high-resolution sources and record provenance, license, dimensions, color space, alpha mode, material assignment, and import settings.

## Required quality gates

1. **Inventory:** identify every placeholder and every runtime reference to it.
2. **Specification:** record intended appearance, dimensions/scale, view, material, pivot, animation requirements, and use locations.
3. **Generation/modeling:** create each source asset independently using the approved workflow.
4. **Visual QA:** inspect each asset at native resolution and in-engine scale. Reject anatomy defects, artifacts, clipping, unwanted halos/backgrounds, style drift, unreadable details, and mismatched proportions.
5. **Technical QA:** validate dimensions, alpha, UV/material compatibility, atlas bounds, pivots, animation metadata, mesh integrity, collision alignment, and import settings.
6. **Integration:** replace references incrementally without breaking gameplay logic.
7. **Regression:** run automated tests and inspect Android and desktop builds, including camera angles, lighting, occlusion, animation, and performance.
8. **Retirement:** remove obsolete placeholder assets only after no runtime, test, sample, or documentation reference depends on them.

## Migration order

- Phase A: inventory and classify all current placeholders and their references.
- Phase B: establish approved style sheets and reference assets for each world and visual family.
- Phase C: replace the playable prologue's visible assets first, keeping the 3D world genuinely 3D.
- Phase D: complete character/creature directional sprites and animation sets.
- Phase E: build the fourth-world RTS asset families, with 3D non-infantry units and 2D infantry.
- Phase F: replace remaining environments, props, materials, effects, and UI art.
- Phase G: run a repository-wide placeholder scan and in-game visual audit.

## Completion criteria

The migration is complete only when the inventory is closed, every in-scope placeholder has an approved replacement or a documented intentional debug-only exception, all runtime references have been migrated, automated validation passes, and Android/desktop visual regression checks are recorded. A document, generated mockup, or isolated asset does not count as completion.
