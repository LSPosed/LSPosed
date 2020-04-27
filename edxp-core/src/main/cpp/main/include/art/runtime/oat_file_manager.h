
#pragma once

#include "base/object.h"

namespace art {

    namespace oat_file_manager {

        CREATE_HOOK_STUB_ENTRIES(void, SetOnlyUseSystemOatFiles) {
            return;
        }

        // http://androidxref.com/9.0.0_r3/xref/art/runtime/oat_file_manager.cc#637
        static void DisableOnlyUseSystemOatFiles(void *handle, HookFunType hook_func) {
            const int api_level = GetAndroidApiLevel();
            if (api_level == ANDROID_P) {
                HOOK_FUNC(SetOnlyUseSystemOatFiles,
                          "_ZN3art14OatFileManager24SetOnlyUseSystemOatFilesEv");
            }
            if (api_level == ANDROID_Q) {
                HOOK_FUNC(SetOnlyUseSystemOatFiles,
                          "_ZN3art14OatFileManager24SetOnlyUseSystemOatFilesEbb");
            }
        };

    }

}
