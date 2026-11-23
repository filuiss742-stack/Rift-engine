import json
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from validate_materials import validate_manifest


class MaterialValidatorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.image_path = self.root / "oak.png"
        Image.new("RGB", (128, 128), (120, 75, 40)).save(self.image_path)
        self.manifest_path = self.root / "materials.json"

    def tearDown(self):
        self.temp.cleanup()

    def write_manifest(self, material):
        payload = {
            "schema_version": 1,
            "minimum_width": 128,
            "minimum_height": 128,
            "materials": [material],
        }
        self.manifest_path.write_text(json.dumps(payload), encoding="utf-8")

    def test_accepts_valid_unique_non_tiling_material(self):
        self.write_manifest({
            "id": "apt_floor_oak",
            "tileable": False,
            "maps": {"base_color": "oak.png"},
        })
        self.assertEqual([], validate_manifest(self.manifest_path))

    def test_rejects_missing_material_image(self):
        self.write_manifest({
            "id": "apt_floor_oak",
            "tileable": False,
            "maps": {"base_color": "missing.png"},
        })
        self.assertTrue(any("missing base_color" in error for error in validate_manifest(self.manifest_path)))

    def test_rejects_undersized_material_image(self):
        Image.new("RGB", (64, 64), (80, 60, 40)).save(self.image_path)
        self.write_manifest({
            "id": "apt_floor_oak",
            "tileable": False,
            "maps": {"base_color": "oak.png"},
        })
        self.assertTrue(any("minimum is 128x128" in error for error in validate_manifest(self.manifest_path)))

    def test_rejects_non_seamless_tileable_material(self):
        image = Image.new("RGB", (128, 128), (0, 0, 0))
        for y in range(128):
            image.putpixel((127, y), (255, 255, 255))
        image.save(self.image_path)
        self.write_manifest({
            "id": "apt_floor_oak",
            "tileable": True,
            "maps": {"base_color": "oak.png"},
        })
        self.assertTrue(any("edge mismatch" in error for error in validate_manifest(self.manifest_path)))

    def test_rejects_path_traversal(self):
        self.write_manifest({
            "id": "apt_floor_oak",
            "tileable": False,
            "maps": {"base_color": "../oak.png"},
        })
        self.assertTrue(any("escapes" in error for error in validate_manifest(self.manifest_path)))


if __name__ == "__main__":
    unittest.main()
