
#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "art/base/macros.h"
#include "utils.h"

namespace edxp {

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

    static const auto kEntryClassName = "com.elderdrivers.riru.edxp.core.Main"s;
    static const auto kClassLinkerClassName = "com.elderdrivers.riru.edxp.art.ClassLinker"s;
    static const auto kSandHookClassName = "com.swift.sandhook.SandHook"s;
    static const auto kSandHookNeverCallClassName = "com.swift.sandhook.ClassNeverCall"s;

    static const auto kLibArtName = "libart.so"s;
    static const auto kLibFwName = "libandroidfw.so"s;

    static const auto kLibBasePath =
            LP_SELECT("/system/lib/"s,
                      "/system/lib64/"s);
    static const auto kLibArtLegacyPath = kLibBasePath + kLibArtName;
    static const auto kLibFwPath = kLibBasePath + kLibFwName;

    inline constexpr const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}