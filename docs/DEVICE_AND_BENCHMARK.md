# Android device setup, benchmarks, and release blockers

## What actually works in this repository
The Android overlay includes:
- persistent 30/60/90/120 FPS frame-rate selection;
- low/medium/high GPU and draw-distance profiles;
- target-FPS-aware per-frame world-cell update budget (trades possible pop-in for smoother frame pacing);
- DXT1/DXT3 software decoding fallback on GPUs without the required DXT support;
- development-only touch panel to change presets and trigger a 60-second frame-time benchmark;
- game directory auto-detection when user-provided game assets exist.

The source-preflight workflow clones pinned MIT-licensed SanAndreasUnity with its submodules, applies guarded patches, executes Python integration tests, and compiles the overlay against a **minimal .NET Unity API stub**, plus real pixel tests for the DXT decoder. A green preflight does **not** mean a Unity APK was built or tested.

## User-owned GTA San Andreas assets
No game data is included. The Windows PC game files need `models/`, `data/`, etc.
After installation of an actual APK, Unity's Android `Application.persistentDataPath` for this app should normally be similar to:

`/storage/emulated/0/Android/data/com.geko7490.saengineandroid/files/`

Place the legally obtained GTA SA PC installation in its `GTASA/` subfolder:
`.../files/GTASA/models/`, `.../files/GTASA/data/`, etc.
The patched loader auto-selects that folder when no prior game path was configured. Directory visibility and manual copying into `Android/data` may be restricted by Android 11+, so ADB or a future in-app Storage Access Framework importer may be necessary. **In-app ZIP importing is not implemented.**

## Touch FPS and graphics control
In a **development** Android build, tap the small `FPS` button at the screen edge to open the overlay. Choose 30/60/90/120 and LOW/MED/HIGH. Frame-rate target selection is persisted, but refresh rate, thermal conditions, engine load and Android frame-pacing can prevent reaching the requested FPS.

## 60-second performance CSV
Tap `BENCHMARK 60s` while in a fixed scene. After 60 seconds it writes `sa_benchmark_YYYYMMDD_HHMMSS.csv` in the app's persistent data folder. It contains:
- requested FPS, frames sampled, elapsed time;
- mean frame time, p95 and p99 frame times, a mean-FPS equivalent;
- each measured frame time in milliseconds.

For fair comparisons, use the same location, camera view, weather, vehicle count, and phone thermal state. The test measures Unity `unscaledDeltaTime`, **not GPU timing**. Do not interpret mean-FPS equivalent as exact 1% lows.

## Native APK build
The manual GitHub Actions `build_apk=true` job invokes the Unity 2020.3.26f1 editor via GameCI and requires Unity activation credentials configured by the repository owner. The Android build path is `SanAndreasUnity/Builds/SAEngineAndroid-dev.apk`.
This job is intentionally not executed on every push. Source code tests are performed automatically.
**An Android APK is not yet verified as built, installable, playable, or faster.** The source project has old Unity / target SDK settings; Unity activation and Android 16 compatibility need actual build and device testing.

## Future HD graphics packs
Asset streaming and adaptive draw distances should remain separate from HD texture loading. A proper graphics-pack loader must implement manifest validation, versioned cache keys, async decode, a memory budget and a safe fallback to original game files. It is not yet implemented.
