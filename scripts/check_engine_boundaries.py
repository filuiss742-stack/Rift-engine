#!/usr/bin/env python3
"""Fail when platform-specific imports leak into the shared engine source."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
ENGINE_SOURCE = ROOT / "engine" / "src"
FORBIDDEN = re.compile(
    r"^\s*import\s+(?:android\.|java\.awt\.|javax\.swing\.)",
    re.MULTILINE,
)

violations = []
for source in ENGINE_SOURCE.rglob("*"):
    if source.suffix not in {".kt", ".java"} or not source.is_file():
        continue
    try:
        content = source.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue
    if FORBIDDEN.search(content):
        violations.append(source.relative_to(ROOT).as_posix())

if violations:
    print("Platform-specific imports found inside :engine:")
    for path in violations:
        print(f" - {path}")
    sys.exit(1)

print("Engine boundary check passed: no Android, AWT, or Swing imports in :engine.")
