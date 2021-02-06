
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
                                     Context::GetInstance()->FindClassFromLoader(env, class_name));
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