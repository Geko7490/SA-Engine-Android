# SA Engine Android — Mobile launcher and Unity engine port

**Status (2026-11-19): V0.5 Android launcher APK is available. V0.6 playable Unity GTA SA game APK is not yet built; Unity activation credentials are missing.**

[Download launcher APK](https://github.com/Geko7490/SA-Engine-Android/releases/tag/launcher-dev) · [V0.2–V0.6 status and usage](docs/V0.2-V0.6_STATUS.md) · [GitHub Actions](https://github.com/Geko7490/SA-Engine-Android/actions)

This project is an experimental performance-oriented Android port overlay for the MIT-licensed [SanAndreasUnity](https://github.com/in0finite/SanAndreasUnity). It is not a complete GTA San Andreas redistribution or a new standalone Rust game engine.

## Implemented
- Android ARM64 / IL2CPP Unity 2020.3.26f1 build script and GitHub Actions pipeline.
- Pinned upstream checkout with Git submodules; guarded, repeatable upstream compatibility patches.
- Fix the upstream 25 FPS mobile override; persistent 30/60/90/120 FPS target selection.
- LOW/MED/HIGH graphics profiles, adaptive draw distance and per-frame world-cell update budget.
- Optional DXT1/DXT3 software fallback on Android GPUs lacking required compressed-texture support.
- Development-only touch panel and 60-second frame-time CSV benchmark recorder.
- Android game directory auto-detection for the user's own PC GTA SA files under `Application.persistentDataPath/GTASA`.
- GitHub Actions source preflight: Python integration tests and actual C# compilation against minimal Unity API stubs; known DXT pixel/bounds test cases.

## Get started

**Source preflight:** Push to `main` triggers the [Android Unity Port workflow](.github/workflows/android-unity.yml) with `build_apk=false` behaviour. Source checks are automated and do not require a Unity license.

**APK attempt:** Use GitHub Actions → Android Unity Port → Run workflow → `build_apk=true`. This uses GameCI, which requires the repository owner to configure Unity activation credentials as Actions secrets, plus working Android tooling. The CI attempt may still uncover actual Unity compilation errors.

**Manual preparation:**
```bash
bash scripts/checkout-upstream.sh SanAndreasUnity
bash scripts/prepare-unity-port.sh SanAndreasUnity
```
Then open `SanAndreasUnity` with Unity 2020.3.26f1 and invoke `SAAndroidBuild.BuildArm64` with Android Build Support installed.

For Android file placement, device benchmarking, limits, and planned HD graphics packs, read [Device & Benchmark](docs/DEVICE_AND_BENCHMARK.md). For more detail, see [Build Guide](docs/ANDROID_BUILD.md) and [CI Notes](docs/GITHUB_ACTIONS.md).

## Legal
No Rockstar or Take-Two game data is shipped. A legally obtained GTA San Andreas PC installation is required for gameplay. This project is unofficial and not affiliated with Rockstar Games.

The original code in this repository is MIT licensed (see LICENSE). Upstream SanAndreasUnity is MIT-licensed and retains its own copyright notice. Git submodules must comply with their own licenses. A previous, separate Rust candidate `ghosty2004/sa-engine` declares itself `UNLICENSED`, so its code has not been imported here.

## Not yet implemented or proven
- No Unity Android build completed; no installable APK validated.
- No device FPS, thermal, loading-time or memory benchmark measured.
- No modern Android Storage Access Framework game-folder/ZIP importer yet.
- No HD graphics pack loader, texture streaming allocator, or Vulkan-specific optimization.
- Touch controls are provided by the existing SanAndreasUnity project, not rewritten by this overlay.

Next engineering step: compile the real Unity project, resolve any Android plug-in/SDK errors, install on device, and compare repeatable in-game 60-second baseline measurements before claiming any performance gain.
