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
#include "config_manager.h"
#include "symbol_cache.h"

namespace lspd {
    static void onModuleLoaded() {
        LOGI("onModuleLoaded: welcome to LSPosed!");
        // rirud must be used in onModuleLoaded
        InitSymbolCache();
        ConfigManager::Init();
    }

    static int shouldSkipUid(int uid) {
        return 0;
    }

    static void nativeForkAndSpecializePre(JNIEnv *env, jclass clazz, jint *_uid, jint *gid,
                                           jintArray *gids, jint *runtime_flags,
                                           jobjectArray *rlimits, jint *mount_external,
                                           jstring *se_info, jstring *nice_name,
                                           jintArray *fds_to_close, jintArray *fds_to_ignore,
                                           jboolean *start_child_zygote, jstring *instruction_set,
                                           jstring *app_data_dir, jboolean *is_top_app, jobjectArray *pkg_data_info_list,
                                           jobjectArray *whitelisted_data_info_list, jboolean *bind_mount_app_data_dirs,
                                           jboolean *bind_mount_app_storage_dirs) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, clazz, *_uid, *gid, *gids,
                                                             *runtime_flags, *rlimits,
                                                             *mount_external, *se_info, *nice_name,
                                                             *fds_to_close,
                                                             *fds_to_ignore,
                                                             *start_child_zygote, *instruction_set,
                                                             *app_data_dir);
    }

    static void nativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
        Context::GetInstance()->OnNativeForkAndSpecializePost(env);
    }

    static void nativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t *uid, gid_t *gid,
                                          jintArray *gids, jint *runtime_flags,
                                          jobjectArray *rlimits, jlong *permitted_capabilities,
                                          jlong *effective_capabilities) {
        Context::GetInstance()->OnNativeForkSystemServerPre(env, clazz, *uid, *gid, *gids,
                                                            *runtime_flags, *rlimits,
                                                            *permitted_capabilities,
                                                            *effective_capabilities
        );
    }

    static void nativeForkSystemServerPost(JNIEnv *env, [[maybe_unused]] jclass clazz, jint res) {
        Context::GetInstance()->OnNativeForkSystemServerPost(env, res);
    }

    /* method added in Android Q */
    static void specializeAppProcessPre(JNIEnv *env, jclass clazz, jint *uid, jint *gid,
                                        jintArray *gids, jint *runtime_flags, jobjectArray *rlimits,
                                        jint *mount_external, jstring *se_info, jstring *nice_name,
                                        jboolean *start_child_zygote, jstring *instruction_set,
                                        jstring *app_data_dir, jboolean *is_top_app, jobjectArray *pkg_data_info_list,
                                        jobjectArray *whitelisted_data_info_list, jboolean *bind_mount_app_data_dirs,
                                        jboolean *bind_mount_app_storage_dirs) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, clazz, *uid, *gid, *gids,
                                                             *runtime_flags, *rlimits,
                                                             *mount_external, *se_info, *nice_name,
                                                             nullptr, nullptr,
                                                             *start_child_zygote, *instruction_set,
                                                             *app_data_dir);
    }

    static void specializeAppProcessPost(JNIEnv *env, [[maybe_unused]] jclass clazz) {
        Context::GetInstance()->OnNativeForkAndSpecializePost(env);
    }
}

int riru_api_version;

RIRU_EXPORT void *init(void *arg) {
    static int step = 0;
    step += 1;

    static void *_module;

    switch (step) {
        case 1: {
            auto core_max_api_version = *(int *) arg;
            riru_api_version = core_max_api_version <= RIRU_MODULE_API_VERSION ? core_max_api_version : RIRU_MODULE_API_VERSION;
            return &riru_api_version;
        }
        case 2: {
            switch (riru_api_version) {
                case 10:
                    [[fallthrough]];
                case 9: {
                    auto module = (RiruModuleInfoV10 *) malloc(sizeof(RiruModuleInfoV10));
                    memset(module, 0, sizeof(RiruModuleInfoV10));
                    _module = module;

                    module->supportHide = true;

                    module->version = RIRU_MODULE_VERSION;
                    module->versionName = STRINGIFY(RIRU_MODULE_VERSION_NAME);
                    module->onModuleLoaded = lspd::onModuleLoaded;
                    module->shouldSkipUid = lspd::shouldSkipUid;
                    module->forkAndSpecializePre = lspd::nativeForkAndSpecializePre;
                    module->forkAndSpecializePost = lspd::nativeForkAndSpecializePost;
                    module->specializeAppProcessPre = lspd::specializeAppProcessPre;
                    module->specializeAppProcessPost = lspd::specializeAppProcessPost;
                    module->forkSystemServerPre = lspd::nativeForkSystemServerPre;
                    module->forkSystemServerPost = lspd::nativeForkSystemServerPost;
                    return module;
                }
                default: {
                    return nullptr;
                }
            }
        }
        case 3: {
            free(_module);
            return nullptr;
        }
        default: {
            return nullptr;
        }
    }
}
