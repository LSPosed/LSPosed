//
// Created by kotori on 2/4/21.
//

#ifndef LSPOSED_NATIVE_API_H
#define LSPOSED_NATIVE_API_H

#include <cstdint>

typedef int (*HookFunType)(void *, void *, void **);  // For portability
struct LsposedNativeAPIEntriesV1 {
    uint32_t version;
    HookFunType inlineHookFunc;
};

#endif //LSPOSED_NATIVE_API_H
