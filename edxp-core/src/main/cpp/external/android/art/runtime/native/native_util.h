/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#pragma once

#include <jni.h>
#include "android-base/logging.h"
#include "nativehelper/scoped_local_ref.h"
#include "art/base/macros.h"

namespace art {
    ALWAYS_INLINE inline void RegisterNativeMethodsWithClass(JNIEnv* env,
                                                            jclass clazz,
                                                            const JNINativeMethod* methods,
                                                            jint method_count) {
        ScopedLocalRef<jclass> c(env, clazz);
        if (clazz == nullptr) {
            LOG(ERROR) << "clazz is null";
            return;
        }
        jint jni_result = env->RegisterNatives(c.get(), methods, method_count);
        CHECK_EQ(JNI_OK, jni_result);
    }
    ALWAYS_INLINE inline void RegisterNativeMethodsWithName(JNIEnv* env,
                                                            const char* jni_class_name,
                                                            const JNINativeMethod* methods,
                                                            jint method_count) {
        ScopedLocalRef<jclass> clazz(env, env->FindClass(jni_class_name));
        if (clazz.get() == nullptr) {
            LOG(FATAL) << "Couldn't find class: " << jni_class_name;
            return;
        }
        RegisterNativeMethodsWithClass(env, clazz.get(), methods, method_count);
    }
#define REGISTER_NATIVE_METHODS(jni_class_name) \
  RegisterNativeMethodsWithName(env, (jni_class_name), gMethods, arraysize(gMethods))

#define REGISTER_NATIVE_METHODS_WITH_CLASS(clazz) \
  RegisterNativeMethodsWithClass(env, (clazz), gMethods, arraysize(gMethods))
}  // namespace art