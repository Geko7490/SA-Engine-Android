// SPDX-License-Identifier: MIT
#pragma once
#include <stdint.h>

namespace saenginefps {
// Runtime-checked GTA SA Android 2.10 ARM64 instruction signatures.
// These values do not identify or license the underlying commercial binary.
constexpr uint32_t kStockInstruction = 0x321E0FE9u;
constexpr uint32_t kMovzW9Mask = 0xFFE0001Fu;
constexpr uint32_t kMovzW9Opcode = 0x52800009u;

constexpr bool isAllowedFps(int fps) {
    return fps == 30 || fps == 60 || fps == 90 || fps == 120;
}
constexpr uint32_t encodeCapMovz(int fps) {
    return kMovzW9Opcode | (static_cast<uint32_t>(fps) << 5);
}
constexpr bool isKnownInstruction(uint32_t opcode) {
    return opcode == kStockInstruction ||
        (opcode & kMovzW9Mask) == kMovzW9Opcode;
}
}
