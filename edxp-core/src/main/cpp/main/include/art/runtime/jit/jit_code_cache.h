#pragma once

#include "base/object.h"
#include "art/runtime/art_method.h"

namespace art {

    namespace jit {

        CREATE_MEM_HOOK_STUB_ENTRIES(const void*, GetSavedEntryPointOfPreCompiledMethod, void *thiz,
                                 void *art_method) {
            if (UNLIKELY(edxp::isHooked(art_method))) {
                LOGD("Found hooked method %p (%s), return entrypoint as jit entrypoint", art_method,
                     art::art_method::PrettyMethod(art_method).c_str());
                return getEntryPoint(art_method);
            }
            return GetSavedEntryPointOfPreCompiledMethodBackup(thiz, art_method);
        }

        static void HookJitCacheCode(void *handle, HookFunType hook_func) {
            const int api_level = edxp::GetAndroidApiLevel();
            // For android R, the invisibly initialization makes static methods initializes multiple
            // times in non-x86 devices. So we need to hook this function to make sure
            // our hooked entry point won't be overwritten.
            // This is for SandHook and YAHFA
            if (api_level >= __ANDROID_API_R__) {
                HOOK_MEM_FUNC(GetSavedEntryPointOfPreCompiledMethod,
                          "_ZN3art3jit12JitCodeCache37GetSavedEntryPointOfPreCompiledMethodEPNS_9ArtMethodE");
            }
        }

    }

}
