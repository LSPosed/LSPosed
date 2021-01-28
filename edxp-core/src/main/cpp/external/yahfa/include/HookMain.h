#ifndef HOOK_MAIN_H
#define HOOK_MAIN_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

void Java_lab_galaxy_yahfa_HookMain_init(JNIEnv *env, jclass clazz, jint sdkVersion);

jobject Java_lab_galaxy_yahfa_HookMain_findMethodNative(JNIEnv *env, jclass clazz,
                                                        jclass targetClass, jstring methodName,
                                                        jstring methodSig);

jboolean Java_lab_galaxy_yahfa_HookMain_backupAndHookNative(JNIEnv *env, jclass clazz,
                                                            jobject target, jobject hook,
                                                            jobject backup);

void setNonCompilable(void *method);

void *getArtMethod(JNIEnv *env, jobject jmethod);

#ifdef __cplusplus
}
#endif

#endif // HOOK_MAIN_H
