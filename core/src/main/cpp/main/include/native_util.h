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
#include <art/base/macros.h>
#include <nativehelper/scoped_local_ref.h>
#include <android-base/logging.h>
#include "JNIHelper.h"

namespace lspd {

    ALWAYS_INLINE inline void RegisterNativeMethodsInternal(JNIEnv *env,
                                                            const char *class_name,
                                                            const JNINativeMethod *methods,
                                                            jint method_count) {

        ScopedLocalRef<jclass> clazz(env,
                                     Context::GetInstance()->FindClassFromCurrentLoader(env, class_name));
        if (clazz.get() == nullptr) {
            LOG(FATAL) << "Couldn't find class: " << class_name;
            return;
        }
        jint jni_result = JNI_RegisterNatives(env, clazz.get(), methods, method_count);
        CHECK_EQ(JNI_OK, jni_result);
    }

#define REGISTER_LSP_NATIVE_METHODS(class_name) \
  RegisterNativeMethodsInternal(env, "io.github.lsposed.lspd.nativebridge." #class_name, gMethods, arraysize(gMethods))

} // namespace lspd

#pragma clang diagnostic pop