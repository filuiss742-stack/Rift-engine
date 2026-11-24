import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from generate_procedural_material import generate
from validate_materials import validate_manifest


class ProceduralMaterialGeneratorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def test_generates_complete_pbr_set_and_valid_manifest(self):
        output = self.root / "stone"
        files = generate("stone", 128, 17, output)
        self.assertEqual(
            {"base_color", "normal", "roughness", "metallic", "height", "manifest"},
            set(files),
        )
        for key in ("base_color", "normal", "roughness", "metallic", "height"):
            self.assertTrue(files[key].is_file())
            from PIL import Image
            with Image.open(files[key]) as image:
                self.assertEqual((128, 128), image.size)
                image.verify()
        self.assertEqual([], validate_manifest(files["manifest"]))

    def test_same_seed_produces_identical_base_color(self):
        first = generate("soil", 128, 99, self.root / "first")
        second = generate("soil", 128, 99, self.root / "second")
        digest = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
        self.assertEqual(digest(first["base_color"]), digest(second["base_color"]))

    def test_different_seed_changes_output(self):
        first = generate("grass", 128, 1, self.root / "first")
        second = generate("grass", 128, 2, self.root / "second")
        self.assertNotEqual(first["base_color"].read_bytes(), second["base_color"].read_bytes())

    def test_rejects_non_power_of_two_size(self):
        with self.assertRaisesRegex(ValueError, "power of two"):
            generate("wood", 96, 1, self.root / "invalid")

    def test_rejects_unknown_material(self):
        with self.assertRaisesRegex(ValueError, "Unsupported material"):
            generate("water", 128, 1, self.root / "invalid")

    def test_all_material_families_produce_distinct_textures(self):
        hashes = set()
        for material in ("stone", "soil", "grass", "wood", "metal"):
            files = generate(material, 128, 5, self.root / material)
            hashes.add(hashlib.sha256(files["base_color"].read_bytes()).hexdigest())
        self.assertEqual(5, len(hashes))


if __name__ == "__main__":
    unittest.main()
