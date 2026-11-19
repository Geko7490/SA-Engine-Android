#!/usr/bin/env python3
"""No-Unity source preflight tests. These do NOT prove Unity compilation."""
from pathlib import Path
from tempfile import TemporaryDirectory
import importlib.util
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("upstream_patch", ROOT / "scripts/patch_upstream.py")
patcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patcher)

class AndroidOverlayTests(unittest.TestCase):
    def test_upstream_patch_is_targeted_and_idempotent(self):
        with TemporaryDirectory() as temp:
            path = Path(temp) / "Assets/Scripts/Behaviours/GameManager.cs"
            path.parent.mkdir(parents=True)
            path.write_text("class GameManager {\n" + patcher.EXPECTED + "\n}", encoding="utf-8")
            self.assertEqual(patcher.patch_project(Path(temp)), "patched")
            updated = path.read_text()
            self.assertIn(patcher.REPLACEMENT, updated)
            self.assertNotIn(patcher.EXPECTED, updated)
            self.assertEqual(patcher.patch_project(Path(temp)), "already-patched")

    def test_unknown_upstream_change_fails_closed(self):
        with TemporaryDirectory() as temp:
            path = Path(temp) / "Assets/Scripts/Behaviours/GameManager.cs"
            path.parent.mkdir(parents=True)
            path.write_text("class GameManager {}", encoding="utf-8")
            with self.assertRaises(RuntimeError):
                patcher.patch_project(Path(temp))

    def test_overlay_files_exist_and_implement_expected_contract(self):
        base = ROOT / "UnityPort/Assets"
        governor = (base / "Scripts/Performance/MobilePerformanceGovernor.cs").read_text()
        self.assertIn("SetTargetFps", governor)
        self.assertIn("Application.targetFrameRate", governor)
        self.assertIn("SA_ANDROID_TARGET_FPS", governor)
        bootstrap = (base / "Scripts/Performance/MobilePerformanceBootstrap.cs").read_text()
        self.assertIn("RuntimeInitializeOnLoadMethod", bootstrap)
        self.assertIn("MobilePerformanceGovernor", bootstrap)
        self.assertIn("MobileWorldTuner", bootstrap)
        self.assertIn("MobilePerformancePanel", bootstrap)
        tuner = (base / "Scripts/Performance/MobileWorldTuner.cs").read_text()
        self.assertIn("WorldManager.Singleton", tuner)
        self.assertIn("maxTimeToUpdatePerFrameMs", tuner)
        self.assertIn("SA_ANDROID_QUALITY", tuner)
        panel = (base / "Scripts/Performance/MobilePerformancePanel.cs").read_text()
        self.assertIn("Debug.isDebugBuild", panel)
        builder = (base / "Editor/SAAndroidBuild.cs").read_text()
        self.assertIn("AndroidArchitecture.ARM64", builder)
        self.assertIn("BuildPipeline.BuildPlayer", builder)
        self.assertIn("com.geko7490.saengineandroid", builder)

    def test_mobile_dxt_guarded_patch(self):
        with TemporaryDirectory() as temp:
            path = Path(temp) / "Assets/Scripts/Importing/Conversion/TextureDictionary.cs"
            path.parent.mkdir(parents=True)
            path.write_text("\\n".join([patcher.TEXTURE_CREATE, patcher.TEXTURE_DATA, patcher.TEXTURE_APPLY]), encoding="utf-8")
            self.assertEqual(patcher.patch_textures(Path(temp)), "patched")
            text = path.read_text()
            self.assertIn(patcher.TEXTURE_PREP, text)
            self.assertIn(patcher.TEXTURE_FALLBACK, text)
            self.assertIn(patcher.TEXTURE_APPLY_REPLACEMENT, text)
            self.assertEqual(patcher.patch_textures(Path(temp)), "already-patched")

    def test_android_owned_game_data_folder_patch(self):
        with TemporaryDirectory() as temp:
            path = Path(temp) / "Assets/Scripts/Behaviours/Loader.cs"
            path.parent.mkdir(parents=True)
            path.write_text("class Loader {\\n" + patcher.LOADER_PATH + "\\n}", encoding="utf-8")
            self.assertEqual(patcher.patch_loader(Path(temp)), "patched")
            updated = path.read_text()
            self.assertIn(patcher.LOADER_ANDROID_PATH, updated)
            self.assertEqual(patcher.patch_loader(Path(temp)), "already-patched")

    def test_preparation_script_contains_safe_patch(self):
        text = (ROOT / "scripts/prepare-unity-port.sh").read_text()
        self.assertIn("patch_upstream.py", text)
        self.assertIn("MobilePerformanceBootstrap.cs", text)
        self.assertIn("MobileDxtDecoder.cs", text)
        self.assertIn("MobileWorldTuner.cs", text)
        self.assertIn("MobilePerformancePanel.cs", text)
        decoder = (ROOT / "UnityPort/Assets/Scripts/Importing/Conversion/MobileDxtDecoder.cs").read_text()
        self.assertIn("DecodeDxt1", decoder)
        self.assertIn("DecodeDxt3", decoder)

if __name__ == "__main__":
    unittest.main()
