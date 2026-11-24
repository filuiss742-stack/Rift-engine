import base64
import json
import os
import sys
import tempfile
import unittest
from io import BytesIO
from pathlib import Path
from unittest import mock

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
import generate_cloudflare_image as generator


class FakeResponse:
    def __init__(self, payload):
        self.payload = json.dumps(payload).encode("utf-8")

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self):
        return self.payload


def encoded_png(size=(512, 512)):
    buffer = BytesIO()
    Image.new("RGB", size, (90, 110, 130)).save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode("ascii")


class CloudflareImageGeneratorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.output = Path(self.temp.name) / "candidate.png"
        self.argv = [
            "generate_cloudflare_image.py",
            "--prompt", "One seamless stone texture",
            "--output", str(self.output),
            "--seed", "7",
            "--steps", "4",
        ]
        self.env = {
            "CLOUDFLARE_ACCOUNT_ID": "test-account",
            "CLOUDFLARE_API_TOKEN": "test-token",
        }

    def tearDown(self):
        self.temp.cleanup()

    def run_main(self, response=None, env=None):
        with mock.patch.object(sys, "argv", self.argv), \
             mock.patch.dict(os.environ, self.env if env is None else env, clear=True), \
             mock.patch.object(generator.urllib.request, "urlopen") as urlopen:
            if response is not None:
                urlopen.return_value = FakeResponse(response)
            result = generator.main()
            return result, urlopen

    def test_missing_credentials_fails_before_network_request(self):
        with self.assertRaises(SystemExit) as raised:
            self.run_main(env={})
        self.assertEqual(1, raised.exception.code)

    def test_rejects_unexpected_api_response(self):
        with self.assertRaises(SystemExit) as raised:
            self.run_main({"success": True, "result": {}})
        self.assertEqual(1, raised.exception.code)

    def test_rejects_invalid_base64(self):
        with self.assertRaises(SystemExit) as raised:
            self.run_main({"result": {"image": "not base64!"}})
        self.assertEqual(1, raised.exception.code)

    def test_rejects_image_below_minimum_dimensions(self):
        with self.assertRaises(SystemExit) as raised:
            self.run_main({"result": {"image": encoded_png((256, 256))}})
        self.assertEqual(1, raised.exception.code)

    def test_writes_verified_candidate_and_review_metadata(self):
        result, urlopen = self.run_main({"result": {"image": encoded_png()}})
        self.assertEqual(0, result)
        urlopen.assert_called_once()
        with Image.open(self.output) as image:
            self.assertEqual((512, 512), image.size)
            self.assertEqual("PNG", image.format)
            image.verify()
        metadata = json.loads(self.output.with_suffix(".json").read_text(encoding="utf-8"))
        self.assertEqual("candidate_requires_visual_review", metadata["status"])
        self.assertFalse(metadata["automated_checks"]["visual_quality_reviewed"])
        self.assertFalse(metadata["automated_checks"]["runtime_integration_verified"])


if __name__ == "__main__":
    unittest.main()
