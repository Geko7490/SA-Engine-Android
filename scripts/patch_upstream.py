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


# Android Mali/Adreno devices do not universally expose S3TC texture support.
# Preserve the compressed path when supported; decode to RGBA32 only as fallback.
TEXTURE_CREATE = "var tex = new Texture2D(src.Width, src.Height, format, false /*loadMips*/);"
TEXTURE_PREP = """bool needsDxtFallback = Application.platform == RuntimePlatform.Android &&
                ((src.Compression == CompressionMode.DXT1 && !SystemInfo.SupportsTextureFormat(TextureFormat.DXT1)) ||
                 (src.Compression == CompressionMode.DXT3 && !SystemInfo.SupportsTextureFormat(TextureFormat.DXT5)));
            if (needsDxtFallback)
                format = TextureFormat.RGBA32;

            var tex = new Texture2D(src.Width, src.Height, format, false /*loadMips*/);"""
TEXTURE_DATA = "var data = src.ImageData;"
TEXTURE_FALLBACK = """var data = src.ImageData;
            if (needsDxtFallback)
            {
                data = src.Compression == CompressionMode.DXT1
                    ? MobileDxtDecoder.DecodeDxt1(src.ImageData, src.Width, src.Height)
                    : MobileDxtDecoder.DecodeDxt3(src.ImageData, src.Width, src.Height);
            }
            else
            {"""
TEXTURE_APPLY = "tex.LoadRawTextureData(data);"
TEXTURE_APPLY_REPLACEMENT = "}\n\n            tex.LoadRawTextureData(data);"

def patch_textures(root: Path) -> str:
    src = root / "Assets/Scripts/Importing/Conversion/TextureDictionary.cs"
    if not src.is_file():
        raise FileNotFoundError(f"Missing upstream TextureDictionary: {src}")
    text = src.read_text(encoding="utf-8-sig").replace("\r\n", "\n")
    if text.count(TEXTURE_PREP) == 1 and text.count(TEXTURE_FALLBACK) == 1:
        return "already-patched"
    for anchor in (TEXTURE_CREATE, TEXTURE_DATA, TEXTURE_APPLY):
        if text.count(anchor) != 1:
            raise RuntimeError(f"TextureDictionary layout changed: expected exactly one {anchor!r}")
    text = text.replace(TEXTURE_CREATE, TEXTURE_PREP, 1)
    text = text.replace(TEXTURE_DATA, TEXTURE_FALLBACK, 1)
    text = text.replace(TEXTURE_APPLY, TEXTURE_APPLY_REPLACEMENT, 1)
    src.write_text(text, encoding="utf-8", newline="\n")
    return "patched"


LOADER_PATH = "string path = Config.GetPath(Config.const_game_dir);"
LOADER_ANDROID_PATH = """string path = Config.GetPath(Config.const_game_dir);

            // Android: load legally owned GTA SA files from the app's own storage.
            // The standard desktop folder browser cannot reliably access scoped storage.
            if (Application.platform == RuntimePlatform.Android && string.IsNullOrEmpty(path))
            {
                string localFolder = Path.Combine(Application.persistentDataPath, "GTASA");
                string pathError;
                if (IsGamePathCorrect(localFolder, out pathError))
                {
                    Config.SetString(Config.const_game_dir, localFolder);
                    Config.SaveUserConfigSafe();
                    path = localFolder;
                }
            }"""

LOADER_CORO = "private static IEnumerator StepSelectGTAPath ()\\n\\t\\t{\\n\\t\\t\\tyield return null;"
LOADER_BRIDGE = """private static IEnumerator StepSelectGTAPath ()
        {
            yield return null;
#if UNITY_ANDROID && !UNITY_EDITOR
            SAEngineAndroid.Performance.GameDataBridge.Begin();
            while (SAEngineAndroid.Performance.GameDataBridge.IsBusy)
                yield return null;
            string bridgeError = SAEngineAndroid.Performance.GameDataBridge.Error;
            if (!string.IsNullOrEmpty(bridgeError))
                throw new System.Exception("Game data transfer failed: " + bridgeError);
#endif"""

def patch_loader(root: Path) -> str:
    src = root / "Assets/Scripts/Behaviours/Loader.cs"
    if not src.is_file():
        raise FileNotFoundError(f"Missing upstream Loader source: {src}")
    content = src.read_text(encoding="utf-8-sig").replace("\\r\\n", "\\n")
    changed = False
    if content.count(LOADER_ANDROID_PATH) != 1:
        if content.count(LOADER_PATH) != 1:
            raise RuntimeError("Loader path selection changed upstream; refusing blind patch")
        content = content.replace(LOADER_PATH, LOADER_ANDROID_PATH, 1)
        changed = True
    if content.count(LOADER_BRIDGE) != 1:
        if content.count(LOADER_CORO) != 1:
            raise RuntimeError("Loader coroutine signature changed upstream; refusing bridge patch")
        content = content.replace(LOADER_CORO, LOADER_BRIDGE, 1)
        changed = True
    if changed:
        src.write_text(content, encoding="utf-8", newline="\\n")
    return "patched" if changed else "already-patched"

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("project", type=Path)
    args = parser.parse_args()
    try:
        print("Android FPS patch:", patch_project(args.project))
        print("Android DXT patch:", patch_textures(args.project))
        print("Android game data path patch:", patch_loader(args.project))
    except (FileNotFoundError, RuntimeError) as exc:
        print("ERROR:", exc, file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
