
#pragma once
#include <dobby.h>

namespace lspd {
    typedef void (*HookFunType)(void *, void *, void **);
    static HookFunType hook_func =  reinterpret_cast<HookFunType>(DobbyHook);
    void InstallInlineHooks();

}
