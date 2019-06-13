#pragma once

#include <jni.h>
#include <art/base/macros.h>
#include "logging.h"

#define JNI_START JNIEnv *env, jclass clazz

ALWAYS_INLINE static void JNIExceptionClear(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
}

ALWAYS_INLINE static bool JNIExceptionCheck(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        jthrowable e = env->ExceptionOccurred();
        env->Throw(e);
        env->DeleteLocalRef(e);
        return true;
    }
    return false;
}

ALWAYS_INLINE static void JNIExceptionClearAndDescribe(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

ALWAYS_INLINE static int ClearException(JNIEnv *env) {
    jthrowable exception = env->ExceptionOccurred();
    if (exception != nullptr) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

#define JNI_FindClass(env, name) \
    env->FindClass(name); \
    if (ClearException(env)) LOGE("FindClass " #name);

#define JNI_GetObjectClass(env, obj) \
    env->GetObjectClass(obj); \
    if (ClearException(env)) LOGE("GetObjectClass " #obj);

#define JNI_GetFieldID(env, class, name, sig) \
    env->GetFieldID(class, name, sig); \
    if (ClearException(env)) LOGE("GetFieldID " #name);

#define JNI_GetObjectField(env, class, fieldId) \
    env->GetObjectField(class, fieldId); \
    if (ClearException(env)) LOGE("GetObjectField " #fieldId);

#define JNI_GetMethodID(env, class, name, sig) \
    env->GetMethodID(class, name, sig); \
    if (ClearException(env)) LOGE("GetMethodID " #name);

#define JNI_CallObjectMethod(env, obj, ...) \
    env->CallObjectMethod(obj, __VA_ARGS__); \
    if (ClearException(env)) LOGE("CallObjectMethod " #obj " " #__VA_ARGS__);

#define JNI_CallVoidMethod(env, obj, ...) \
    env->CallVoidMethod(obj, __VA_ARGS__); \
    if (ClearException(env)) LOGE("CallVoidMethod " #obj " " #__VA_ARGS__);

#define JNI_GetStaticFieldID(env, class, name, sig) \
    env->GetStaticFieldID(class, name, sig); \
    if (ClearException(env)) LOGE("GetStaticFieldID " #name " " #sig);

#define JNI_GetStaticObjectField(env, class, fieldId) \
    env->GetStaticObjectField(class, fieldId); \
    if (ClearException(env)) LOGE("GetStaticObjectField " #fieldId);

#define JNI_GetStaticMethodID(env, class, name, sig) \
    env->GetStaticMethodID(class, name, sig); \
    if (ClearException(env)) LOGE("GetStaticMethodID " #name);

#define JNI_CallStaticVoidMethod(env, obj, ...) \
    env->CallStaticVoidMethod(obj, __VA_ARGS__); \
    if (ClearException(env)) LOGE("CallStaticVoidMethod " #obj " " #__VA_ARGS__);

#define JNI_CallStaticObjectMethod(env, obj, ...) \
    env->CallStaticObjectMethod(obj, __VA_ARGS__); \
    if (ClearException(env)) LOGE("CallStaticObjectMethod " #obj " " #__VA_ARGS__);

#define JNI_CallStaticIntMethod(env, obj, ...) \
    env->CallStaticIntMethod(obj, __VA_ARGS__); \
    if (ClearException(env)) LOGE("CallStaticIntMethod " #obj " " #__VA_ARGS__);

#define JNI_GetArrayLength(env, array) \
    env->GetArrayLength(array); \
    if (ClearException(env)) LOGE("GetArrayLength " #array);

#define JNI_NewObject(env, class, ...) \
    env->NewObject(class, __VA_ARGS__); \
    if (ClearException(env)) LOGE("NewObject " #class " " #__VA_ARGS__);

#define JNI_RegisterNatives(env, class, methods, size) \
    env->RegisterNatives(class, methods, size); \
    if (ClearException(env)) LOGE("RegisterNatives " #class);

