#!/usr/bin/env bash
set -euo pipefail

# Download real, photographed CC0 PBR base-colour maps from Poly Haven.
# These are source images, not procedural/noise-generated textures.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="${TMPDIR:-/tmp}/rift-cc0-materials"
mkdir -p "$TMP"

BASE="https://dl.polyhaven.org/file/ph-assets/Textures/png/1k"
fetch() {
  local asset="$1"
  local filename="$2"
  local map="$3"
  local url="$BASE/$asset/${asset}_${map}_1k.png"
  curl --fail --location --retry 3 --silent --show-error "$url" --output "$TMP/$filename"
  local signature
  signature="$(od -An -tx1 -N8 "$TMP/$filename" | tr -d ' \n')"
  if [[ "$signature" != "89504e470d0a1a0a" ]]; then
    echo "Downloaded file is not a valid PNG: $url" >&2
    exit 1
  fi
  echo "Fetched $asset -> $filename ($(wc -c < "$TMP/$filename") bytes)"
}

fetch "asphalt_02" "asphalt_02.png" "diff"
fetch "brick_wall_001" "brick_wall_001.png" "diffuse"
fetch "concrete_wall_008" "concrete_wall_008.png" "diff"
fetch "wood_floor" "wood_floor.png" "diff"
fetch "stone_wall" "stone_wall.png" "diffuse"
fetch "mossy_rock" "mossy_rock.png" "diff"

for destination in \
  "$ROOT/app/src/main/assets/prologue/materials" \
  "$ROOT/desktop-app/src/main/resources/prologue/materials"; do
  mkdir -p "$destination"
  cp "$TMP"/*.png "$destination/"
  cat > "$destination/CC0_SOURCES.txt" <<'CREDITS'
Photographic PBR base-colour textures downloaded from Poly Haven (https://polyhaven.com).
All four source materials are marked CC0 on their asset pages:
- asphalt_02: https://polyhaven.com/a/asphalt_02
- brick_wall_001: https://polyhaven.com/a/brick_wall_001
- concrete_wall_008: https://polyhaven.com/a/concrete_wall_008
- wood_floor: https://polyhaven.com/a/wood_floor
- stone_wall: https://polyhaven.com/a/stone_wall
- mossy_rock: https://polyhaven.com/a/mossy_rock
CREDITS
done

# Keep the existing heroine placeholder available to the Desktop runtime too.
HEROINE_SOURCE="$ROOT/app/src/main/assets/prologue/characters/heroine/heroine_idle.png"
HEROINE_DEST="$ROOT/desktop-app/src/main/resources/prologue/characters/heroine"
if [[ -f "$HEROINE_SOURCE" ]]; then
  mkdir -p "$HEROINE_DEST"
  cp "$HEROINE_SOURCE" "$HEROINE_DEST/heroine_idle.png"
else
  echo "Missing source heroine sprite: $HEROINE_SOURCE" >&2
  exit 1
fi

echo \\"Installed six real 1K PNG material maps for Android and Desktop.\\"
