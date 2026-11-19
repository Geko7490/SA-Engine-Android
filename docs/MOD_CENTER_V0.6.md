# SA Engine Mobile — GTA San Andreas 2.11.311 Mod Center V0.6

## Runtime deliverable
- Real Android `.apk` built with Java 17, Android Gradle plugin and Shizuku 13.1.5.
- Launch from SA Engine home screen using **GTA SA MOD CENTER — GRAFİK & ADB SHELL**.
- Links to **two real published GTA SA Android graphics mods**:
  - [PS2 Graphics v5.0](https://libertycity.net/files/gta-san-andreas-ios-android/146765-ps2-graphics-v5.0.html) — timecycle/colorcycle style.
  - [Timecyc uuk ENB (2026)](https://libertycity.net/files/gta-san-andreas-ios-android/242422-timecyc-uuk-enb.html) — timecycle colors and sky.
- Mod sources are **not redistributed**: author download page opens in the app's WebView; DownloadManager handles authorized downloads, then user selects the downloaded ZIP using SAF.

## In-app shell and permission model
- Uses official `dev.rikka.shizuku:api:13.1.5` + `provider:13.1.5`.
- Shizuku UserService runs actual `/system/bin/sh -c ...` as UID 2000 (ADB shell) when Shizuku is started with wireless debugging, or the effective UID provided by the Shizuku installation.
- The user must install Shizuku separately, activate it with wireless/USB debugging and grant our app permission. Android security **does not allow us to silently perform initial wireless-debugging pairing on the user's behalf.**
- Commands are entered by the user, must be confirmed, and provide exit status plus stdout and stderr, with a bounded 45-second timeout and output size. Commands run in a new shell each time; interactive sessions are not supported.
- **No root** is conferred on a normal Shizuku ADB session. `/data/user/0/com.rockstargames.gtasa` remains inaccessible without root or a privilege specifically granted by the OS.

## Graphics Mod engine (experimental on 2.11.311)
- Strict ZIP analysis only extracts `timecyc.dat`, `timecycp.dat`, and `colorcycle.dat`; path traversal, binaries, `.so`, `.apk`, scripts and unexpected formats are not installed.
- Before install app runs shell diagnostics for GTA SA 2.11.311 and the external target path `/sdcard/Android/data/com.rockstargames.gtasa/files/data`.
- If the destination directory or an existing corresponding original file is absent, install **refuses** to write anything. ADB cannot make GTA SA dynamically load graphics assets that it does not read from external storage.
- If the original files are present and writable, the installer copies all originals to an app-specific backup directory *before any modification*. Then it verifies staging copy equality and atomically swaps files; shell exit trap attempts rollback on failures.
- `GERİ YÜKLE` restores originals from the backup; only one installed graphics mod is supported at once to avoid mod conflicts.
- **Not proven** that the stock GTA SA 2.11.311 release reads these external files. The app may correctly report that this installation mode is unavailable. Do not call it a compatible graphics mod loader until tested on the user's phone.
- Asset archives, split APKs and internal protected `/data/user/0/` are never modified by the automatic installer. No bypass of Rockstar package signature is attempted.

## Verification
- GitHub Actions builds `app-debug.apk` and runs JUnit tests.
- Tests cover archive traversal rejection, shell quoting, installer success markers, catalogue safety, and FPS module selectors.
- Actual Shizuku authorization, HyperOS shell access, original GTA SA gameplay, image changes and FPS must be verified on the user's device.

## Install
1. Back up your game saves and the earlier launcher import data before replacing a debug-signed APK. If Android reports signing mismatch, do not uninstall without backup: uninstall may delete app-specific launcher data.
2. Install the current [launcher APK](https://github.com/Geko7490/SA-Engine-Android/releases/tag/launcher-dev).
3. Install and start [Shizuku](https://shizuku.rikka.app/download/), enable wireless debugging and approve this app.
4. Open **GTA SA MOD CENTER** and press **SHIZUKU'YU BAĞLA**.
5. Press **ADB ERİŞİM TESTİ**; confirm the shell can see GTA SA external game data before installing a mod.
6. Open one of the two graphics mod sources inside the app, download the ZIP, select it in Mod Center, inspect selected filenames and confirm install. If external overrides are absent, a clear error is expected.
7. Use **ORİJİNAL GRAFİK DOSYALARINI GERİ YÜKLE** to remove the last supported replacement.

## External sources and license
Our installer and shell code are original, under the repository MIT license. Third-party mods remain hosted by their original authors and are **not included** in the APK or republished. Official GTA SA files are required to play. This is an unofficial tool not affiliated with Rockstar.
