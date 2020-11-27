
#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "art/base/macros.h"
#include "utils.h"

namespace edxp {

//#define LOG_DISABLED
//#define DEBUG

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) (lp64)
#else
# define LP_SELECT(lp32, lp64) (lp32)
#endif

    static const auto kInjectDexPath = "/system/framework/edxp.dex:"
                                           "/system/framework/eddalvikdx.dex:"
                                           "/system/framework/eddexmaker.dex"_str;

    static const auto kEntryClassName = "com.elderdrivers.riru.edxp.core.Main"_str;
    static const auto kClassLinkerClassName = "com.elderdrivers.riru.edxp.art.ClassLinker";
    static const auto kSandHookClassName = "com.swift.sandhook.SandHook"_str;
    static const auto kSandHookNeverCallClassName = "com.swift.sandhook.ClassNeverCall"_str;

    static const auto kLibArtName = "libart.so"_str;
    static const auto kLibFwName = "libandroidfw.so"_str;
    static const auto kLibSandHookName = "libsandhook.edxp.so"_str;
    static const auto kLibDlName = "libdl.so"_str;
    static const auto kLibSandHookNativeName = "libsandhook-native.so"_str;

    static const auto kLibBasePath = std::string(
            LP_SELECT("/system/lib/",
                      "/system/lib64/"));
    static const auto kLibArtLegacyPath = kLibBasePath + kLibArtName;
    static const auto kLibSandHookPath = kLibBasePath + kLibSandHookName;
    static const auto kLibSandHookNativePath = kLibBasePath + kLibSandHookNativeName;
    static const auto kLibFwPath = kLibBasePath + kLibFwName;

    inline constexpr const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}