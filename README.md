# SA Engine Mobile — native GTA SA Android FPS toolkit

**Current status (19 Nov 2026): Android launcher APK and ARM64 native FPS plugin compile and publish successfully. They are not a complete GTA San Andreas game APK. Real on-device 60/120 FPS gameplay has NOT been verified.**

## Download the built components

- [SA Engine Mobile Launcher Android APK](https://github.com/Geko7490/SA-Engine-Android/releases/tag/launcher-dev)
- [Native GTA SA FPS plugin (.so)](https://github.com/Geko7490/SA-Engine-Android/releases/tag/native-fps-dev)
- [Complete native setup, limitations, and licensed game requirements](docs/NATIVE_GTA_SA_FPS.md)
- [GitHub Actions builds and automated tests](https://github.com/Geko7490/SA-Engine-Android/actions)

The GitHub repository is currently private, so release downloads require an account with repository access.

## What works at the source/build level

**Launcher** (Java / Android / Gradle; no Unity):
- Finds the legally installed `com.rockstargames.gtasa` application, displays its version and launches it.
- Selectable **30 / 60 / 90 / 120 FPS**, plus low/medium/high quality preferences.
- Verified GitHub Release catalogue and resumable SHA-256-checked package downloads.
- Installs the project-owned ARM64 FPS plugin into a **user-approved** AML mods folder using Android Storage Access Framework (including `AMLMods/com.rockstargames.gtasa`).
- Preserves the ZIP/folder importer for user's own GTA files.
- Stores the selected FPS in `SAEngineFPS.cfg` beside the plugin for use when GTA SA is started.
- Keeps experimental original Unity-based engine launcher options separated from the official-game path.

**Native FPS module** (NDK C++ original code, MIT):
- Runs only after user has installed a compatible, *AML-enabled* Android GTA SA with required SAUtils mod.
- Runtime guards for two known Android 2.10 ARM64 frame-rate patch locations, and three experimental Android 2.11 ARM64 locations.
- Refuses to patch unknown game binary layouts. Does **not** disable signature checks, provide official game binaries or bypass Android security.
- Adds a **SA Engine FPS Limit** in-game menu entry when SAUtils loads successfully.
- Target FPS is **not** guaranteed actual performance; physics, animations, UI and mission timing require on-device testing.

The native module is NOT a standalone executable and the launcher is NOT GTA San Andreas.

## Prerequisites for actual GTA gameplay

1. User-owned licensed **GTA San Andreas for Android**, with an appropriate version (2.10 ARM64 or experimental 2.11).
2. [AndroidModLoader](https://github.com/AndroidModLoader/AndroidModLoader) and [SAUtils](https://github.com/AndroidModLoader/SAUtils) installed into **the user's game** through their supported installation process. The stock Play Store game cannot load AML extensions by itself.
3. User consents to install this project-owned `libSAEngineFPS.so` into the accessible AML mods folder, then launches the GTA SA app.

Never install an untrusted modified game APK; obtain the game through legitimate channels. Neither Rockstar's APK nor `libGTASA.so` or copyrighted assets may be mirrored in this repository without permission.

## Build/test status

- Android launcher: GitHub Actions Gradle build plus Java JUnit catalogue, safety, and FPS validation tests.
- Native module: GitHub Actions Android NDK ARM64 build plus C++ FPS instruction tests.
- Unity project: source overlay exists under `UnityPort/`; **not built as a playable game**, because Unity Editor activation and device validation remain unsatisfied. Unity Actions CI is manual to avoid unnecessary builds.
- No real smartphone gameplay validation, measured FPS improvement, or verified 120 FPS cap on a real GTA SA installation yet.

Native code is offered as an **experimental mod for a legally owned game**, not a pirated game replacement.

## Licenses and attribution

Our original launcher and FPS module are MIT-licensed. AndroidModLoader and SAUtils have their own MIT notices and remain linked as external build dependencies. `JPatch` is proprietary freeware; it is not copied or redistributed. SanAndreasUnity is MIT-licensed; original GTA SA assets/engine are copyrighted to their respective owners. This is an independent, unofficial project.
