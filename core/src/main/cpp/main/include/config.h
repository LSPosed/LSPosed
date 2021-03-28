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

#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "macros.h"
#include "utils.h"

namespace lspd {

//#define LOG_DISABLED
//#define DEBUG


    inline bool constexpr Is64() {
#if defined(__LP64__)
        return true;
#else
        return false;
#endif
    }

    inline constexpr bool is64 = Is64();

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) lp64
#else
# define LP_SELECT(lp32, lp64) lp32
#endif

    static constexpr auto kEntryClassName = "org.lsposed.lspd.core.Main"_tstr;
    static constexpr auto kClassLinkerClassName = "org.lsposed.lspd.nativebridge.ClassLinker"_tstr;
    static constexpr auto kBridgeServiceClassName = "org.lsposed.lspd.service.BridgeService"_tstr;
    static constexpr auto kDexPath = "framework/lspd.dex"_tstr;

    static constexpr auto kLibArtName = "libart.so"_tstr;
    static constexpr auto kLibFwName = "libandroidfw.so"_tstr;
    static constexpr auto kLinkerName = LP_SELECT("linker"_tstr, "linker64"_tstr);
    static constexpr auto kLibcName = "libc.so"_tstr;
    static constexpr auto kLibbaseName = "libbase.so"_tstr;

    static constexpr auto kLibBasePath =
            LP_SELECT("/system/lib/"_tstr,
                      "/system/lib64/"_tstr);

    static constexpr auto kBinBasePath = "/system/bin/"_tstr;

    static constexpr auto kLibFwPath = kLibBasePath + kLibFwName;
    static constexpr auto kLinkerPath = kBinBasePath + kLinkerName;
    static constexpr auto kLibcPath = kLibBasePath + kLibcName;
    static constexpr auto kLibbasePath = kLibBasePath + kLibbaseName;

    inline constexpr const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}
