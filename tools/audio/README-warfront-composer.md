# Warfront code-composed soundtrack

This source recreates the approved 126 BPM, 4/4, 48-bar Warfront prototype as four synchronized stereo PCM stems.

## Requirements

- C compiler (C11)
- FluidSynth shared library
- A General MIDI SoundFont
- libfluidsynth and libm at link time

The current prototype's SoundFont path is /usr/share/sounds/sf2/TimGM6mb.sf2. Change sfpath in warfront_composer.c to a valid local SoundFont path before running elsewhere. Do not bundle a SoundFont unless its redistribution license has been checked.

## Build (Linux example)

```sh
mkdir -p /tmp/warfront-output
cc -O2 -std=c11 tools/audio/warfront_composer.c -o /tmp/warfront-composer -lfluidsynth -lm
/tmp/warfront-composer /tmp/warfront-output
```

The output directory must exist before running. The renderer writes:
- 01_foundation.wav — harmony, bass and pulsing cellos
- 02_brass_theme.wav — main brass melody
- 03_war_drums.wav — combat percussion
- 04_battle_drive.wav — battle counter-rhythm

All stems have the same sample rate, duration, tempo and zero-time origin. The approved full mix and strategy preview are preserved as separate rendered deliverables in the conversation artifact package; the binary audio masters have not yet been committed to the game repository.

## Important limitations

This is a prototype renderer, not the final runtime music pipeline. The SoundFont determines the instrument timbres. The output is not automatically mastered or validated on target devices. The hard-coded SoundFont path must be made configurable, and a portable render/test job should be added before CI use.