#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Usage: bash scripts/prepare-unity-port.sh /path/to/SanAndreasUnity
# Copies only this repo's original overlay; does NOT copy third-party game data.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="${1:-}"
if [[ -z "$PROJECT" || ! -f "$PROJECT/ProjectSettings/ProjectVersion.txt" || ! -d "$PROJECT/Assets" ]]; then
  echo "Usage: bash scripts/prepare-unity-port.sh /path/to/SanAndreasUnity" >&2
  exit 2
fi
if ! grep -q '2020.3.26f1' "$PROJECT/ProjectSettings/ProjectVersion.txt"; then
  echo "Warning: upstream Unity version differs from verified 2020.3.26f1; verify API compatibility." >&2
fi
if [[ -e "$PROJECT/Assets/Scripts/Performance/MobilePerformanceGovernor.cs" ]]; then
  echo "Target governor already exists; refusing to overwrite without review." >&2
  exit 3
fi
mkdir -p "$PROJECT/Assets/Scripts/Performance" "$PROJECT/Assets/Editor" "$PROJECT/Assets/Scripts/Importing/Conversion"
cp "$ROOT/UnityPort/Assets/Scripts/Performance/MobilePerformanceGovernor.cs" "$PROJECT/Assets/Scripts/Performance/"
cp "$ROOT/UnityPort/Assets/Scripts/Performance/MobilePerformanceBootstrap.cs" "$PROJECT/Assets/Scripts/Performance/"
cp "$ROOT/UnityPort/Assets/Editor/SAAndroidBuild.cs" "$PROJECT/Assets/Editor/"
cp "$ROOT/UnityPort/Assets/Scripts/Importing/Conversion/MobileDxtDecoder.cs" "$PROJECT/Assets/Scripts/Importing/Conversion/"
python3 "$ROOT/scripts/patch_upstream.py" "$PROJECT"
echo "Installed Android overlay in $PROJECT"
echo "Open with Unity 2020.3.26f1, install Android Build Support + IL2CPP and build via SAAndroidBuild.BuildArm64."
