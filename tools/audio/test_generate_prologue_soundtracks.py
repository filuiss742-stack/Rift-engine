import tempfile
import unittest
import wave
from pathlib import Path

from generate_prologue_soundtracks import SAMPLE_RATE, generate


class PrologueSoundtrackTests(unittest.TestCase):
    def test_generates_three_decodable_world_tracks(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = generate(Path(directory), duration=2.0)
            self.assertEqual(4, len(paths))
            self.assertEqual(
                {"01_earth_everyday.wav", "02_mira_ruined_fantasy.wav", "03_thea_distortion.wav", "04_warfront_rts.wav"},
                {path.name for path in paths},
            )
            for path in paths:
                with wave.open(str(path), "rb") as source:
                    self.assertEqual(1, source.getnchannels())
                    self.assertEqual(2, source.getsampwidth())
                    self.assertEqual(SAMPLE_RATE, source.getframerate())
                    self.assertGreater(source.getnframes(), SAMPLE_RATE)
                    self.assertLessEqual(source.getnframes(), SAMPLE_RATE * 2)

    def test_output_is_not_silent(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = generate(Path(directory), duration=1.0)
            for path in paths:
                with wave.open(str(path), "rb") as source:
                    raw = source.readframes(source.getnframes())
                self.assertTrue(any(raw))


if __name__ == "__main__":
    unittest.main()
