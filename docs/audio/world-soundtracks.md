# World soundtracks

The project has four original, deterministic world scores. The three prologue scores and the fourth-world RTS score are generated
on demand by `engine.audio.ProceduralMusicAsset` and played through the same
`AudioManager` interface on Android and Windows.

| World / viewpoint | Score ID | Scene use | Musical direction |
|---|---|---|---|
| Thea's distortion | `thea` | Opening rift sequence | Dark low drones, unstable pulses, dissonant upper tones |
| Earth / Lisa | `earth` | Apartment and modern street | Warm soft keys, gentle pulse, light melodic phrases |
| Ruined fantasy / Mira | `mira` | Awakening and first exploration | Slow string-like pads, airy melody, sparse distant percussion |\n| Fourth world / RTS campaign | `warfront` | Fourth-world strategic warfare | Retro-industrial march, mechanical bass ostinato, dry field percussion, restrained minor-key command motif |

The prologue requests these IDs through the host audio loader using
`procedural:thea`, `procedural:earth`, and `procedural:mira`. Each host renders
a 38-second, 22.05 kHz, 16-bit mono PCM WAV into a temporary file and plays it
with its native decoder. Android uses MediaPlayer; Windows uses Java Sound.
Resources are loaded lazily on the first transition into each world and looped.
Leaving a scene stops the current track; the shared AudioManager retains loaded
handles until the game runtime closes, when it releases the players and temporary WAV files.

## Offline WAV masters

`tools/audio/generate_prologue_soundtracks.py` independently creates optional
WAV masters using only Python's standard library:

```bash
python tools/audio/generate_prologue_soundtracks.py --output /tmp/rift-music
python -m unittest discover -s tools/audio -p "test_*.py" -v
```

The GitHub Actions workflow `.github/workflows/generate-prologue-soundtracks.yml`
runs the generator and tests, then uploads the three masters as a workflow
artifact. The game itself does not depend on generated repository binaries.

These are original prototype cues, not final mastered orchestral scores. The
engine currently switches tracks at world transitions; adaptive combat layers,
crossfades, and final mastering remain later audio improvements.
