# Rift Combat Trial

The Android and Windows demo apps start in this playable trial.

## Controls
- WASD / arrows / left virtual joystick: move in the outdoor hub.
- Space / ATTACK: melee strike; move close to the stone golem.
- Shift / DODGE: directional evade with a short invulnerability window.
- Q / SKILL: close-range burst with cooldown.
- E / USE: speak to Mira nearby or enter the violet dungeon gate. Touch players have a dedicated USE button.
- Dungeon: A/D or joystick to move; Space / JUMP to clear blockers; E / USE near the exit returns to the hub.

The golem pauses and displays a red attack-radius telegraph before its area strike. Leaving the marked radius or timing a dodge avoids damage.

## Story
Helping Mira adds 2 relationship points and sets the mira_helped flag. Dismissing her subtracts 2 and sets mira_dismissed. Returning from the dungeon resolves a good, neutral, or bitter ending from the relationship value.

## Visual scope
The bundled 4-column by 2-row sprite sheet contains 64x64 frames. Hero and golem use 3 FPS frame animations. The hub uses Y-sorted 2D sprites and a follow camera; the dungeon uses a side-view movement profile, jump physics, obstacles, and a zoomed camera.

This is a 2.5D gameplay prototype, not true 3D. Rift 0.5.0 currently has a 2D camera and 2D physics; 3D coordinates, camera rotation, and hybrid 2D/3D rendering remain future work.

Automated tests cover telegraphed attacks, dodge timing, damage, melee hitboxes, skill damage, dialogue effects, relationship routes, and dungeon jumping. They do not replace launching the APK and Windows app image on real devices.
