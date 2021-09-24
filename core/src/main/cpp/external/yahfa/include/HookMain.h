#ifndef HOOK_MAIN_H
#define HOOK_MAIN_H

#include <jni.h>

namespace yahfa {
    constexpr uint32_t kAccPublic = 0x0001;  // class, field, method, ic
    constexpr uint32_t kAccPrivate = 0x0002;  // field, method, ic
    constexpr uint32_t kAccProtected = 0x0004;  // field, method, ic
    constexpr uint32_t kAccStatic = 0x0008;  // field, method, ic

    void init(JNIEnv *env, jclass clazz, jint sdkVersion);

    jobject findMethodNative(JNIEnv *env, jclass clazz,
                             jclass targetClass, jstring methodName,
                             jstring methodSig);

    jboolean backupAndHookNative(JNIEnv *env, jclass clazz,
                                 jobject target, jobject hook,
                                 jobject backup);

    void *getArtMethod(JNIEnv *env, jobject jmethod);

    uint32_t getAccessFlags(void* art_method);

    void setAccessFlags(void* art_method, uint32_t access_flags);
}

#endif // HOOK_MAIN_H
