#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Clone a known upstream snapshot, with its Git submodules.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-SanAndreasUnity}"
REV="$(tr -d '\r\n' < "$ROOT/upstream.lock")"
if [[ ! "$REV" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid upstream.lock SHA" >&2
  exit 2
fi
if [[ -e "$TARGET" ]]; then
  echo "Refusing to replace existing target: $TARGET" >&2
  exit 3
fi
git clone --filter=blob:none --no-checkout https://github.com/in0finite/SanAndreasUnity.git "$TARGET"
git -C "$TARGET" checkout --detach "$REV"
git -C "$TARGET" submodule update --init --recursive --depth 1
actual="$(git -C "$TARGET" rev-parse HEAD)"
test "$actual" = "$REV"
test -f "$TARGET/LICENSE"
test -f "$TARGET/Assets/Scenes/Startup.unity"
echo "Checked out SanAndreasUnity at $actual"
