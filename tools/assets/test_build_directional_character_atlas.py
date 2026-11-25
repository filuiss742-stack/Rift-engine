import json
import sys
import tempfile
import unittest
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from build_directional_character_atlas import AssetError, DIRECTIONS, build


class DirectionalAtlasTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name) / "lisa"
        self.root.mkdir()

    def tearDown(self):
        self.tmp.cleanup()

    def make_frames(self, actions=("idle","walk","attack","dodge"), count=2):
        for d_index, direction in enumerate(DIRECTIONS):
            for action in actions:
                folder = self.root/"source"/direction/action
                folder.mkdir(parents=True, exist_ok=True)
                for i in range(count):
                    im = Image.new("RGBA",(96,128),(0,0,0,0))
                    draw = ImageDraw.Draw(im)
                    draw.ellipse((28,8,68,48),fill=(100+d_index,70+i,180,255))
                    draw.rectangle((34,44,62,116),fill=(100+d_index,70+i,180,255))
                    im.save(folder/f"lisa_{direction}_{action}_{i:03d}.png")

    def test_packs_all_eight_authored_views_and_clips(self):
        self.make_frames()
        atlas, manifest_path = build(self.root,self.root/"atlas",8,2)
        data=json.loads(manifest_path.read_text())
        self.assertEqual(8,len(data["directions"]))
        self.assertEqual(2,len(data["animations"]["walk"]["back"]["frames"]))
        self.assertTrue(atlas.is_file())

    def test_rejects_missing_direction_instead_of_faking_it(self):
        self.make_frames()
        import shutil
        shutil.rmtree(self.root/"source"/"back")
        with self.assertRaisesRegex(AssetError,"Missing independently authored directions"):
            build(self.root,self.root/"atlas")

    def test_rejects_missing_required_action(self):
        self.make_frames(actions=("idle","walk","attack"))
        with self.assertRaisesRegex(AssetError,"Missing required actions"):
            build(self.root,self.root/"atlas")

    def test_rejects_direction_with_different_frame_count(self):
        self.make_frames()
        extra=self.root/"source"/"left"/"walk"/"lisa_left_walk_002.png"
        im=Image.new("RGBA",(96,128),(0,0,0,0))
        ImageDraw.Draw(im).ellipse((28,8,68,48),fill=(1,2,3,255))
        im.save(extra)
        with self.assertRaisesRegex(AssetError,"Frame count differs"):
            build(self.root,self.root/"atlas")

    def test_manifest_uses_configured_pivot_and_action_fps(self):
        self.make_frames()
        _, manifest_path = build(self.root, self.root/"atlas", pivot_x=40, pivot_y=110,
                                 fps_overrides={"walk": 9, "dodge": 18})
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        self.assertEqual({"x": 40, "y": 110}, data["pivot"])
        self.assertEqual(9, data["animations"]["walk"]["front"]["fps"])
        self.assertEqual(18, data["animations"]["dodge"]["back"]["fps"])

    def test_rejects_pivot_outside_canvas(self):
        self.make_frames()
        with self.assertRaisesRegex(AssetError, "pivot must be inside"):
            build(self.root, self.root/"atlas", pivot_x=96, pivot_y=20)

    def test_rejects_atlas_over_pixel_budget(self):
        self.make_frames()
        with self.assertRaisesRegex(AssetError, "safety limit"):
            build(self.root, self.root/"atlas", max_atlas_pixels=100)

    def test_rejects_invalid_fps_override(self):
        self.make_frames()
        with self.assertRaisesRegex(AssetError, "FPS override"):
            build(self.root, self.root/"atlas", fps_overrides={"walk": 0})

    def test_rejects_non_png_files_in_action_folder(self):
        self.make_frames()
        (self.root/"source"/"front"/"idle"/"notes.txt").write_text("not an image")
        with self.assertRaisesRegex(AssetError, "Unexpected files"):
            build(self.root, self.root/"atlas")

    def test_rejects_mixed_character_identity(self):
        self.make_frames()
        path=self.root/"source"/"front"/"idle"/"not_lisa_front_idle_000.png"
        (self.root/"source"/"front"/"idle"/"lisa_front_idle_000.png").rename(path)
        with self.assertRaisesRegex(AssetError,"Invalid filename|Mixed character identity"):
            build(self.root,self.root/"atlas")


if __name__ == "__main__":
    unittest.main()
