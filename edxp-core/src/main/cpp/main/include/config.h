
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

    static const auto kLibBasePath = std::string(LP_SELECT("/system/lib/", "/system/lib64/"));
    static const auto kLibRuntimeBasePath = std::string(
            LP_SELECT("/apex/com.android.runtime/lib/", "/apex/com.android.runtime/lib64/"));

    static const auto kLibArtPath =
            (GetAndroidApiLevel() >= ANDROID_Q ? kLibRuntimeBasePath : kLibBasePath) + kLibArtName;
    static const auto kLibWhalePath = kLibBasePath + "libwhale.edxp.so";
    static const auto kLibSandHookPath = kLibBasePath + "libsandhook.edxp.so";
    static const auto kLibFwPath = kLibBasePath + "libandroidfw.so";
    static const auto kLibDlPath = kLibBasePath + "libdl.so";
    static const auto kLibFwkPath = kLibBasePath + kLibFwkName;

    inline const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}