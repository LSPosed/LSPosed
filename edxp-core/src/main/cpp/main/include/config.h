
#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "art/base/macros.h"
#include "android_build.h"

namespace edxp {

//#define LOG_DISABLED
//#define DEBUG

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) (lp64)
#else
# define LP_SELECT(lp32, lp64) (lp32)
#endif

    static constexpr auto kInjectDexPath = "/system/framework/edxp.jar:"
                                           "/system/framework/eddalvikdx.jar:"
                                           "/system/framework/eddexmaker.jar";

    static constexpr auto kEntryClassName = "com.elderdrivers.riru.edxp.core.Main";
    static constexpr auto kClassLinkerClassName = "com.elderdrivers.riru.edxp.art.ClassLinker";
    static constexpr auto kSandHookClassName = "com.swift.sandhook.SandHook";
    static constexpr auto kSandHookNeverCallClassName = "com.swift.sandhook.ClassNeverCall";

    static constexpr auto kLibArtName = "libart.so";
    static constexpr auto kLibFwkName = "libandroid_runtime.so";
    static constexpr auto kLibFwName = "libandroidfw.so";
    static constexpr auto kLibWhaleName = "libwhale.edxp.so";
    static constexpr auto kLibSandHookName = "libsandhook.edxp.so";
    static constexpr auto kLibDlName = "libdl.so";
    static constexpr auto kLibSandHookNativeName = "libsandhook-native.so";

    static const auto kLibBasePath = std::string(
            LP_SELECT("/system/lib/",
                      "/system/lib64/"));
    static const auto kLinkerPath = std::string(
            LP_SELECT("/apex/com.android.runtime/bin/linker",
                      "/apex/com.android.runtime/bin/linker64"));

    static const auto kLibDlPath = kLibBasePath + kLibDlName;
    static const auto kLibArtLegacyPath = kLibBasePath + kLibArtName;
    static const auto kLibWhalePath = kLibBasePath + kLibWhaleName;
    static const auto kLibSandHookPath = kLibBasePath + kLibSandHookName;
    static const auto kLibSandHookNativePath = kLibBasePath + kLibSandHookNativeName;
    static const auto kLibFwPath = kLibBasePath + kLibFwName;
    static const auto kLibFwkPath = kLibBasePath + kLibFwkName;

    inline const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}