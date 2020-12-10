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

void Java_lab_galaxy_yahfa_HookMain_ensureMethodCached(JNIEnv *env, jclass clazz,
                                                       jobject hook,
                                                       jobject backup);

void setNonCompilable(void *method);

bool setNativeFlag(void *method, bool isNative);

void *getArtMethod(JNIEnv *env, jobject jmethod);

// TODO: move to common utils instead of in YAHFA's code
void *getEntryPoint(void* method);

void setEntryPoint(void* method, void* entry);

#ifdef __cplusplus
}
#endif

#endif // HOOK_MAIN_H