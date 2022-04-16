/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#include <dlfcn.h>
#include "dobby.h"
#include <sys/mman.h>
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"
#pragma once

#include <context.h>
#include "macros.h"
#include "utils/jni_helper.hpp"
#include "logging.h"
#include "config.h"
#include <cassert>
#include "ConfigBridge.h"

#define _uintval(p)               reinterpret_cast<uintptr_t>(p)
#define _ptr(p)                   reinterpret_cast<void *>(p)
#define _align_up(x, n)           (((x) + ((n) - 1)) & ~((n) - 1))
#define _align_down(x, n)         ((x) & -(n))
#define _page_size                4096
#define _page_align(n)            _align_up(static_cast<uintptr_t>(n), _page_size)
#define _ptr_align(x)             _ptr(_align_down(reinterpret_cast<uintptr_t>(x), _page_size))
#define _make_rwx(p, n)           ::mprotect(_ptr_align(p), \
                                              _page_align(_uintval(p) + n) != _page_align(_uintval(p)) ? _page_align(n) + _page_size : _page_align(n), \
                                              PROT_READ | PROT_WRITE | PROT_EXEC)

namespace lspd {

[[gnu::always_inline]]
inline bool RegisterNativeMethodsInternal(JNIEnv *env,
                                          std::string_view class_name,
                                          const JNINativeMethod *methods,
                                          jint method_count) {

    auto clazz = Context::GetInstance()->FindClassFromCurrentLoader(env, class_name.data());
    if (clazz.get() == nullptr) {
        LOGF("Couldn't find class: {}", class_name.data());
        return false;
    }
    return JNI_RegisterNatives(env, clazz, methods, method_count);
}

#if defined(__cplusplus)
#define _NATIVEHELPER_JNI_MACRO_CAST(to) \
    reinterpret_cast<to>
#else
#define _NATIVEHELPER_JNI_MACRO_CAST(to) \
    (to)
#endif

#ifndef LSP_NATIVE_METHOD
#define LSP_NATIVE_METHOD(className, functionName, signature)                \
  { #functionName,                                                       \
    signature,                                                           \
    _NATIVEHELPER_JNI_MACRO_CAST(void*) (Java_org_lsposed_lspd_nativebridge_## className ## _ ## functionName) \
  }
#endif

#define JNI_START [[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz

#ifndef LSP_DEF_NATIVE_METHOD
#define LSP_DEF_NATIVE_METHOD(ret, className, functionName, ...)                \
  extern "C" ret Java_org_lsposed_lspd_nativebridge_## className ## _ ## functionName (JNI_START, ##  __VA_ARGS__)
#endif

#define REGISTER_LSP_NATIVE_METHODS(class_name) \
  RegisterNativeMethodsInternal(env, GetNativeBridgeSignature() + #class_name, gMethods, arraysize(gMethods))

inline int HookFunction(void *original, void *replace, void **backup) {
    _make_rwx(original, _page_size);
    if constexpr (isDebug) {
        Dl_info info;
        if (dladdr(original, &info))
        LOGD("Hooking {} ({}) from {} ({})",
             info.dli_sname ? info.dli_sname : "(unknown symbol)", info.dli_saddr,
             info.dli_fname ? info.dli_fname : "(unknown file)", info.dli_fbase);
    }
    return DobbyHook(original, replace, backup);
}

inline int UnhookFunction(void *original) {
    if constexpr (isDebug) {
        Dl_info info;
        if (dladdr(original, &info))
        LOGD("Unhooking {} ({}) from {} ({})",
             info.dli_sname ? info.dli_sname : "(unknown symbol)", info.dli_saddr,
             info.dli_fname ? info.dli_fname : "(unknown file)", info.dli_fbase);
    }
    return DobbyDestroy(original);
}

static std::string GetNativeBridgeSignature() {
    const auto &obfs_map = ConfigBridge::GetInstance()->obfuscation_map();
    static auto signature = obfs_map.at("org.lsposed.lspd.nativebridge.");
    return signature;
}

} // namespace lspd

#pragma clang diagnostic pop
