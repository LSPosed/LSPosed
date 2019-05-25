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
#include "inject/framework_hook.h"
#include "native_hook/native_hook.h"

#include "include/logging.h"
#include "include/misc.h"

#include "include/config.h"

extern "C" {

__attribute__((visibility("default"))) void onModuleLoaded() {
    LOGI("onModuleLoaded: welcome to EdXposed!");
    install_inline_hooks();
}

__attribute__((visibility("default"))) int shouldSkipUid(int uid) {
    return 0;
}

__attribute__((visibility("default"))) void
nativeForkAndSpecializePre(JNIEnv *env, jclass clazz, jint *_uid, jint *gid, jintArray *gids,
                           jint *runtime_flags,
                           jobjectArray *rlimits, jint *_mount_external, jstring *se_info,
                           jstring *se_name,
                           jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
                           jstring *instructionSet, jstring *appDataDir, jstring *packageName,
                           jobjectArray *packagesForUID, jstring *sandboxId) {
    onNativeForkAndSpecializePre(env, clazz, *_uid, *gid, *gids, *runtime_flags, *rlimits,
                                 *_mount_external, *se_info, *se_name, *fdsToClose, *fdsToIgnore,
                                 *is_child_zygote, *instructionSet, *appDataDir);
}

__attribute__((visibility("default"))) int nativeForkAndSpecializePost(JNIEnv *env, jclass clazz,
                                                                       jint res) {
    return onNativeForkAndSpecializePost(env, clazz, res);
}

__attribute__((visibility("default")))
void nativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t *uid, gid_t *gid, jintArray *gids,
                               jint *runtime_flags,
                               jobjectArray *rlimits, jlong *permittedCapabilities,
                               jlong *effectiveCapabilities) {
    onNativeForkSystemServerPre(env, clazz, *uid, *gid, *gids, *runtime_flags, *rlimits,
                                *permittedCapabilities, *effectiveCapabilities);
}

__attribute__((visibility("default")))
int nativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
    return onNativeForkSystemServerPost(env, clazz, res);
}

__attribute__((visibility("default"))) void specializeAppProcessPre(
        JNIEnv *env, jclass clazz, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jstring *packageName, jobjectArray *packagesForUID, jstring *sandboxId) {
    onNativeForkAndSpecializePre(env, clazz, *_uid, *gid, *gids, *runtimeFlags, *rlimits,
                                 *mountExternal, *seInfo, *niceName, nullptr, nullptr,
                                 *startChildZygote, *instructionSet, *appDataDir);
}

__attribute__((visibility("default"))) int specializeAppProcessPost(
        JNIEnv *env, jclass clazz) {
    return onNativeForkAndSpecializePost(env, clazz, 0);
}

}
