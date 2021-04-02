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
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"
#pragma once

#include <context.h>
#include "macros.h"
#include "jni_helper.h"
#include "logging.h"
#include <cassert>

namespace lspd {

    ALWAYS_INLINE inline bool RegisterNativeMethodsInternal(JNIEnv *env,
                                                            const char *class_name,
                                                            const JNINativeMethod *methods,
                                                            jint method_count) {

        auto clazz = Context::GetInstance()->FindClassFromCurrentLoader(env, class_name);
        if (clazz.get() == nullptr) {
            LOGF("Couldn't find class: %s", class_name);
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

#ifndef LSP_DEF_NATIVE_METHOD
#define LSP_DEF_NATIVE_METHOD(ret, className, functionName, ...)                \
  extern "C" ret Java_org_lsposed_lspd_nativebridge_## className ## _ ## functionName (JNI_START, ##  __VA_ARGS__)
#endif

#define REGISTER_LSP_NATIVE_METHODS(class_name) \
  RegisterNativeMethodsInternal(env, "org.lsposed.lspd.nativebridge." #class_name, gMethods, arraysize(gMethods))

} // namespace lspd

#pragma clang diagnostic pop
