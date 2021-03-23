#ifndef HOOK_MAIN_H
#define HOOK_MAIN_H

#include <jni.h>

namespace yahfa {
    void init(JNIEnv *env, jclass clazz, jint sdkVersion);

    jobject findMethodNative(JNIEnv *env, jclass clazz,
                             jclass targetClass, jstring methodName,
                             jstring methodSig);

    jboolean backupAndHookNative(JNIEnv *env, jclass clazz,
                                 jobject target, jobject hook,
                                 jobject backup);

    void *getArtMethod(JNIEnv *env, jobject jmethod);
}

#endif // HOOK_MAIN_H
