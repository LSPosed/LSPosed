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

#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <array>
#include "logging.h"
#include "config.h"
#include "context.h"
#include <riru.h>
#include "symbol_cache.h"

namespace lspd {
    static void onModuleLoaded() {
        LOGI("onModuleLoaded: welcome to LSPosed!");
        // rirud must be used in onModuleLoaded
        Context::GetInstance()->PreLoadDex(kDexPath);
        InitSymbolCache();
    }

    static int shouldSkipUid(int) {
        return 0;
    }

    static void nativeForkAndSpecializePre(JNIEnv *env, jclass, jint *_uid, jint *,
                                           jintArray *, jint *,
                                           jobjectArray *, jint *,
                                           jstring *, jstring *nice_name,
                                           jintArray *, jintArray *,
                                           jboolean *start_child_zygote, jstring *,
                                           jstring *app_data_dir, jboolean *,
                                           jobjectArray *,
                                           jobjectArray *,
                                           jboolean *,
                                           jboolean *) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, *_uid,
                                                             *nice_name,
                                                             *start_child_zygote,
                                                             *app_data_dir);
    }

    static void nativeForkAndSpecializePost(JNIEnv *env, jclass, jint res) {
        if (res == 0)
            Context::GetInstance()->OnNativeForkAndSpecializePost(env);
    }

    static void nativeForkSystemServerPre(JNIEnv *env, jclass, uid_t *, gid_t *,
                                          jintArray *, jint *,
                                          jobjectArray *, jlong *,
                                          jlong *) {
        Context::GetInstance()->OnNativeForkSystemServerPre(env);
    }

    static void nativeForkSystemServerPost(JNIEnv *env, jclass, jint res) {
        Context::GetInstance()->OnNativeForkSystemServerPost(env, res);
    }

    /* method added in Android Q */
    static void specializeAppProcessPre(JNIEnv *env, jclass, jint *uid, jint *,
                                        jintArray *, jint *, jobjectArray *,
                                        jint *, jstring *, jstring *nice_name,
                                        jboolean *start_child_zygote, jstring *,
                                        jstring *app_data_dir, jboolean *,
                                        jobjectArray *,
                                        jobjectArray *,
                                        jboolean *,
                                        jboolean *) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, *uid, *nice_name,
                                                             *start_child_zygote,
                                                             *app_data_dir);
    }

    static void specializeAppProcessPost(JNIEnv *env, jclass) {
        Context::GetInstance()->OnNativeForkAndSpecializePost(env);
    }
}

static RiruVersionedModuleInfo module{
        .moduleApiVersion = 24,
        .moduleInfo = RiruModuleInfo{
                .supportHide = true,
                .version = RIRU_MODULE_VERSION,
                .versionName = STRINGIFY(RIRU_MODULE_VERSION_NAME),
                .onModuleLoaded = lspd::onModuleLoaded,
                .shouldSkipUid = lspd::shouldSkipUid,
                .forkAndSpecializePre = lspd::nativeForkAndSpecializePre,
                .forkAndSpecializePost = lspd::nativeForkAndSpecializePost,
                .forkSystemServerPre = lspd::nativeForkSystemServerPre,
                .forkSystemServerPost = lspd::nativeForkSystemServerPost,
                .specializeAppProcessPre = lspd::specializeAppProcessPre,
                .specializeAppProcessPost = lspd::specializeAppProcessPost,
        }
};

static std::string magiskPath;

__attribute__((noinline)) RIRU_EXPORT RiruVersionedModuleInfo *init(Riru *riru) {
    LOGD("riru %d", riru->riruApiVersion);
    LOGD("Support hide: %d", module.moduleInfo.supportHide);
    LOGD("module path: %s", riru->magiskModulePath);
    magiskPath = riru->magiskModulePath;
    return &module;
}

int main() {
    init(nullptr);
}
