
#pragma once

#define XHOOK_REGISTER(NAME) \
    if (xhook_register(".*", #NAME, (void*) new_##NAME, (void **) &old_##NAME) == 0) { \
        void *f = riru_get_func(#NAME); \
        if (f != nullptr) { \
            memcpy(&old_##NAME, &f, sizeof(void *)); \
        } \
        riru_set_func(#NAME, (void *) new_##NAME); \
    } else { \
        LOGE("failed to register riru hook " #NAME "."); \
    }

#define NEW_FUNC_DEF(ret, func, ...) \
    static ret (*old_##func)(__VA_ARGS__); \
    static ret new_##func(__VA_ARGS__)

namespace edxp {

    // @ApiSensitive(Level.HIGH)
    static constexpr const char *kPropKeyCompilerFilter = "dalvik.vm.dex2oat-filter";
    static constexpr const char *kPropKeyCompilerFlags = "dalvik.vm.dex2oat-flags";
    static constexpr const char *kPropKeyUseJitProfiles = "dalvik.vm.usejitprofiles";
    static constexpr const char *kPropKeyPmBgDexopt = "pm.dexopt.bg-dexopt";

    static constexpr const char *kPropValueCompilerFilter = "quicken";
    static constexpr const char *kPropValuePmBgDexopt = "speed";
    static constexpr const char *kPropValueCompilerFlags = "--inline-max-code-units=0";
    static constexpr const char *kPropValueCompilerFlagsWS = " --inline-max-code-units=0";


    void InstallRiruHooks();

}
