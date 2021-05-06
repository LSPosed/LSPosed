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

#pragma once

#include "symbol_cache.h"
#include "base/object.h"
#include "context.h"
#include "runtime.h"

namespace art {

    namespace hidden_api {

        CREATE_FUNC_SYMBOL_ENTRY(void, DexFile_setTrusted, JNIEnv *env, jclass clazz,
                                 jobject j_cookie) {
            if (LIKELY(DexFile_setTrustedSym != nullptr)) {
                Runtime::Current()->SetJavaDebuggable(true);
                DexFile_setTrustedSym(env, clazz, j_cookie);
                Runtime::Current()->SetJavaDebuggable(false);
            }
        };

        inline void
        maybeSetTrusted(JNIEnv *env, jclass clazz, jobject class_loader, jobject j_cookie) {
            static auto get_parent = env->GetMethodID(env->FindClass("java/lang/ClassLoader"),
                                                      "getParent", "()Ljava/lang/ClassLoader;");
            for (auto current = lspd::Context::GetInstance()->GetCurrentClassLoader();
                 class_loader != nullptr;
                 class_loader = env->CallObjectMethod(class_loader, get_parent)) {
                if (!current || env->IsSameObject(class_loader, current)) {
                    DexFile_setTrusted(env, clazz, j_cookie);
                    LOGD("Set classloader as trusted");
                    return;
                }
            }
        }

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3artL25DexFile_openDexFileNativeEP7_JNIEnvP7_jclassP8_jstringS5_iP8_jobjectP13_jobjectArray",
                jobject, DexFile_openDexFileNative, (JNIEnv * env,
                jclass clazz,
                jstring javaSourceName,
                jstring javaOutputName,
                jint flags,
                jobject class_loader,
                jobjectArray dex_elements), {
                    auto j_cookie = backup(env, clazz, javaSourceName, javaOutputName, flags,
                                           class_loader,
                                           dex_elements);
                    maybeSetTrusted(env, clazz, class_loader, j_cookie);
                    return j_cookie;
                }
        );

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3artL34DexFile_openInMemoryDexFilesNativeEP7_JNIEnvP7_jclassP13_jobjectArrayS5_P10_jintArrayS7_P8_jobjectS5_",
                jobject, DexFile_openInMemoryDexFilesNative, (JNIEnv * env,
                jclass clazz,
                jobjectArray buffers,
                jobjectArray arrays,
                jintArray jstarts,
                jintArray jends,
                jobject class_loader,
                jobjectArray dex_elements), {
                    auto j_cookie = backup(env, clazz, buffers, arrays, jstarts, jends,
                                           class_loader,
                                           dex_elements);
                    maybeSetTrusted(env, clazz, class_loader, j_cookie);
                    return j_cookie;
                }
        );

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3artL29DexFile_createCookieWithArrayEP7_JNIEnvP7_jclassP11_jbyteArrayii",
                jobject, DexFile_createCookieWithArray, (JNIEnv * env,
                jclass clazz,
                jbyteArray buffer,
                jint start,
                jint end), {
                    auto j_cookie = backup(env, clazz, buffer, start, end);
                    DexFile_setTrusted(env, clazz, j_cookie);
                    return j_cookie;
                }
        );

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3artL36DexFile_createCookieWithDirectBufferEP7_JNIEnvP7_jclassP8_jobjectii",
                jobject, DexFile_createCookieWithDirectBuffer, (JNIEnv * env,
                jclass clazz,
                jobject buffer,
                jint start,
                jint end), {
                    auto j_cookie = backup(env, clazz, buffer, start, end);
                    DexFile_setTrusted(env, clazz, j_cookie);
                    return j_cookie;
                }
        );

        static void DisableHiddenApi(void *handle) {

            const int api_level = lspd::GetAndroidApiLevel();
            if (api_level < __ANDROID_API_P__) {
                return;
            }
            DexFile_setTrustedSym = reinterpret_cast<decltype(DexFile_setTrustedSym)>(lspd::sym_setTrusted);
            lspd::HookSymNoHandle(lspd::sym_openDexFileNative, DexFile_openDexFileNative);
            lspd::HookSymNoHandle(lspd::sym_openInMemoryDexFilesNative,
                                  DexFile_openInMemoryDexFilesNative);
            if (api_level == __ANDROID_API_P__) {
                lspd::HookSymNoHandle(lspd::sym_createCookieWithArray,
                                      DexFile_createCookieWithArray);
                lspd::HookSymNoHandle(lspd::sym_createCookieWithDirectBuffer,
                                      DexFile_createCookieWithDirectBuffer);
            }
        };

    }

}
