
#pragma once
#include <base/object.h>

namespace lspd {

    // @ApiSensitive(Level.HIGH)
    static constexpr const char *kPropKeyCompilerFilter = "dalvik.vm.dex2oat-filter";
    static constexpr const char *kPropKeyCompilerFlags = "dalvik.vm.dex2oat-flags";
    static constexpr const char *kPropKeyUseJitProfiles = "dalvik.vm.usejitprofiles";
    static constexpr const char *kPropKeyPmBgDexopt = "pm.dexopt.bg-dexopt";

    static constexpr const char *kPropValueCompilerFilter = "quicken";
    static constexpr const char *kPropValuePmBgDexopt = "speed";
    static constexpr const char *kPropValueCompilerFlags = "--inline-max-code-units=0";
    static constexpr const char *kPropValueCompilerFlagsWS = " --inline-max-code-units=0";


    void InstallRiruHooks(HookFunType hook_func);

}
