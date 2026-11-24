# Cloud image-generation pipeline

This workflow creates one PNG candidate at a time with Cloudflare Workers AI. It runs on GitHub Actions; it does not use the phone's CPU/GPU and does not depend on ChatGPT's built-in image-generation quota.

## One-time setup

1. Create or sign in to a Cloudflare account and enable Workers AI.
2. In the Cloudflare dashboard, copy the account ID.
3. Create an API token limited to the account's Workers AI access. Do not use a global API key.
4. In this GitHub repository, open **Settings → Secrets and variables → Actions** and add:
   - `CLOUDFLARE_ACCOUNT_ID`
   - `CLOUDFLARE_API_TOKEN`
5. Never paste either secret into chat, commit it, or place it in a workflow input.

Cloudflare currently documents a free Workers AI allocation of 10,000 Neurons per day. Actual image count depends on model usage; requests can fail when the daily allocation is exhausted. Check the Cloudflare dashboard before repeated batches.

## Generate one candidate

1. Open **Actions → Generate asset candidate → Run workflow**.
2. Select branch `development/0.7.0`.
3. Keep the first test to one image. Use a simple asset slug such as `oak-floor-test`.
4. Enter a prompt describing exactly one image.
5. Wait for the workflow to finish.
6. Download the `rift-asset-candidate-...` artifact to inspect the PNG and JSON metadata.

The workflow also commits the candidate under:

`app/src/main/assets/generated/candidates/`

Candidates are deliberately isolated from production runtime asset paths. A successful API request and file validation do not mean the asset is visually approved, seamless, PBR-complete, or integrated into a scene. Promote it only after visual review, material validation (where applicable), and an in-game check.

## Automated checks and limitations

The generator checks API response shape, base64 decoding, image decoding, minimum 512×512 dimensions, PNG encoding, and final PNG readability. It records the model, prompt, seed, dimensions, timestamp, and review status in a sidecar JSON file.

It does not reliably detect malformed anatomy, bad composition, visible seams, unwanted text, or style drift. Those require visual inspection. The current FLUX endpoint returns its own image dimensions; the script does not claim it can force 2048×2048 output. Upscaling is a separate operation and cannot restore missing detail.

## Local smoke test

With Pillow installed and the two environment variables configured:

```bash
python -m pip install Pillow
python tools/assets/generate_cloudflare_image.py \
  --prompt "A single seamless oak floor texture, top-down, no objects" \
  --output app/src/main/assets/generated/candidates/oak-floor-test.png
```
