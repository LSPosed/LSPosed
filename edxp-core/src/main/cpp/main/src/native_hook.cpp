
#include <dlfcn.h>
#include <android_build.h>
#include <string>
#include <vector>
#include <SubstrateHook.h>
#include <config_manager.h>
#include <art/runtime/runtime.h>
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>

#include "logging.h"
#include "native_hook.h"
#include "riru_hook.h"
#include "art/runtime/mirror/class.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/gc/heap.h"
#include "art/runtime/hidden_api.h"

namespace edxp {

    static bool installed = false;

    void InstallInlineHooks() {
        if (installed) {
            LOGI("Inline hooks have been installed, skip");
            return;
        }
        LOGI("Start to install inline hooks");
        int api_level = GetAndroidApiLevel();
        if (UNLIKELY(api_level < ANDROID_LOLLIPOP)) {
            LOGE("API level not supported: %d, skip inline hooks", api_level);
            return;
        }
        LOGI("Using api level %d", api_level);
        InstallRiruHooks();
#ifdef __LP64__
        ScopedDlHandle whale_handle(kLibWhalePath.c_str());
        if (!whale_handle.IsValid()) {
            return;
        }
        void *hook_func_symbol = whale_handle.DlSym<void *>("WInlineHookFunction");
#else
        void *hook_func_symbol = (void *) MSHookFunction;
#endif
        if (!hook_func_symbol) {
            return;
        }
        auto hook_func = reinterpret_cast<HookFunType>(hook_func_symbol);
        ScopedDlHandle art_handle(kLibArtPath.c_str());
        if (!art_handle.IsValid()) {
            return;
        }
        art::hidden_api::DisableHiddenApi(art_handle.Get(), hook_func);
        art::Runtime::Setup(art_handle.Get(), hook_func);
        art::gc::Heap::Setup(art_handle.Get(), hook_func);
        art::ClassLinker::Setup(art_handle.Get(), hook_func);
        art::mirror::Class::Setup(art_handle.Get(), hook_func);
        art::JNIEnvExt::Setup(art_handle.Get(), hook_func);
        LOGI("Inline hooks installed");
    }

}

