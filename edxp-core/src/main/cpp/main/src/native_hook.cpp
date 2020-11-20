
#include <dlfcn.h>
#include <android_build.h>
#include <string>
#include <vector>
#include <config_manager.h>
#include <art/runtime/runtime.h>
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>
#include <dobby.h>
#include "android_restriction.h" // from Dobby

#include "logging.h"
#include "native_hook.h"
#include "riru_hook.h"
#include "art/runtime/mirror/class.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/gc/heap.h"
#include "art/runtime/hidden_api.h"
#include "art/runtime/oat_file_manager.h"
#include "framework/fd_utils.h"

std::vector<soinfo_t> linker_get_solist(); // Dobby but not in .h

namespace edxp {

    static volatile bool installed = false;
    static volatile bool art_hooks_installed = false;
    static volatile bool fwk_hooks_installed = false;
    static HookFunType hook_func = nullptr;

    void InstallArtHooks(void *art_handle);

    void InstallFwkHooks(void *fwk_handle);

    void InstallInlineHooks() {
        if (installed) {
            LOGI("Inline hooks have been installed, skip");
            return;
        }
        installed = true;
        LOGI("Start to install inline hooks");
        int api_level = GetAndroidApiLevel();
        if (UNLIKELY(api_level < __ANDROID_API_L__)) {
            LOGE("API level not supported: %d, skip inline hooks", api_level);
            return;
        }
        LOGI("Using api level %d", api_level);
        InstallRiruHooks();
        hook_func = reinterpret_cast<HookFunType>(DobbyHook);

        // install ART hooks
        if (api_level >= __ANDROID_API_Q__) {
            // From Riru v22 we can't get ART handle by hooking dlopen, so we get libart.so from soinfo.
            // Ref: https://android.googlesource.com/platform/bionic/+/master/linker/linker_soinfo.h
            auto solist = linker_get_solist();
            bool found = false;
            for (auto & it : solist) {
                const char* real_path = linker_soinfo_get_realpath(it);
                if (real_path != nullptr && std::string(real_path).find(kLibArtName) != std::string::npos) {
                    found = true;
                    InstallArtHooks(it);
                    break;
                }
            }
            if(!found) {
                LOGE("Android 10+ detected and libart.so can't be found in memory.");
                return;
            }
        } else {
            // do dlopen directly in Android 9-
            ScopedDlHandle art_handle(kLibArtLegacyPath.c_str());
            InstallArtHooks(art_handle.Get());
        }

        ScopedDlHandle fwk_handle(kLibFwkPath.c_str());
        InstallFwkHooks(fwk_handle.Get());
    }

    void InstallArtHooks(void *art_handle) {
        if (art_hooks_installed) {
            return;
        }
        if (ConfigManager::GetInstance()->IsHiddenAPIBypassEnabled()) {
            art::hidden_api::DisableHiddenApi(art_handle, hook_func);
        }
        art::Runtime::Setup(art_handle, hook_func);
        art::gc::Heap::Setup(art_handle, hook_func);
        art::ClassLinker::Setup(art_handle, hook_func);
        art::mirror::Class::Setup(art_handle, hook_func);
        art::JNIEnvExt::Setup(art_handle, hook_func);
        art::oat_file_manager::DisableOnlyUseSystemOatFiles(art_handle, hook_func);

        art_hooks_installed = true;
        LOGI("ART hooks installed");
    }

    void InstallFwkHooks(void *fwk_handle) {
        if (fwk_hooks_installed) {
            return;
        }
        android::FileDescriptorWhitelist::Setup(fwk_handle, hook_func);
    }

}

