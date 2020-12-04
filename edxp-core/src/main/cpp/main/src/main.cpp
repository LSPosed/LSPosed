#include <cstdio>
#include <unistd.h>
#include <fcntl.h>
#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <sys/mman.h>
#include <array>
#include <thread>
#include <vector>
#include <utility>
#include <string>
#include <android-base/logging.h>
#include "logging.h"
#include "config.h"
#include "edxp_context.h"
#include <riru.h>
#include "config_manager.h"
#include "native_hook.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"

namespace edxp {
    static void onModuleLoaded() {
        LOGI("onModuleLoaded: welcome to EdXposed!");
        // rirud must be used in onModuleLoaded
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
        Context::GetInstance()->OnNativeForkAndSpecializePost(env, clazz, res);
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

    static void nativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
        Context::GetInstance()->OnNativeForkSystemServerPost(env, clazz, res);
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

    static void specializeAppProcessPost(JNIEnv *env, jclass clazz) {
        Context::GetInstance()->OnNativeForkAndSpecializePost(env, clazz, 0);
    }
}

int riru_api_version;
RiruApiV10 *riru_api_v10;

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
                    riru_api_v10 = (RiruApiV10 *) arg;

                    auto module = (RiruModuleInfoV10 *) malloc(sizeof(RiruModuleInfoV10));
                    memset(module, 0, sizeof(RiruModuleInfoV10));
                    _module = module;

                    module->supportHide = true;

                    module->version = RIRU_MODULE_VERSION;
                    module->versionName = STRINGIFY(RIRU_MODULE_VERSION_NAME);
                    module->onModuleLoaded = edxp::onModuleLoaded;
                    module->shouldSkipUid = edxp::shouldSkipUid;
                    module->forkAndSpecializePre = edxp::nativeForkAndSpecializePre;
                    module->forkAndSpecializePost = edxp::nativeForkAndSpecializePost;
                    module->specializeAppProcessPre = edxp::specializeAppProcessPre;
                    module->specializeAppProcessPost = edxp::specializeAppProcessPost;
                    module->forkSystemServerPre = edxp::nativeForkSystemServerPre;
                    module->forkSystemServerPost = edxp::nativeForkSystemServerPost;
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

#pragma clang diagnostic pop