// SPDX-License-Identifier: MIT
#pragma once
#include <stdint.h>

namespace saenginefps {
// Opcode guards for version-specific, research-only Android ARM64 FPS hooks.
// Matching these guards does not guarantee full game compatibility.
constexpr uint32_t kStock210 = 0x321E0FE9u;
constexpr uint32_t kMovzMask = 0xFFE0001Fu;
constexpr uint32_t kMovzW8 = 0x52800008u;
constexpr uint32_t kMovzW9 = 0x52800009u;

constexpr bool isAllowedFps(int fps) {
    return fps == 30 || fps == 60 || fps == 90 || fps == 120;
}
constexpr uint32_t encodeCapMovz(int fps) {
    return kMovzW9 | (static_cast<uint32_t>(fps) << 5);
}
constexpr uint32_t encodeCapW8(int fps) {
    return kMovzW8 | (static_cast<uint32_t>(fps) << 5);
}
constexpr bool isKnownInstruction(uint32_t opcode) {
    return opcode == kStock210 || (opcode & kMovzMask) == kMovzW9;
}
constexpr bool isKnownW8(uint32_t opcode) {
    return (opcode & kMovzMask) == kMovzW8;
}
constexpr bool isKnownW9(uint32_t opcode) {
    return (opcode & kMovzMask) == kMovzW9;
}
}