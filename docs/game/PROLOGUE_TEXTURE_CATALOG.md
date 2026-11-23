# Prologue texture and asset production catalog

Status: production checklist; assets are not considered integrated until the source image, material metadata, UV assignment, and in-scene review are all present.

## Shared art direction

- Target look: high-quality stylized semi-realistic anime JRPG environments; physically believable materials, clean readable forms, restrained color grading, detailed surface response.
- Modern Earth: neutral warm wood, cool grey plaster/concrete, natural daylight, realistic but slightly stylized urban materials.
- Fantasy dimension: the same rendering language and texture detail, with colder stone, muted violet energy accents, weathering and restrained bioluminescence.
- Keep lighting and material response coherent across every asset. Do not use pixel art, low-resolution painted blobs, blurry upscales, UI mockups, screenshots, contact sheets, or texture atlases as source assets.
- Generate each texture/object/character frame as an independent image. Preserve each approved original source.
- A texture may be reused only when it is genuinely the same physical material. Unique designed surfaces (fridge door, car body, window assembly, furniture fronts) receive their own dedicated maps.

## Texture acceptance requirements

1. Individual source image per material or unique designed surface; no multi-material collage.
2. Prefer 2048×2048 for ordinary props and 4096×4096 for hero/large surfaces when practical. Never silently upscale a small source and call it high resolution.
3. Albedo/base-color must not bake in directional lighting, cast shadows, highlights, or perspective.
4. Produce separate normal, roughness, and ambient-occlusion maps where the material needs them; keep resolution, UV alignment, and material identity consistent.
5. Seamless tiling is required only for repeating materials. Test opposite edges and a 3×3 tiled preview before approval.
6. Unique object textures should be authored for the actual UV layout, not merely pasted onto an arbitrary rectangle.
7. Check native-resolution detail, compression artifacts, seams, color consistency, and material readability in both close and gameplay camera views.
8. Record source dimensions, color space, intended UV scale, tile/repeat policy, and approval status in the asset manifest.

## Modern apartment catalog

| Asset ID | Dedicated source | Use |
|---|---|---|
| apt_floor_oak | oak floor planks | apartment floor |
| apt_wall_plaster_warm | warm off-white plaster | interior walls |
| apt_ceiling_paint | matte ceiling paint | ceiling |
| apt_baseboard_painted | painted skirting board | wall/floor trim |
| apt_window_frame_pvc | white PVC frame | window frame |
| apt_window_glass | neutral slightly reflective glass | window panes |
| apt_window_sill | painted stone sill | window sill |
| apt_fridge_front | brushed stainless refrigerator front | fridge front |
| apt_fridge_side | refrigerator enamel side panel | fridge side |
| apt_fridge_handle | brushed steel handle | fridge handle |
| apt_counter_laminate | warm grey countertop laminate | kitchen counter |
| apt_sink_steel | brushed stainless sink | sink basin |
| apt_faucet_chrome | chrome faucet | faucet |
| apt_desk_wood | dark walnut desk finish | desk |
| apt_bed_frame | charcoal painted bed frame | bed frame |
| apt_bed_linen | muted blue-grey cotton linen | bedding |
| apt_hoodie_charcoal | charcoal fleece knit | heroine hoodie material reference |
| apt_curtain_fabric | muted grey woven fabric | curtains |
| apt_door_wood | dark apartment door veneer | door |

## Modern street catalog

| Asset ID | Dedicated source | Use |
|---|---|---|
| street_asphalt | worn urban asphalt | road |
| street_sidewalk_concrete | light grey sidewalk concrete | sidewalk |
| street_paving_blocks | individual paving stone pattern | paved areas |
| street_curb_concrete | curb concrete | road edge |
| street_road_marking | road paint | lane and crossing markings |
| street_building_plaster | weathered urban facade plaster | building facade |
| street_brick_facade | red-brown brick | selected facade |
| street_window_glass | city window glass | building windows |
| street_window_frame | dark anodized window frame | window frames |
| street_car_paint_silver | silver automotive paint | car body |
| street_car_glass | automotive safety glass | windscreen/windows |
| street_car_rubber | tire rubber | tires |
| street_car_trim | black automotive trim | bumpers and trim |
| street_lamp_metal | dark painted metal | lamp posts |
| street_bench_wood | weathered outdoor timber | bench |
| street_bench_metal | dark coated steel | bench supports |
| street_tree_bark | natural tree bark | tree trunk |
| street_leaf_cluster | foliage material | tree leaves |

## Fantasy wake catalog

| Asset ID | Dedicated source | Use |
|---|---|---|
| fantasy_ground_cracked_stone | cracked grey stone | ruin ground |
| fantasy_arch_stone | aged carved stone | arches and columns |
| fantasy_broken_stone | fractured masonry | rubble |
| fantasy_moss | dark green moss | stone growth |
| fantasy_violet_crystal | translucent violet crystal | rift fragments |
| fantasy_energy_surface | controlled violet energy pattern | magical surfaces |

## Character sprite pipeline

- Character, monster, NPC, boss, and living entities remain 2D sprites placed in the real 3D world.
- One generation produces one character, one direction, one pose, and one animation frame. No contact sheets or repeated tiny characters.
- Keep approved design, canvas dimensions, scale, ground pivot, palette, and lighting consistent across frames.
- Required direction coverage is authored separately; do not rotate one front-facing sprite to fake a rear view.
- Review every original PNG at native resolution. Reject anatomy defects, costume drift, clipping, halos, opaque backgrounds, text, watermarks, and inconsistent proportions.
- Pack only approved source frames into runtime atlases. Preserve individual frames and emit a machine-readable manifest.

## Current integration status

- The current experimental scene still uses flat-color triangle geometry; it does not yet sample UV textures.
- The previously generated wooden-floor image is only a candidate source and has not been accepted into the repository or runtime.
- No fridge, window, car, street, furniture, or fantasy material set is currently approved and connected.
- This catalog is a production checklist, not a claim that the listed assets already exist.

## Completion gate

The prologue is not ready for broad testing until:
- all visible surfaces and unique props have approved material assets;
- the 3D renderer supports perspective-correct textured triangles and correct depth/occlusion;
- UVs and material bindings are validated for every mesh;
- sprite directions and animations are integrated;
- interaction uses world-space colliders/raycasting rather than distance-only placeholders;
- the complete apartment → street → sleep → fantasy wake sequence is playable on Android and desktop;
- automated tests, APK build, desktop build, and manual device testing pass.
