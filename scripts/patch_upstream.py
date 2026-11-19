#!/usr/bin/env python3
"""Apply narrow, auditable Android-only upstream compatibility fixes.

The unmodified SanAndreasUnity GameManager sets a serialized default of
25 FPS on mobile every time Awake() runs. This defeats the Android governor.
Use the stored user target instead, without touching desktop defaults.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import sys

EXPECTED = "if (Application.isMobilePlatform)\n\t\t\t\tSetMaxFps(m_defaultMaxFpsOnMobile);"
REPLACEMENT = (
    "if (Application.isMobilePlatform)\n"
    "\t\t\t\tSetMaxFps(Mathf.Clamp(PlayerPrefs.GetInt("
    "\"SA_ANDROID_TARGET_FPS\", 60), 30, 120));"
)

def patch_project(root: Path) -> str:
    src = root / "Assets/Scripts/Behaviours/GameManager.cs"
    if not src.is_file():
        raise FileNotFoundError(f"Required upstream source missing: {src}")
    text = src.read_text(encoding="utf-8-sig")
    normalized = text.replace("\r\n", "\n")
    if normalized.count(REPLACEMENT) == 1 and EXPECTED not in normalized:
        return "already-patched"
    count = normalized.count(EXPECTED)
    if count != 1:
        raise RuntimeError(f"Expected exactly one mobile FPS assignment; found {count}. Review upstream changes.")
    normalized = normalized.replace(EXPECTED, REPLACEMENT, 1)
    src.write_text(normalized, encoding="utf-8", newline="\n")
    return "patched"

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("project", type=Path)
    args = parser.parse_args()
    try:
        print("Android FPS patch:", patch_project(args.project))
    except (FileNotFoundError, RuntimeError) as exc:
        print("ERROR:", exc, file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
