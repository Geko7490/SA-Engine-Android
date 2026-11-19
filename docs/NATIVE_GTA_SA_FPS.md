# Native GTA San Andreas Android FPS — first working path toward 60/120

## What was actually built
- Android launcher APK with GitHub Release catalogue, SHA-256 download verification and SAF installation of our open-source FPS module.
- Native ARM64 `libSAEngineFPS.so` built without any Rockstar game libraries or textures.
- A launcher action that opens an already-installed, legally obtained `com.rockstargames.gtasa` app.
- A 30 / 60 / 90 / 120 FPS selector in the launcher. Once the user has successfully installed the AML module into the permitted game mods folder, the selector writes `SAEngineFPS.cfg` alongside `libSAEngineFPS.so`.
- A second selector inside the game's Game settings *if* the game supports AML and has SAUtils installed. This is not available in stock GTA SA without AML.
- Strict opcode checks in the plugin for the candidate GTA SA Android 2.10 ARM64 binary layout. Unrecognized versions are deliberately refused.

## Legal and runtime prerequisites
1. A **legally purchased GTA San Andreas Android** app and its matching data, **Android 2.10 ARM64**. This is *not* the Definitive Edition, GTA SA PC or Unity version.
2. An AML (AndroidModLoader) enabled install of your own game. Stock Play Store GTA SA does not load AML `.so` mods automatically.
3. [AndroidModLoader](https://github.com/AndroidModLoader/AndroidModLoader) (MIT) and [SAUtils](https://github.com/AndroidModLoader/SAUtils) (MIT) loaded in the game.
4. Ability to select the game's AML `mods` folder through Android's system file picker. On Android 11+, `Android/data` access can be restricted by the OS. Some AML installations use an accessible `Android_unprotected/data/com.rockstargames.gtasa/mods/` path. If the OS blocks that folder, use the supported setup method for your own AML installation; launcher cannot bypass Android's storage restrictions.
5. A compatible, legally obtained GTA SA game copy — no game executables, `libGTASA.so`, GTA assets or repackaged APK are provided in this repository.

## Steps
1. Install our [latest launcher preview](https://github.com/Geko7490/SA-Engine-Android/releases/tag/launcher-dev) without deleting existing game data, when Android permits an in-place upgrade with the signing certificate.
2. In launcher select **GitHub Deposundan Kur** and load releases. For a private repository, grant a narrowly scoped read-only GitHub token for that repository. Do not paste this token into chat.
3. Select the **GTA SA FPS MODÜLÜ** release `native-fps-dev`, download it and choose **your own GTA SA AML mods folder** using Android's folder picker. The launcher verifies the downloaded file's GitHub SHA-256 and writes `libSAEngineFPS.so` and `SAEngineFPS.cfg` into the chosen directory.
4. Pick **60 or 120 FPS** from the launcher. The config is updated for the installed module. Restart GTA SA.
5. Tap **GTA SA Android'i Aç**. The game's own settings should also show **SA Engine FPS Limit** when SAUtils loads the plugin.
6. Compare the in-game frame rate, animations, save/load behaviour, driving and missions on the real device. A requested 120 FPS is **not a guarantee**: screen refresh, GPU capability, temperature and game physics may limit actual FPS.

## Notes about high-FPS fixes
The game was originally designed with a much lower FPS cap. Timers, steering, physics, animations and missions may misbehave at high FPS. The separately maintained [JPatch](https://github.com/AndroidModLoader/JPatch) provides many high-FPS fixes for Android 2.10, but has a proprietary freeware license that **forbids independently rehosting its binaries/source**. We therefore only link its official project; it is not bundled in our launcher or releases.

## Runtime verification status
- Native `.so` module: compiled successfully against public AML/SAUtils headers; host-side ARM64 instruction encoding tests added.
- Launcher APK: Gradle compilation and Java unit tests pass in GitHub Actions.
- GTA SA Android gameplay with this particular plugin on the user's phone: **not yet measured or verified**.
- An AML-enabled, user-owned copy of the game is still a prerequisite. The launcher is not a standalone GTA SA game distribution.

## Why no standalone GTA SA APK is included
Open repositories may contain proprietary `libGTASA.so`, fonts or game assets without the rights to redistribute them. We do not copy or mirror such binaries. Users supply their own legitimate game; our own module and launcher are open source.
