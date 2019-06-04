
#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "art/base/macros.h"

namespace edxp {

//#define LOG_DISABLED
//#define DEBUG

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) (lp64)
#else
# define LP_SELECT(lp32, lp64) (lp32)
#endif

    static constexpr const char *kInjectDexPath = "/system/framework/edxp.jar:"
                                                  "/system/framework/eddalvikdx.jar:"
                                                  "/system/framework/eddexmaker.jar";
    static constexpr const char *kEntryClassName = "com.elderdrivers.riru.edxp.core.Main";
    static constexpr const char *kClassLinkerClassName = "com.elderdrivers.riru.edxp.art.ClassLinker";
    static constexpr const char *kSandHookClassName = "com.swift.sandhook.SandHook";
    static constexpr const char *kSandHookNeverCallClassName = "com.swift.sandhook.ClassNeverCall";

    static const std::string kLibBasePath = LP_SELECT("/system/lib/", "/system/lib64/");
    static const std::string kLibArtPath = kLibBasePath + "libart.so";
    static const std::string kLibWhalePath = kLibBasePath + "libwhale.edxp.so";
    static const std::string kLibSandHookPath = kLibBasePath + "libsandhook.edxp.so";
    static const std::string kLibFwPath = kLibBasePath + "libandroidfw.so";

    inline const char *const BoolToString(bool b) {
        return b ? "true" : "false";
    }
}