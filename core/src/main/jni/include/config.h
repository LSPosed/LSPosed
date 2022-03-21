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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "macros.h"
#include "utils.h"
#include "utils/hook_helper.hpp"

namespace lspd {

//#define LOG_DISABLED
//#define DEBUG
    using lsplant::operator""_tstr;

    inline bool constexpr Is64() {
#if defined(__LP64__)
        return true;
#else
        return false;
#endif
    }

    inline constexpr bool is64 = Is64();

    inline bool constexpr IsDebug() {
#ifdef NDEBUG
        return false;
#else
        return true;
#endif
    }

    inline constexpr bool isDebug = IsDebug();

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) lp64
#else
# define LP_SELECT(lp32, lp64) lp32
#endif

    inline static constexpr auto kLibArtName = "libart.so"_tstr;
    inline static constexpr auto kLibFwName = "libandroidfw.so"_tstr;

    inline constexpr const char *BoolToString(bool b) {
        return b ? "true" : "false";
    }

    extern const int versionCode;
    extern const char* const versionName;
}
