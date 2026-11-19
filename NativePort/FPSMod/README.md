# Native SA Engine FPS — AML Android plugin

Standalone original MIT source. No GTA executables or game assets are bundled.

- Target: **legally installed GTA: San Andreas Android v2.10 ARM64**, with **experimental v2.11 ARM64 support** if all opcode signatures match with AML and SAUtils >= 1.4.
- After loading, adds **SA Engine FPS Limit** in the game's own settings: 30 / 60 / 90 / 120 FPS.
- Persists the chosen target using AML Mod Local Storage.
- Checks both version-sensitive ARM64 instruction signatures before writing and refuses unrecognized layouts.
- A chosen cap is a **request**, not a promise of actual FPS, nor a full high-FPS physics fix. For gameplay timing fixes, link users to JPatch's official source; its proprietary-freeware license prohibits rehosting.
- Requires an already-operational AML-enabled installation of the **user's own** Android GTA SA; stock Play Store APKs do not magically load AML mods.
- No root or signature bypass code is distributed. We do not redistribute Rockstar binaries.

## Build
See [native-fps-plugin.yml](../../.github/workflows/native-fps-plugin.yml).
CI obtains the public MIT-licensed AML mod headers and SAUtils interface headers from pinned upstream Git commits. The NDK builds an ARM64 `libSAEngineFPS.so`. This does **not** create a GTA SA game APK.

## Install
Once the user's own AML-enabled GTA SA v2.10 ARM64 is present, place the compiled `libSAEngineFPS.so` in the GTA SA AML mods folder, usually `AMLMods/com.rockstargames.gtasa` or the game's `Android/data/.../mods` via the game's permitted storage location, restart the game and choose SA Engine FPS Limit in its Game settings (SAUtils required).

Actual runtime game compatibility must be verified on the user's device; compile success alone is not evidence of a working 120 FPS game.

**Dependencies / acknowledgment:** [AndroidModLoader](https://github.com/AndroidModLoader/AndroidModLoader) (MIT), [SAUtils](https://github.com/AndroidModLoader/SAUtils) (MIT), GTA SA MoreSettings research (MIT). The source code in this directory is original and MIT licensed under the repository LICENSE.

## Supported game engine versions

- 2.10 ARM64: two guarded known FPS instruction locations.
- 2.11 ARM64: three guarded W8/W9 instruction locations, **experimental**. If the binary differs, the mod refuses to modify it.
- All other releases: unsupported until the original owned game version is verified, without distributing Rockstar binaries.

The plugin has been built and opcode-tested on GitHub Actions but **has not been validated in live GTA SA gameplay**. A successful native build is not proof of safe 120 FPS physics or missions.
