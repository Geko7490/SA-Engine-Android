# SanAndreasUnity Android performance baseline

## Verified upstream facts (2026-11-19)
- Source: https://github.com/in0finite/SanAndreasUnity (default branch: dev).
- Upstream README states Android is a tested target and requires the user's own PC GTA SA game files.
- Upstream LICENSE is MIT, copyright (c) 2015 James King. Keep its full license notice when importing or redistributing upstream source.
- Upstream recommends Unity 2020.3.26 or newer.
- This repository is **not yet an imported Unity project**. No game code, scenes, or playable APK are included.

## Initial implementation
`UnityPort/Assets/Scripts/Performance/MobilePerformanceGovernor.cs` is an original, optional Unity MonoBehaviour:
- Selects 30/60/90/120 requested frame rate.
- Reports sampled FPS and average frame time (not actual GPU time).
- Optionally adjusts shadow distance using hysteresis.
- Restores saved global settings on disable.

### Integrate
1. Clone SanAndreasUnity WITH submodules, in a properly licensed working tree.
2. Copy the governor script into its Unity Assets/Scripts folder.
3. Attach to one GameObject in the startup/game scene; ensure only one instance exists.
4. Build a Development APK with Android ARM64 + IL2CPP, after confirming target Unity version and plugin compatibility.
5. Profile on actual Android device. Compare matched 60-second runs in identical locations, camera angles, traffic density and quality settings.

### Important constraints
- `Application.targetFrameRate` only requests a cap; display refresh, thermals and CPU/GPU limits still apply.
- Shadow distance adaptation cannot cure CPU/main-thread, I/O or shader compilation stalls.
- This script does not implement GPU dynamic resolution or Vulkan by itself.
- For later HD texture/mod support, keep file loading, texture budget and graphics settings as separate layers.
- Do not bundle Rockstar game assets, game binaries or proprietary textures.

## Performance acceptance criteria (to measure, not assumed)
- Median and 1% low frame rate in a reproducible scene
- Main-thread and render-thread frame time via Unity Profiler
- GPU frame time when GPU profiler is supported
- Peak managed/native RAM, loading spikes, thermal throttling
- Equivalent visual settings before/after optimization

## Next engineering step
Bring in the licensed Unity project, identify its world streaming/asset import/rendering hot paths, and make targeted changes based on profiler evidence.
