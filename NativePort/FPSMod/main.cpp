// SPDX-License-Identifier: MIT
// Original SA Engine Mobile FPS AML extension. No Rockstar libraries or data.
#include <mod/amlmod.h>
#include <mod/logger.h>
#include "isautils.h"
#include <stdint.h>
#include <stdio.h>

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
    constexpr uintptr_t kLimitSite1 = 0x70A38C;
    constexpr uintptr_t kLimitSite2 = 0x70A458;
    constexpr uint32_t kStockMoveW9 = 0x321E0FE9u;
    constexpr int kCaps[] = { 30, 60, 90, 120 };
    const char* kCapsText[] = { "30 FPS", "60 FPS", "90 FPS", "120 FPS" };
    uintptr_t gameBase = 0;
    bool validated = false;
    bool ready = false;
    int selected = 1;

    // Accept only the original 2.10 instruction or an immediate MOVZ we
    // previously wrote. Refuse to touch other patchers' unexpected bytes.
    bool isSupported(uint32_t instruction) {
        return instruction == kStockMoveW9 ||
            ((instruction & 0xFFE0001Fu) == 0x52800009u);
    }

    bool validateGame() {
        if (gameBase == 0 || aml->GetLibLength("libGTASA.so") < kLimitSite2 + 4) return false;
        uint32_t first = *reinterpret_cast<volatile uint32_t*>(gameBase + kLimitSite1);
        uint32_t second = *reinterpret_cast<volatile uint32_t*>(gameBase + kLimitSite2);
        if (!isSupported(first) || !isSupported(second)) {
            logger->Error("Unsupported libGTASA.so layout / another FPS patch. No memory changes.");
            return false;
        }
        return true;
    }

    bool applyFrameCap(int fps) {
        if (!validated || !ready) return false;
        if (fps != 30 && fps != 60 && fps != 90 && fps != 120) return false;
        if (!validateGame()) return false;

        // AArch64: MOVZ W9,#fps. Do not overwrite anything until both
        // patch sites match the expected instruction signature.
        const uint32_t movz = 0x52800009u | (static_cast<uint32_t>(fps) << 5);
        aml->Unprot(gameBase + kLimitSite1, sizeof(uint32_t));
        aml->Unprot(gameBase + kLimitSite2, sizeof(uint32_t));
        aml->Write32(gameBase + kLimitSite1, movz);
        aml->Write32(gameBase + kLimitSite2, movz);
        logger->Info("GTA SA Android 2.10 FPS cap set to %d", fps);
        return true;
    }

    void onCapChange(int, int newIndex, void*) {
        if (newIndex < 0 || newIndex > 3) return;
        if (applyFrameCap(kCaps[newIndex])) {
            selected = newIndex;
            aml->MLSSetInt("SAENGINE_FPS_MODE", selected);
        }
    }
}

extern "C" void OnAllModsLoaded() {
    logger->SetTag("SA Engine FPS");
    gameBase = aml->GetLib("libGTASA.so");
    if (!gameBase) {
        logger->Error("libGTASA.so not loaded; only GTA SA Android 2.10 is supported");
        return;
    }
    validated = validateGame();
    if (!validated) return;
    ready = true;

    aml->MLSGetInt("SAENGINE_FPS_MODE", &selected);
    if (selected < 0 || selected > 3) selected = 1;

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
