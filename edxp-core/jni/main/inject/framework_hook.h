
#ifndef RIRU_FRAMEWORK_HOOK_H
#define RIRU_FRAMEWORK_HOOK_H


#include <jni.h>

void onNativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t uid, gid_t gid, jintArray gids,
                                 jint runtime_flags, jobjectArray rlimits,
                                 jlong permittedCapabilities, jlong effectiveCapabilities);

int onNativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res);

void onNativeForkAndSpecializePre(JNIEnv *env, jclass clazz,
                                  jint _uid, jint gid,
                                  jintArray gids,
                                  jint runtime_flags,
                                  jobjectArray rlimits,
                                  jint _mount_external,
                                  jstring se_info,
                                  jstring se_name,
                                  jintArray fdsToClose,
                                  jintArray fdsToIgnore,
                                  jboolean is_child_zygote,
                                  jstring instructionSet,
                                  jstring appDataDir);

int onNativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res);

#endif //RIRU_FRAMEWORK_HOOK_H
