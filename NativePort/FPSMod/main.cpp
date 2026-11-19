// SPDX-License-Identifier: MIT
// Original SA Engine Mobile FPS AML extension. No Rockstar libraries or data.
#include <mod/amlmod.h>
#include <mod/logger.h>
#include "isautils.h"
#include "fps_instruction.h"
#include <stdint.h>
#include <stdio.h>
#include <dlfcn.h>
#include <string>
#include <fstream>

extern "C" void OnAllModsLoaded();

MYMOD(net.geko7490.saengine.fps, SA Engine FPS, 0.1, Geko7490)
NEEDGAME(com.rockstargames.gtasa)

BEGIN_DEPLIST()
    ADD_DEPENDENCY_VER(net.rusjj.aml, 1.2)
END_DEPLIST()

// Verified reference locations from open research for the GTA SA Android 2.10
// AArch64 build ONLY. Never apply to an unrecognized game or offset layout.
#if !defined(__aarch64__)
#error "This module is intentionally ARM64-only"
#endif
namespace {
    constexpr uintptr_t k210First = 0x70A38C;
    constexpr uintptr_t k210Second = 0x70A458;
    constexpr uintptr_t k211W8First = 0x3683D0;
    constexpr uintptr_t k211W8Second = 0x368644;
    constexpr uintptr_t k211W9Third = 0x3685B8;
    enum class Layout { Unknown, Android210, Android211 };
    Layout layout = Layout::Unknown;
    constexpr int kCaps[] = { 30, 60, 90, 120 };
    const char* kCapsText[] = { "30 FPS", "60 FPS", "90 FPS", "120 FPS" };
    uintptr_t gameBase = 0;
    bool validated = false;
    bool ready = false;
    int selected = 1;
    std::string configFile;

    void findConfigFile() {
        Dl_info info{};
        if (!dladdr(reinterpret_cast<const void*>(&OnAllModsLoaded), &info) || !info.dli_fname)
            return;
        std::string path(info.dli_fname);
        std::string::size_type slash = path.find_last_of('/');
        if (slash != std::string::npos)
            configFile = path.substr(0, slash + 1) + "SAEngineFPS.cfg";
    }
    bool readDesiredFps(int& fps) {
        if (configFile.empty()) return false;
        std::ifstream file(configFile);
        int value = 0;
        if (!(file >> value)) return false;
        for (int cap : kCaps) if (cap == value) { fps = value; return true; }
        return false;
    }
    void persistDesiredFps(int fps) {
        if (configFile.empty()) return;
        std::ofstream file(configFile, std::ios::trunc);
        if (file) file << fps << "\n";
    }

    // Select a patch layout only after checking *all* its expected
    // instructions. Unknown/modified versions are never patched.
    uint32_t readWord(uintptr_t offset) {
        return *reinterpret_cast<volatile const uint32_t*>(gameBase + offset);
    }

    Layout detectLayout() {
        if (gameBase == 0) return Layout::Unknown;
        uintptr_t length = aml->GetLibLength("libGTASA.so");
        if (length > k210Second + sizeof(uint32_t) &&
                saenginefps::isKnownInstruction(readWord(k210First)) &&
                saenginefps::isKnownInstruction(readWord(k210Second))) {
            return Layout::Android210;
        }
        // Experimental 2.11 branch from public native integration references.
        // Only accept existing MOVZ W8/W9 instructions at ALL three sites.
        if (length > k211W8Second + sizeof(uint32_t) &&
                saenginefps::isKnownW8(readWord(k211W8First)) &&
                saenginefps::isKnownW8(readWord(k211W8Second)) &&
                saenginefps::isKnownW9(readWord(k211W9Third))) {
            return Layout::Android211;
        }
        return Layout::Unknown;
    }

    bool applyFrameCap(int fps) {
        if (!validated || !ready || !saenginefps::isAllowedFps(fps))
            return false;
        if (detectLayout() != layout) {
            logger->Error("Game executable changed or FPS patch layout unknown; refusing changes.");
            return false;
        }
        if (layout == Layout::Android210) {
            const uint32_t opcode = saenginefps::encodeCapMovz(fps);
            aml->Unprot(gameBase + k210First, sizeof(opcode));
            aml->Unprot(gameBase + k210Second, sizeof(opcode));
            aml->Write32(gameBase + k210First, opcode);
            aml->Write32(gameBase + k210Second, opcode);
        } else if (layout == Layout::Android211) {
            const uint32_t w8 = saenginefps::encodeCapW8(fps);
            const uint32_t w9 = saenginefps::encodeCapMovz(fps);
            aml->Unprot(gameBase + k211W8First, sizeof(w8));
            aml->Unprot(gameBase + k211W8Second, sizeof(w8));
            aml->Unprot(gameBase + k211W9Third, sizeof(w9));
            aml->Write32(gameBase + k211W8First, w8);
            aml->Write32(gameBase + k211W8Second, w8);
            aml->Write32(gameBase + k211W9Third, w9);
        } else return false;
        logger->Info("GTA SA native ARM64 FPS target set to %d", fps);
        return true;
    }

    void onCapChange(int, int newIndex, void*) {
        if (newIndex < 0 || newIndex > 3) return;
        if (applyFrameCap(kCaps[newIndex])) {
            selected = newIndex;
            aml->MLSSetInt("SAENGINE_FPS_MODE", selected);
            persistDesiredFps(kCaps[newIndex]);
        }
    }
}

extern "C" void OnAllModsLoaded() {
    logger->SetTag("SA Engine FPS");
    gameBase = aml->GetLib("libGTASA.so");
    if (!gameBase) {
        logger->Error("libGTASA.so not loaded; requires GTA SA Android ARM64 2.10/2.11");
        return;
    }
    layout = detectLayout();
    validated = layout != Layout::Unknown;
    if (!validated) {
        logger->Error("Unsupported GTA SA ARM64 version or unknown patch layout. No memory changes.");
        return;
    }
    logger->Info("Compatible candidate FPS patch sites detected (%s)",
        layout == Layout::Android210 ? "2.10" : "2.11 experimental");
    ready = true;

    aml->MLSGetInt("SAENGINE_FPS_MODE", &selected);
    if (selected < 0 || selected > 3) selected = 1;
    findConfigFile();
    int requested = 0;
    if (readDesiredFps(requested)) {
        for (int i = 0; i < 4; ++i) if (kCaps[i] == requested) selected = i;
    }
    applyFrameCap(kCaps[selected]);

    auto* utils = reinterpret_cast<ISAUtils*>(GetInterface("SAUtils"));
    if (!utils) {
        logger->Info("SAUtils not found. Install SAUtils >=1.4 for in-game FPS choices.");
        return;
    }
    utils->AddClickableItem(SetType_Game, "SA Engine FPS Limit", selected,
            0, 3, kCapsText, onCapChange, nullptr);
    logger->Info("In-game 30/60/90/120 FPS selector enabled");
}
