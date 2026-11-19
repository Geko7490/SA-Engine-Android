# Android overlay: reproducible source integration

Source: https://github.com/in0finite/SanAndreasUnity (MIT). Verified Unity version is `2020.3.26f1`; upstream has five Git submodules. The project does not yet contain an upstream Unity checkout; it contains a minimal overlay.

## Prepare a checkout (PC or CI runner with Unity installed)
```bash
git clone --recurse-submodules https://github.com/in0finite/SanAndreasUnity.git
bash scripts/prepare-unity-port.sh ./SanAndreasUnity
```
This repository's script must be run from the SA-Engine-Android checkout.

1. Install Unity 2020.3.26f1 plus Android Build Support, SDK/NDK and OpenJDK.
2. Open the upstream checkout and verify its startup scene and third-party dependencies.
3. Build with `SAAndroidBuild.BuildArm64` from the Unity Editor or `-executeMethod` in batchmode.
4. For batchmode, a licensed and activated Unity Editor is required. `SA_ANDROID_APK_PATH` overrides the APK output filename.
5. On the phone, supply your own legally obtained GTA SA PC files as required by the upstream implementation.

## Included overlay
- `MobilePerformanceGovernor.cs` — requested frame rate cap (30/60/90/120), sampled frame time and optional shadow governor.
- `MobilePerformanceBootstrap.cs` — installs the governor automatically on Android after a scene loads.
- `SAAndroidBuild.cs` — Android ARM64 + IL2CPP development build entry point; uses enabled build scenes, or Startup.unity as fallback.

## Known limitations
This is **not a complete Android game**; overlay scripts are uncompiled against the upstream project here, and no APK has been produced.
Unity version/API and upstream submodules must be validated in a Unity installation.
The current automatic bootstrap runs only in Android player builds, not Editor.
The script deliberately does not copy upstream assets into this repo. Any distribution of MIT-licensed upstream code must preserve license and copyright notices.
A target FPS request is not an FPS guarantee. Evaluate baseline vs overlay with a device profiler.
Android API level, Unity build infrastructure, storage access, touch input and renderer support may need further work.
