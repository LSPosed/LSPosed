//
// Created by kotori on 2/4/21.
//

#ifndef LSPOSED_NATIVE_API_H
#define LSPOSED_NATIVE_API_H

#include <cstdint>

typedef int (*HookFunType)(void *, void *, void **);  // For portability
typedef void (*LsposedNativeOnModuleLoaded) (const char*);  // param=so name
struct LsposedNativeAPIEntriesV1 {
    uint32_t version;
    HookFunType inlineHookFunc;
};

namespace lspd {
    void InstallNativeAPI(HookFunType hook_func_);
}

#endif //LSPOSED_NATIVE_API_H
