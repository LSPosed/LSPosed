//
// Created by kotori on 2/4/21.
//

#include "native_api.h"
#include "symbol_cache.h"
#include <dobby.h>
#include <vector>

/*
 * Module: define xposed_native file in /assets, each line is a .so file name
 * LSP: Hook do_dlopen, if any .so file matches the name above, try to call
 *      "native_init(void*)" function in target so with function pointer of "init" below.
 * Module: Call init function with the pointer of callback function.
 * LSP: Store the callback function pointer (multiple callback allowed) and return
 *      LsposedNativeAPIEntries struct.
 * Module: Since JNI is not yet available at that time, module can store the struct to somewhere else,
 *      and handle them in JNI_Onload or later.
 * Module: Do some MAGIC provided by LSPosed framework.
 * LSP: If any so loaded by target app, we will send a callback to the specific module callback function.
 *      But an exception is, if the target skipped dlopen and handle linker stuffs on their own, the
 *      callback will not work.
 */

namespace lspd {
    static HookFunType hook_func = nullptr;
    std::vector<LsposedNativeOnModuleLoaded *> moduleLoadedCallbacks;

    LsposedNativeAPIEntriesV1 init(LsposedNativeOnModuleLoaded *onModuleLoaded) {
        if (onModuleLoaded != nullptr) moduleLoadedCallbacks.push_back(onModuleLoaded);

        LsposedNativeAPIEntriesV1 ret{
                .version = 1,
                .inlineHookFunc = hook_func
        };
        return ret;
    }

    void InstallNativeAPI(HookFunType hook_func_) {
        hook_func = hook_func_;
        // hook_func(symbol_do_dlopen)
    }
}