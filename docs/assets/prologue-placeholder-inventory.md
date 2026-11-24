# Visual Placeholder Inventory — Current Prologue Prototype

Branch audited: development/0.7.0
Audited files:
- demo-game/src/main/kotlin/com/rift/engine/demo/HybridPrologueScene.kt
- app/src/main/assets/prologue/**
- app/src/main/assets/sprites/**

This is a partial inventory of confirmed placeholder visuals, not a claim that every repository asset has been audited.

## Confirmed primitive geometry in HybridPrologueScene

### Apartment
- Ground plane: replace flat color with a real floor material (geometry remains 3D).
- Three visible room walls: replace flat face colors with wall/plaster/wallpaper material and proper trim.
- Bed: replace cuboid placeholder with a modeled bed, mattress, blanket, pillow and legs.
- Desk: replace cuboid placeholder with a modeled desk and details.
- Fridge: replace cuboid placeholder with a detailed refrigerator model, door seam/handle and material.
- Sink: replace cuboid placeholder with sink/cabinet/faucet model.
- Phone: replace cuboid placeholder with a small detailed 3D phone model.
- TV: replace cuboid placeholder with screen/bezel/stand or wall-mounted assembly.
- Exit/door: replace placeholder with a proper door, frame, handle and interaction pivot.
- Window: replace flat cuboid glass/sill with frame, glass material and sill.

### Modern street
- Ground plane: replace flat color with road/sidewalk surface materials and markings.
- Store, shop and home: replace single cuboids with modular 3D building meshes, doors, windows, facade details and roof geometry.
- Bench: replace cuboid with a detailed 3D bench.
- Message/phone-like prop: replace placeholder cuboid with a detailed 3D object.
- Bus stop: replace cuboid with shelter, supports, roof, seat and sign.
- Street lamps/trees: replace stacked cuboids with modeled lamp posts and trees (trunk, branches, foliage).

### Fantasy wake / ruins
- Ground plane: replace flat color with terrain/stone material and variation.
- Ruin walls/columns: replace primitive blocks with damaged masonry modules, broken edges and material detail.
- Central platform: replace cuboid with a designed stone altar/platform.
- Campfire: replace stacked cuboids with logs/embers/flame effect and lighting.
- Enemy: replace cuboid placeholder with an approved character/creature sprite set using the established per-frame workflow.
- Rift/glitch marker: replace primitive bars with a layered VFX asset/system.

## Existing raster placeholders
The repository contains legacy prologue maps and small object images under app/src/main/assets/prologue, plus a legacy heroine idle sprite and a trial sprite sheet. These must be inspected individually and migrated or retired only after their runtime references are replaced. They are not approved production master assets.

## Required implementation discipline

- Preserve each prop's stable interaction ID while replacing only its render asset/model.
- Separate visual mesh from collider and interaction target.
- Do not replace a physical 3D prop with a flat image.
- Keep all source textures/models/sprite frames separately versioned.
- Validate each replacement in the apartment, street, and ruins scenes at runtime before retiring its placeholder.

## Not yet audited

This document does not yet inventory every primitive or placeholder in all demo scenes, platform UI, test scenes, RTS systems, and future world content. Continue repository-wide code and asset-reference searches before claiming full replacement.
