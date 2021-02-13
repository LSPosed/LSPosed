/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#ifndef __KEYCHECK_H__
#define __KEYCHECK_H__
#include <unordered_set>
#include <unordered_map>

// Constants: pressed keys
#define KEYCHECK_CHECK_VOLUMEDOWN 0x01u
#define KEYCHECK_CHECK_VOLUMEUP 0x02u
#define KEYCHECK_PRESSED_VOLUMEDOWN 41u
#define KEYCHECK_PRESSED_VOLUMEUP 42u

enum class Variant {
    YAHFA = 0x11,
    SandHook = 0x12,
    End = SandHook,
};
const auto AllVariants = { Variant::YAHFA, Variant::SandHook };

Variant& operator++( Variant &c ) {
    using IntType = typename std::underlying_type<Variant>::type;
    c = static_cast<Variant>( static_cast<IntType>(c) + 1 );
    if ( c > Variant::End )
        c = Variant::YAHFA;
    return c;
}

Variant operator++( Variant &c, int ) {
    Variant result = c;
    ++c;
    return result;
}

enum Arch {
    ARM,
    ARM64,
    x86,
    x86_64
};

struct VariantDetail {
    const char* expression;
    std::unordered_set<Arch> supported_arch;
};

#endif // __KEYCHECK_H__