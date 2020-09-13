
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
#include "art/runtime/oat_file_manager.h"
#include "framework/fd_utils.h"

namespace edxp {

    static volatile bool installed = false;
    static volatile bool art_hooks_installed = false;
    static volatile bool fwk_hooks_installed = false;
    static HookFunType hook_func = nullptr;

    void InstallArtHooks(void *art_handle);

    void InstallFwkHooks(void *fwk_handle);

    bool InstallLinkerHooks(const char *linker_path);

    CREATE_HOOK_STUB_ENTRIES(void *, mydlopen, const char *file_name, int flags,
                             const void *ext_info,
                             const void *caller) {
        void *handle = mydlopenBackup(file_name, flags, ext_info, caller);
        if (file_name != nullptr && std::string(file_name).find(kLibArtName) != std::string::npos) {
            InstallArtHooks(handle);
        }
        return handle;
    }

    void InstallInlineHooks() {
        if (installed) {
            LOGI("Inline hooks have been installed, skip");
            return;
        }
        LOGI("Start to install inline hooks");
        int api_level = GetAndroidApiLevel();
        if (UNLIKELY(api_level < __ANDROID_API_L__)) {
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
        hook_func = reinterpret_cast<HookFunType>(hook_func_symbol);

        if (api_level >= __ANDROID_API_Q__) {
#if defined(__i386__) || defined(__x86_64__)
            ScopedDlHandle dl_handle(kLibDlPath.c_str());
            void *handle = dl_handle.Get();
            HOOK_FUNC(mydlopen, "__loader_dlopen");
#else
            InstallLinkerHooks(kLinkerPath.c_str());
#endif
        } else {
            ScopedDlHandle art_handle(kLibArtLegacyPath.c_str());
            InstallArtHooks(art_handle.Get());
        }

        ScopedDlHandle fwk_handle(kLibFwkPath.c_str());
        InstallFwkHooks(fwk_handle.Get());
    }

    // @ApiSensitive(Level.MIDDLE)
    bool InstallLinkerHooks(const char *linker_path) {
        // TODO flags
        void *handle = dlopen(kLibSandHookNativePath.c_str(), RTLD_NOW);

        if (!handle) {
            LOGI("Failed to open libsandhook-native");
            return false;
        }

        auto getSym = reinterpret_cast<void *(*)(const char *, const char *)>(dlsym(handle,
                                                                                    "SandGetSym"));
        if (!getSym) {
            LOGI("SandGetSym is null");
            return false;
        }

        auto dlopen_symbol = "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv";
        void *dlopen_addr = getSym(linker_path, dlopen_symbol);
        if (dlopen_addr) {
            hook_func(dlopen_addr, (void *) mydlopenReplace,
                      (void **) &mydlopenBackup);
            LOGI("dlopen hooked");
            return true;
        }

        LOGI("dlopen_addr is null");
        return false;
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

