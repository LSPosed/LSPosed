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

#define RIRU_MODULE
#include <riru.h>

namespace lspd {
    int *allowUnload = nullptr;
    namespace {
        std::string magiskPath;

        jstring nice_name = nullptr;
        jstring app_data_dir = nullptr;

        void onModuleLoaded() {
            LOGI("onModuleLoaded: welcome to LSPosed!");
            LOGI("onModuleLoaded: version v%s (%d)", versionName, versionCode);
            lspd::InitSymbolCache(nullptr);
            Context::GetInstance()->Init();
            if constexpr (isDebug) {
                Context::GetInstance()->PreLoadDex("/system/" + kDexPath);
            } else {
                Context::GetInstance()->PreLoadDex(magiskPath + '/' + kDexPath);
            }
        }

        void nativeForkAndSpecializePre(JNIEnv *env, jclass, jint *_uid, jint *,
                                        jintArray *gids, jint *,
                                        jobjectArray *, jint *,
                                        jstring *, jstring *_nice_name,
                                        jintArray *, jintArray *,
                                        jboolean *start_child_zygote, jstring *,
                                        jstring *_app_data_dir, jboolean *,
                                        jobjectArray *,
                                        jobjectArray *,
                                        jboolean *,
                                        jboolean *) {
            nice_name = *_nice_name;
            app_data_dir = *_app_data_dir;
            Context::GetInstance()->OnNativeForkAndSpecializePre(env, *_uid, *gids,
                                                                 nice_name,
                                                                 *start_child_zygote,
                                                                 app_data_dir);
        }

        void nativeForkAndSpecializePost(JNIEnv *env, jclass, jint res) {
            if (res == 0)
                Context::GetInstance()->OnNativeForkAndSpecializePost(env, nice_name, app_data_dir);
        }

        void nativeForkSystemServerPre(JNIEnv *env, jclass, uid_t *, gid_t *,
                                       jintArray *, jint *,
                                       jobjectArray *, jlong *,
                                       jlong *) {
            Context::GetInstance()->OnNativeForkSystemServerPre(env);
        }

        void nativeForkSystemServerPost(JNIEnv *env, jclass, jint res) {
            if (res == 0)
                Context::GetInstance()->OnNativeForkSystemServerPost(env);
        }

        /* method added in Android Q */
        void specializeAppProcessPre(JNIEnv *env, jclass, jint *_uid, jint *,
                                     jintArray *gids, jint *,
                                     jobjectArray *, jint *,
                                     jstring *, jstring *_nice_name,
                                     jboolean *start_child_zygote, jstring *,
                                     jstring *_app_data_dir, jboolean *,
                                     jobjectArray *,
                                     jobjectArray *,
                                     jboolean *,
                                     jboolean *) {
            nice_name = *_nice_name;
            app_data_dir = *_app_data_dir;
            Context::GetInstance()->OnNativeForkAndSpecializePre(env, *_uid, *gids,
                                                                 nice_name,
                                                                 *start_child_zygote,
                                                                 app_data_dir);
        }

        void specializeAppProcessPost(JNIEnv *env, jclass) {
            Context::GetInstance()->OnNativeForkAndSpecializePost(env, nice_name, app_data_dir);
        }
    }

    RiruVersionedModuleInfo module{
            .moduleApiVersion = apiVersion,
            .moduleInfo = RiruModuleInfo{
                    .supportHide = !isDebug,
                    .version = versionCode,
                    .versionName = versionName,
                    .onModuleLoaded = lspd::onModuleLoaded,
                    .forkAndSpecializePre = lspd::nativeForkAndSpecializePre,
                    .forkAndSpecializePost = lspd::nativeForkAndSpecializePost,
                    .forkSystemServerPre = lspd::nativeForkSystemServerPre,
                    .forkSystemServerPost = lspd::nativeForkSystemServerPost,
                    .specializeAppProcessPre = lspd::specializeAppProcessPre,
                    .specializeAppProcessPost = lspd::specializeAppProcessPost,
            }
    };
}

RIRU_EXPORT RiruVersionedModuleInfo *init(Riru *riru) {
    LOGD("using riru %d", riru->riruApiVersion);
    LOGD("module path: %s", riru->magiskModulePath);
    lspd::magiskPath = riru->magiskModulePath;
    if (!lspd::isDebug && lspd::magiskPath.find(lspd::moduleName) == std::string::npos) {
        LOGE("who am i");
        return nullptr;
    }
    lspd::allowUnload = riru->allowUnload;
    return &lspd::module;
}
