// SPDX-License-Identifier: MIT
#include "../fps_instruction.h"
#include <cassert>
#include <iostream>

using namespace saenginefps;
static_assert(encodeCapMovz(30) == 0x528003C9u, "30 FPS MOVZ");
static_assert(encodeCapMovz(60) == 0x52800789u, "60 FPS MOVZ");
static_assert(encodeCapMovz(90) == 0x52800B49u, "90 FPS MOVZ");
static_assert(encodeCapMovz(120) == 0x52800F09u, "120 FPS MOVZ");
static_assert(isKnownInstruction(kStock210), "accept known original");
static_assert(isKnownInstruction(encodeCapMovz(120)), "accept own FPS setting");
static_assert(isKnownW8(encodeCapW8(30)), "2.11 W8 opcode");
static_assert(isKnownW8(encodeCapW8(120)), "2.11 W8 at 120");
static_assert(isKnownW9(encodeCapMovz(60)), "2.11 W9 at 60");
static_assert(encodeCapW8(60) == 0x52800788u, "2.11 W8 encoding");
static_assert(encodeCapW8(120) == 0x52800F08u, "2.11 W8 120 encoding");
static_assert(!isKnownW8(encodeCapMovz(120)), "W8 must reject W9");
static_assert(!isKnownW9(encodeCapW8(120)), "W9 must reject W8");
static_assert(!isKnownInstruction(0xffffffffu), "reject unknown patch");

int main() {
    for (int fps : {30, 60, 90, 120}) {
        assert(isAllowedFps(fps));
        assert(isKnownInstruction(encodeCapMovz(fps)));
    }
    for (int fps : {0, 20, 29, 31, 59, 144, 255})
        assert(!isAllowedFps(fps));
    std::cout << "PASS native AArch64 FPS opcode and supported selection tests" << std::endl;
}
