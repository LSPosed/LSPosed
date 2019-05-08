/* Cydia Substrate - Powerful Code Insertion Platform
 * Copyright (C) 2008-2011  Jay Freeman (saurik)
*/

/* GNU Lesser General Public License, Version 3 {{{ */
/*
 * Substrate is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Substrate is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Substrate.  If not, see <http://www.gnu.org/licenses/>.
**/
/* }}} */

#ifndef SUBSTRATE_H_
#define SUBSTRATE_H_

#ifdef __APPLE__
#ifdef __cplusplus
extern "C" {
#endif
#include <mach-o/nlist.h>
#ifdef __cplusplus
}
#endif

#include <objc/runtime.h>
#include <objc/message.h>
#endif

#include <dlfcn.h>
#include <stdlib.h>

#define _finline \
    inline __attribute__((__always_inline__))
#define _disused \
    __attribute__((__unused__))

#define _extern \
    extern "C" __attribute__((__visibility__("default")))

#ifdef __cplusplus
#define _default(value) = value
#else
#define _default(value)
#endif

#ifdef __cplusplus
extern "C" {
#endif

bool MSHookProcess(pid_t pid, const char *library);

typedef const void *MSImageRef;

MSImageRef MSGetImageByName(const char *file);
void *MSFindSymbol(MSImageRef image, const char *name);

void MSHookFunction(void *symbol, void *replace, void **result);

#ifdef __APPLE__
#ifdef __arm__
__attribute__((__deprecated__))
IMP MSHookMessage(Class _class, SEL sel, IMP imp, const char *prefix _default(NULL));
#endif
void MSHookMessageEx(Class _class, SEL sel, IMP imp, IMP *result);
#endif

#ifdef SubstrateInternal
typedef void *SubstrateAllocatorRef;
typedef struct __SubstrateProcess *SubstrateProcessRef;
typedef struct __SubstrateMemory *SubstrateMemoryRef;

SubstrateProcessRef SubstrateProcessCreate(SubstrateAllocatorRef allocator, pid_t pid);
void SubstrateProcessRelease(SubstrateProcessRef process);

SubstrateMemoryRef SubstrateMemoryCreate(SubstrateAllocatorRef allocator, SubstrateProcessRef process, void *data, size_t size);
void SubstrateMemoryRelease(SubstrateMemoryRef memory);
#endif

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus

#ifdef SubstrateInternal
struct SubstrateHookMemory {
    SubstrateMemoryRef handle_;

    SubstrateHookMemory(SubstrateProcessRef process, void *data, size_t size) :
        handle_(SubstrateMemoryCreate(NULL, NULL, data, size))
    {
    }

    ~SubstrateHookMemory() {
        if (handle_ != NULL)
            SubstrateMemoryRelease(handle_);
    }
};
#endif


template<typename Type_>
static inline void MSHookFunction(Type_ *symbol, Type_ *replace, Type_ **result) {
    MSHookFunction(
            reinterpret_cast<void *>(symbol),
            reinterpret_cast<void *>(replace),
            reinterpret_cast<void **>(result)
    );
}

template<typename Type_>
static inline void MSHookFunction(Type_ *symbol, Type_ *replace) {
    return MSHookFunction(symbol, replace, reinterpret_cast<Type_ **>(NULL));
}

template<typename Type_>
static inline void MSHookSymbol(Type_ *&value, const char *name, MSImageRef image = NULL) {
    value = reinterpret_cast<Type_ *>(MSFindSymbol(image, name));
}

template<typename Type_>
static inline void MSHookFunction(const char *name, Type_ *replace, Type_ **result = NULL) {
    Type_ *symbol;
    MSHookSymbol(symbol, name);
    return MSHookFunction(symbol, replace, result);
}

#endif

#define MSHook(type, name, args...) \
    _disused static type (*_ ## name)(args); \
    static type $ ## name(args)

#ifdef __cplusplus
#define MSHake(name) \
    &$ ## name, &_ ## name
#else
#define MSHake(name) \
    &$ ## name, (void **) &_ ## name
#endif


#endif//SUBSTRATE_H_
