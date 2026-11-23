# RTS Runtime Architecture

## Goal

Rift Engine must support a full real-time strategy mode alongside its action-RPG mode. RTS is a first-class runtime mode, not a separate game executable or a collection of unrelated minigame scripts.

Both modes share:
- the same 3D world, transforms, model/material/texture assets and lighting;
- the same entity identity and lifecycle conventions;
- the same audio, save, diagnostics, plugin and asset services;
- the same deterministic simulation clock and platform layer.

Mode-specific systems remain separate behind explicit interfaces. Switching modes changes camera rig, input context, HUD, selection rules and simulation systems; it must not duplicate the world or reload every asset.

## RPG mode

- Third-person/elevated exploration camera.
- Direct control of the heroine.
- Character sprite animation and interaction targeting.
- Dialogue, quests, inventory and RPG combat.

## RTS mode

- Elevated strategic camera with pan, zoom and bounded rotation.
- Screen-space selection, drag selection, contextual right-click/command input and keyboard shortcuts.
- Unit selection groups and command issuing.
- Ground-plane movement in the shared 3D world.
- Buildings, placement previews, territory ownership and construction.
- Resource income, costs, production queues and technology unlocks.
- Unit roles, formations, attack/defend/hold/capture commands and hero abilities.
- Enemy AI, fog of war, minimap, tactical UI, mission objectives and replayable deterministic simulation.

## Campaign hero rule

The campaign heroine is represented as a special RTS unit, not a detached UI avatar. Her RPG equipment and unlocked abilities are translated through explicit balance profiles into RTS stats and ability definitions. Army technologies and building upgrades remain RTS progression and do not overwrite the RPG character sheet.

For the campaign event, the mission rule `heroDeathCausesDefeat` is enabled. If the heroine is defeated, the strategic mission ends in defeat even if bases and other units remain. This is a mission rule, not a universal hard-coded rule for every possible RTS scenario.

## Implemented foundation

`engine/.../rts/RtsWorld.kt` currently provides a platform-neutral first simulation slice:
- RTS teams, unit roles, building types and match status;
- validated unit/building specs and state;
- move, attack, hold and capture orders;
- resource accumulation, production queues and outpost capture;
- bounded deterministic update steps;
- explicit match setup/start boundary;
- campaign hero death defeat rule and headquarters mission rules.

The core is renderer-agnostic and uses the shared 3D `Vector3` ground plane (X/Z). It does not yet implement navigation/pathfinding around obstacles, formations, placement preview, selection input, camera controls, fog of war, minimap, AI, network play, or the RTS HUD. Production currently spawns at a simple offset from the building; a real spawn-point/occupancy system is still required.

## Required next implementation phases

1. **Mode and input layer:** shared `GameMode` contract, safe mode transitions, input-context switching and separate RPG/RTS camera rigs.
2. **3D selection and commands:** screen-to-world ground picking, selection rectangles, selectable components, command validation and group orders.
3. **Navigation:** navmesh/grid hybrid, obstacle avoidance, path smoothing, crowd separation and deterministic unit movement.
4. **Building placement:** terrain validation, footprint/collision checks, construction progress, resource payment and ownership.
5. **Combat and abilities:** stat profiles, cooldowns, target priorities, attack windups, area effects and hero ability adapter from RPG equipment.
6. **Economy and tech tree:** production, upgrades, unlock prerequisites, supply limits and save/load.
7. **AI and mission scripting:** enemy expansion, scouting, defense, attacks, reinforcements, objectives and difficulty profiles.
8. **Presentation:** strategic camera, minimap, HUD, selection indicators, unit markers and tactical feedback.
9. **Performance and QA:** large-unit-count profiling, fixed-step determinism, save/replay tests, Android touch controls and desktop mouse/keyboard controls.

## Non-negotiable architecture constraints

- RTS units and buildings are entities in the same 3D world, not sprites painted onto a separate 2D map.
- Living characters and creatures may continue to use directional 2D sprites in 3D space; buildings and props use actual 3D meshes.
- The heroine's RTS representation must preserve her visual identity and animation assets while using an RTS-specific movement/combat controller.
- The simulation must be independent from rendering and input devices so the same mission can run on Android and desktop.
- Keep the RPG systems operational; do not replace them with RTS assumptions.
- All new systems require tests and Rift Diagnostics integration. Diagnostics must report confirmed errors separately from warnings and hypotheses.
