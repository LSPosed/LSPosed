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
#include "native_hook.h"
#include "logging.h"
#include "config.h"
#include "edxp_context.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"

#define EXPORT extern "C" __attribute__((visibility("default")))

namespace edxp {

    EXPORT void onModuleLoaded() {
        LOG(INFO) << "onModuleLoaded: welcome to EdXposed!";
        InstallInlineHooks();
    }

    EXPORT int shouldSkipUid(int uid) {
        return 0;
    }

    EXPORT void nativeForkAndSpecializePre(JNIEnv *env, jclass clazz, jint *_uid, jint *gid,
                                           jintArray *gids, jint *runtime_flags,
                                           jobjectArray *rlimits, jint *mount_external,
                                           jstring *se_info, jstring *nice_name,
                                           jintArray *fds_to_close, jintArray *fds_to_ignore,
                                           jboolean *start_child_zygote, jstring *instruction_set,
                                           jstring *app_data_dir,
            /* parameters added in Android Q */
                                           jstring *package_name, jobjectArray *packages_for_uid,
                                           jstring *sandbox_id) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, clazz, *_uid, *gid, *gids,
                                                             *runtime_flags, *rlimits,
                                                             *mount_external, *se_info, *nice_name,
                                                             *fds_to_close,
                                                             *fds_to_ignore,
                                                             *start_child_zygote, *instruction_set,
                                                             *app_data_dir);
    }

    EXPORT int nativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
        return Context::GetInstance()->OnNativeForkAndSpecializePost(env, clazz, res);
    }

    EXPORT void nativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t *uid, gid_t *gid,
                                          jintArray *gids, jint *runtime_flags,
                                          jobjectArray *rlimits, jlong *permitted_capabilities,
                                          jlong *effective_capabilities) {
        Context::GetInstance()->OnNativeForkSystemServerPre(env, clazz, *uid, *gid, *gids,
                                                            *runtime_flags, *rlimits,
                                                            *permitted_capabilities,
                                                            *effective_capabilities
        );
    }

    EXPORT int nativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
        return Context::GetInstance()->OnNativeForkSystemServerPost(env, clazz, res);
    }

    /* method added in Android Q */
    EXPORT void specializeAppProcessPre(JNIEnv *env, jclass clazz, jint *uid, jint *gid,
                                        jintArray *gids, jint *runtime_flags, jobjectArray *rlimits,
                                        jint *mount_external, jstring *se_info, jstring *nice_name,
                                        jboolean *start_child_zygote, jstring *instruction_set,
                                        jstring *app_data_dir, jstring *package_name,
                                        jobjectArray *packages_for_uid, jstring *sandbox_id) {
        Context::GetInstance()->OnNativeForkAndSpecializePre(env, clazz, *uid, *gid, *gids,
                                                             *runtime_flags, *rlimits,
                                                             *mount_external, *se_info, *nice_name,
                                                             nullptr, nullptr,
                                                             *start_child_zygote, *instruction_set,
                                                             *app_data_dir);
    }

    EXPORT int specializeAppProcessPost(JNIEnv *env, jclass clazz) {
        return Context::GetInstance()->OnNativeForkAndSpecializePost(env, clazz, 0);
    }

}

#pragma clang diagnostic pop