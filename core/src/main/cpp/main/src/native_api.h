//
// Created by kotori on 2/4/21.
//

#ifndef LSPOSED_NATIVE_API_H
#define LSPOSED_NATIVE_API_H

#include <cstdint>
#include <string>
#include <base/object.h>

// typedef int (*HookFunType)(void *, void *, void **);  // For portability
typedef void (*LsposedNativeOnModuleLoaded) (const char* name, void* handle);
typedef void (*NativeInit)(void * init_func);
struct LsposedNativeAPIEntriesV1 {
    uint32_t version;
    lspd::HookFunType inlineHookFunc;
};

namespace lspd {
    void InstallNativeAPI();
    void RegisterNativeLib(const std::string& library_name);
}

#endif //LSPOSED_NATIVE_API_H
