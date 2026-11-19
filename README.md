# SA Engine Android — port workspace

Experimental Android ARM64 port workspace for studying a GTA San Andreas-compatible Rust engine.

## Important: upstream code and licensing
The upstream [ghosty2004/sa-engine](https://github.com/ghosty2004/sa-engine) workspace declares `license = "UNLICENSED"` and has no root LICENSE file as checked on 2026-11-19. **Do not copy or redistribute upstream source here without obtaining permission or a suitable license from its author.** This repository currently contains only independently written port scaffolding and planning.

No Rockstar game files are included. Users must supply legally obtained game data. This project is unofficial and not endorsed by Rockstar Games.

## Initial Android plan
1. Obtain upstream author's permission/license to modify and redistribute source.
2. Audit 32-bit legacy address assumptions before ARM64 porting; replace fixed-address mappings with portable handles or an emulated 32-bit memory arena where feasible.
3. Implement Android activity/window lifecycle, JNI entry points, asset access via Storage Access Framework, and touch input.
4. Integrate wgpu Vulkan surface and Android audio backend.
5. Test a minimal renderer on arm64-v8a before integrating gameplay.
6. Add CI build for an Android native library, then an APK packaging step once the application host exists.

## Current status
- [x] Repository initialized
- [x] Upstream architecture and license blocker identified
- [ ] Upstream redistribution permission
- [ ] Android native renderer
- [ ] Game runtime port
- [ ] Playable APK

This README is a planning baseline, **not a completed game port**.
