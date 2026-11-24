import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from validate_character_art import validate_image, validate_many


class CharacterArtValidationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def make_png(self, name="portrait.png", size=(512, 512), box=(80, 60, 430, 450), opaque=False):
        image = Image.new("RGBA", size, (0, 0, 0, 255 if opaque else 0))
        ImageDraw.Draw(image).ellipse(box, fill=(140, 80, 180, 255))
        path = self.root / name
        image.save(path)
        return path

    def test_valid_portrait_passes(self):
        report = validate_image(self.make_png(), "portrait")
        self.assertEqual([], report.errors)

    def test_opaque_background_fails(self):
        report = validate_image(self.make_png(opaque=True), "portrait")
        self.assertTrue(any("fully opaque" in error for error in report.errors))

    def test_small_face_component_is_allowed_by_its_profile(self):
        path = self.make_png("eye.png", (128, 128), (20, 20, 108, 108))
        self.assertEqual([], validate_image(path, "face_component").errors)

    def test_portrait_aspect_ratio_rejected_for_full_body(self):
        report = validate_image(self.make_png(), "full_body")
        self.assertTrue(any("Aspect ratio" in error for error in report.errors))

    def test_subject_touching_canvas_edge_fails(self):
        report = validate_image(self.make_png(box=(0, 0, 511, 511)), "portrait")
        self.assertTrue(any("padding" in error for error in report.errors))

    def test_duplicate_images_are_reported(self):
        a = self.make_png("a.png")
        b = self.root / "b.png"
        b.write_bytes(a.read_bytes())
        result = validate_many([a, b], "portrait", 2, True)
        self.assertFalse(result["passed"])
        self.assertEqual(1, result["summary"]["failed"])

    def test_non_png_extension_fails(self):
        original = self.make_png()
        renamed = self.root / "portrait.jpg"
        renamed.write_bytes(original.read_bytes())
        report = validate_image(renamed, "portrait")
        self.assertTrue(any("Source must be PNG" in error for error in report.errors))


if __name__ == "__main__":
    unittest.main()
