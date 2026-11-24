# World soundtracks

The prologue uses three original, deterministic PCM soundtrack sketches:

| World / viewpoint | Asset | Scene use |
|---|---|---|
| Thea's distortion | `music/03_thea_distortion.wav` | Opening rift sequence |
| Earth / Lisa | `music/01_earth_everyday.wav` | Apartment and modern street |
| Ruined fantasy / Mira | `music/02_mira_ruined_fantasy.wav` | Awakening and first exploration |

The source generator is `tools/audio/generate_prologue_soundtracks.py`. It uses only
Python's standard library and emits 22.05 kHz, 16-bit mono PCM WAV files. This
format is decoded by Android MediaPlayer and the desktop Java Sound backend
without adding a codec dependency.

Generated files live in `demo-game/src/main/resources/music/`. The Android app
adds that shared resource directory to its packaged assets; the desktop audio
loader first checks the project audio root and then its classpath resources.

## Rebuild

Run:

```bash
python tools/audio/generate_prologue_soundtracks.py --output demo-game/src/main/resources/music
python -m unittest discover -s tools/audio -p "test_*.py" -v
```

The GitHub Actions workflow `.github/workflows/generate-prologue-soundtracks.yml`
regenerates the files, runs the audio tests, publishes a recovery artifact, and
commits the assets to `development/0.7.0`.

These are original prototype cues, not final mastered orchestral scores. The
engine currently switches tracks at world transitions; adaptive combat layers
and crossfades are later audio-system improvements.
