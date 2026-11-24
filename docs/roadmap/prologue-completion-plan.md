# Prologue completion plan — «Искажение: Нулевая грань»

Status: in progress. This plan distinguishes the current playable prototype from the finished, shippable prologue. A scene is not complete merely because it can be entered or rendered.

## Definition of done

The prologue is complete when a player can start a fresh save and reach the first red rift in the fantasy ruins without developer intervention, on Android and desktop, with:
- the approved opening tear / Thea hand sequence;
- Lisa's ordinary morning in her apartment;
- the institute's neutral-space announcement delivered naturally through the TV/news;
- a short city outing with readable objectives and believable interactions;
- Lisa returning home, going to sleep, and waking as Mira in the ruined fantasy location;
- a tutorial that teaches movement, camera-relative direction, interaction, attack, enemy telegraph reading, and dodge;
- the first distortion/glitch beat and a clear chapter-end handoff;
- coherent dialogue, sound, transitions, UI, save/resume behavior, and no progression blockers.

Story canon is owned by the user. Implementation may polish presentation and connective dialogue, but must not invent major lore, reveal future twists early, or change the three-version identity rules.

## Current prototype — verified from HybridPrologueScene

Implemented in the current demo scene:
- a timed abstract red-tear intro with a hand silhouette;
- a 3D-blockout apartment with phone, fridge, TV, bed, exit and simple interaction tasks;
- a blockout street with a bus stop, shop and return-home interaction;
- a fantasy ruin/camp blockout;
- dialogue boxes and basic objective hints;
- keyboard and virtual joystick/button input;
- a simple enemy encounter with a visible attack telegraph, attack input, dodge movement, and first glitch interaction.

These are prototype systems, not final content. The heroine currently loads a legacy idle texture and is drawn as one billboard; the scene uses primitive blockout geometry and placeholder copy. The fantasy enemy/combat is only a tutorial stub and does not yet provide a complete, verified damage/evade loop. No production-ready character asset set, directional sprite set, final environment art, soundscape, or full-device acceptance pass is established by this scene.

## Work packages and acceptance gates

### P0 — Restore a trustworthy build
- Fix the currently failing desktop texture-repeat test.
- Fix the directional-sprite mesh test and near-plane clipping test.
- Run shared JVM tests and platform CI again; record exact commit and run IDs.
- Do not build new gameplay on a red baseline.

Exit gate: Android and desktop CI both green on the same commit; no known failing tests hidden or skipped.

### P1 — Production heroine asset pack
- Approve one canonical modern Lisa neutral full-body image.
- Produce separate portrait, bust, full-body and expression PNGs.
- Produce eight authored directional views, then separate animation frames for required actions.
- Validate each image, manually inspect anatomy/identity/crop/alpha, lock a stable canvas and feet pivot.
- Replace the legacy idle billboard only after approved source images and runtime directional animation are available.

Exit gate: the heroine renders with correct view selection and animation in a real 3D test scene; no single front sprite is rotated to fake other views.

### P2 — Real 3D environment art
- Replace apartment, street and fantasy blockout cuboids with authored 3D meshes and proper material maps.
- Add readable silhouettes, set dressing, collision bounds, occlusion and lighting.
- Preserve the 3D-world / 2D-character split.
- Validate materials and import bounds; keep source assets and reproducible manifests.

Exit gate: each location is navigable, readable and visually coherent at the actual target camera and mobile resolution.

### P3 — Gameplay and progression correctness
- Make interactions contextual and robust; prevent accidental progression skips.
- Add proper camera-relative movement and directional sprite selection.
- Implement collision-aware movement and interaction ranges.
- Complete enemy telegraph -> attack resolution -> dodge invulnerability / hit reaction -> damage feedback.
- Ensure the player can neither bypass nor become trapped by objectives.
- Add checkpoints and save/resume at safe story boundaries.

Exit gate: a clean playthrough succeeds repeatedly on touch and keyboard controls; deliberate edge-case tests cover missed interactions, repeated inputs, app pause/resume and scene transitions.

### P4 — Narrative and presentation pass
- Refine the intro timing and hand/rift visual; no placeholder primitive hand in final.
- Review every dialogue line against the approved story tone and canon.
- Add sound design, ambient layers, interaction feedback, combat cues and transition audio.
- Add final UI layout, readable mobile typography, subtitles and accessibility options.
- Add scene transitions, fades and chapter-end screen with no abrupt debug behavior.

Exit gate: the prologue reads as one continuous chapter rather than a sequence of test rooms.

### P5 — Full acceptance
- Fresh-install and fresh-save playthrough on Android.
- Desktop playthrough with keyboard/controller-equivalent inputs.
- Save/resume, pause/resume, rotation/resizing, low-memory and missing-asset diagnostics.
- No placeholder art, debug text, softlocks, unhandled crashes, or unresolved critical diagnostics.
- Capture a short acceptance recording and attach CI evidence to the release candidate.

Exit gate: all mandatory checks pass on a versioned candidate. Only then call the prologue complete.

## Delivery rule

Work proceeds in vertical slices: make one part real, test it, then expand. Estimates are not promises; progress is reported by completed gates and verified evidence. A successful prototype scene, asset generator, or unit-test suite alone does not equal a finished prologue.
