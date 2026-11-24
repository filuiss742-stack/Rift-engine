#!/usr/bin/env python3
"""Generate one image candidate with Cloudflare Workers AI and validate the file."""
from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image


MODEL = "@cf/black-forest-labs/flux-1-schnell"
API_ROOT = "https://api.cloudflare.com/client/v4/accounts"


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prompt", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--seed", type=int, default=314159)
    parser.add_argument("--steps", type=int, default=4, choices=range(4, 9))
    args = parser.parse_args()

    if not args.prompt.strip() or len(args.prompt) > 2048:
        fail("Prompt must contain 1–2048 characters.")
    if args.output.suffix.lower() != ".png":
        fail("Output must use the .png extension.")

    account_id = os.environ.get("CLOUDFLARE_ACCOUNT_ID", "").strip()
    api_token = os.environ.get("CLOUDFLARE_API_TOKEN", "").strip()
    if not account_id or not api_token:
        fail("Missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN secret.")

    url = f"{API_ROOT}/{account_id}/ai/run/{MODEL}"
    payload = json.dumps({
        "prompt": args.prompt,
        "seed": args.seed,
        "steps": args.steps,
    }).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Authorization": f"Bearer {api_token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "RiftEngine-AssetPipeline/1.0",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            result = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read(2000).decode("utf-8", errors="replace")
        fail(f"Cloudflare API returned HTTP {exc.code}: {body}")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        fail(f"Cloudflare request failed: {exc}")

    image_b64 = (result.get("result") or {}).get("image")
    if not isinstance(image_b64, str) or not image_b64:
        fail(f"Cloudflare response did not contain result.image: {json.dumps(result)[:1500]}")

    try:
        image_bytes = base64.b64decode(image_b64, validate=True)
    except (ValueError, base64.binascii.Error) as exc:
        fail(f"Image payload is not valid base64: {exc}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(".download")
    temporary.write_bytes(image_bytes)
    try:
        with Image.open(temporary) as source:
            source.verify()
        with Image.open(temporary) as source:
            source.load()
            width, height = source.size
            if width < 512 or height < 512:
                fail(f"Generated image is unexpectedly small: {width}x{height}.")
            source.convert("RGB").save(args.output, format="PNG", optimize=True)
    except Exception as exc:
        fail(f"Generated image failed Pillow decoding/validation: {exc}")
    finally:
        temporary.unlink(missing_ok=True)

    with Image.open(args.output) as final_image:
        final_image.verify()
    metadata = {
        "schema_version": 1,
        "status": "candidate_requires_visual_review",
        "provider": "Cloudflare Workers AI",
        "model": MODEL,
        "prompt": args.prompt,
        "seed": args.seed,
        "steps": args.steps,
        "file": args.output.name,
        "format": "PNG",
        "width": width,
        "height": height,
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "automated_checks": {
            "base64_decoded": True,
            "source_image_decoded": True,
            "png_written_and_verified": True,
            "visual_quality_reviewed": False,
            "seamless_tile_verified": False,
            "runtime_integration_verified": False,
        },
    }
    args.output.with_suffix(".json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Generated candidate: {args.output} ({width}x{height})")
    print("Automated file checks passed; visual and in-game review are still required.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
