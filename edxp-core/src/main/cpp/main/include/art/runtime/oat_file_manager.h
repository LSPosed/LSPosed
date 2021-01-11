
#pragma once

#include "base/object.h"

namespace art {

    namespace oat_file_manager {

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art14OatFileManager24SetOnlyUseSystemOatFilesEv", // 9 & 11
                void, SetOnlyUseSystemOatFiles, (), {
                    return;
                });

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art14OatFileManager24SetOnlyUseSystemOatFilesEbb", // 10
                void, SetOnlyUseSystemOatFilesQ, (), {
                    return;
                });

        // @ApiSensitive(Level.LOW)
        // http://androidxref.com/9.0.0_r3/xref/art/runtime/oat_file_manager.cc#637
        static void DisableOnlyUseSystemOatFiles(void *handle, HookFunType hook_func) {
            const int api_level = edxp::GetAndroidApiLevel();
            if (api_level >= __ANDROID_API_P__) {
                edxp::HookSyms(handle, hook_func, SetOnlyUseSystemOatFiles,
                               SetOnlyUseSystemOatFilesQ);
            }
        }

    }

}
