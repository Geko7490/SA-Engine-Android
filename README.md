# SA Engine Mobile — GTA SA Mod Center (Android)

**Current version: V0.6.0 Mod Center.**

An independent Android launcher and mod manager for **user-installed GTA San Andreas Android**. The application does not redistribute Rockstar's game or bypass the game's package signatures.

## Downloads

- [V0.6.0 Android APK](https://github.com/Geko7490/SA-Engine-Android/releases/download/launcher-dev/app-debug.apk)
- [V0.6 release and source](https://github.com/Geko7490/SA-Engine-Android/releases/tag/launcher-dev)
- [V0.6 setup, two graphics mods and limitations](docs/MOD_CENTER_V0.6.md)
- [Latest Android CI](https://github.com/Geko7490/SA-Engine-Android/actions)

## Mod Center
- Detects installed GTA SA Android version (target **2.11.311**).
- In-app source browser and Android DownloadManager for two real community graphics mods:
  - PS2 Graphics v5.0 — [original source](https://libertycity.net/files/gta-san-andreas-ios-android/146765-ps2-graphics-v5.0.html).
  - Timecyc uuk ENB — [original source](https://libertycity.net/files/gta-san-andreas-ios-android/242422-timecyc-uuk-enb.html).
- Strict file importer for user-downloaded ZIPs: `timecyc.dat`, `timecycp.dat`, `colorcycle.dat`.
- Shizuku integrated via official Shizuku API v13.1.5 and user service. Executes explicit user-entered ADB shell commands and shows exit status and output without PC, **after Shizuku is activated by the user**.
- Read-only permission diagnostics and a transactional backup-and-restore path for files already accessible at the conventional GTA SA external location.

## Important: GTA SA 2.11.311 compatibility is not yet verified
Compilation and emulator activity launch are not proof that stock GTA SA 2.11.311 loads graphics changes from external `Android/data`. This version may store its game assets in APK/splits and protected directories. When the expected external files do not exist, the Mod Center refuses the install rather than claiming it succeeded. **No game APK or proprietary assets are included.**

The app is not a full replacement game or a guaranteed one-button graphics installer for GTA SA 2.11.311. Its shell management, archive inspection, and restore tools are real features, but final on-device game integration needs a compatible loader or Rockstar-provided external override path.

## Shizuku
Obtain [Shizuku](https://shizuku.rikka.app/download/) and follow its official [wireless debugging setup guide](https://shizuku.rikka.app/guide/setup/). The app does not self-activate ADB privileges or grant itself root.

## Other project components
- Experimental native GTA SA ARM64 FPS plugin: [release](https://github.com/Geko7490/SA-Engine-Android/releases/tag/native-fps-dev).
- Original Unity overlay sources under `UnityPort`. This is not currently an installable or playable GTA SA engine APK.

## Testing / source
`AndroidLauncher/` is a Java/Gradle application, built using Android SDK 35 and Java 17. GitHub Actions compiles the Android APK, runs JUnit security tests, and runs optional Android 35 emulator launch smoke tests. Source is released under MIT; third-party game assets and graphics mods remain owned and distributed by their respective authors.
