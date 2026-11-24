import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from build_character_atlas import AssetError, build


class CharacterAtlasBuilderTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.character = self.root / "heroine_01"
        (self.character / "source" / "idle").mkdir(parents=True)

    def tearDown(self):
        self.temp.cleanup()

    def frame(self, index, color):
        image = Image.new("RGBA", (128, 192), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.ellipse((35, 10, 93, 70), fill=color)
        draw.rectangle((42, 65, 86, 178), fill=color)
        path = self.character / "source" / "idle" / f"heroine_01_idle_{index:03d}.png"
        image.save(path)
        return path

    def test_builds_atlas_and_metadata_from_individual_pngs(self):
        self.frame(0, (180, 80, 190, 255))
        self.frame(1, (180, 90, 190, 255))
        atlas, manifest = build(self.character, self.character / "atlas", None, 2, 4, True)
        self.assertTrue(atlas.is_file())
        self.assertTrue(manifest.is_file())
        with Image.open(atlas) as image:
            self.assertEqual((2 * 128 + 3 * 4, 192 + 2 * 4), image.size)
        self.assertEqual(2, len(__import__("json").loads(manifest.read_text())["animations"]["idle"]["frames"]))

    def test_rejects_filename_with_wrong_character_identity(self):
        self.frame(0, (180, 80, 190, 255))
        bad = self.character / "source" / "idle" / "other_hero_idle_001.png"
        bad.write_bytes((self.character / "source" / "idle" / "heroine_01_idle_000.png").read_bytes())
        with self.assertRaisesRegex(AssetError, "not named for"):
            build(self.character, self.character / "atlas", None, 2, 4, True)

    def test_rejects_missing_frame_index(self):
        self.frame(0, (180, 80, 190, 255))
        self.frame(2, (180, 90, 190, 255))
        with self.assertRaisesRegex(AssetError, "Missing frame number"):
            build(self.character, self.character / "atlas", None, 2, 4, True)


if __name__ == "__main__":
    unittest.main()
