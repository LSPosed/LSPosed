//
// Created by 甘尧 on 2019/1/13.
//

#ifndef SANDHOOK_UTILS_H
#define SANDHOOK_UTILS_H

#include <stdlib.h>
#include <sys/mman.h>
#include "jni.h"
#include "../includes/arch.h"
#include <unistd.h>
#include <sys/mman.h>
#include <type_traits>

#define RoundUpToPtrSize(x) (x + BYTE_POINT - 1 - ((x + BYTE_POINT - 1) & (BYTE_POINT - 1)))

extern "C" {

Size getAddressFromJava(JNIEnv *env, const char *className, const char *fieldName);

Size callStaticMethodAddr(JNIEnv *env, const char *className, const char *method, const char *sig, ...);

jobject callStaticMethodObject(JNIEnv *env, const char *className, const char *method, const char *sig, ...);

jobject getMethodObject(JNIEnv *env, const char *clazz, const char *method);

Size getAddressFromJavaByCallMethod(JNIEnv *env, const char *className, const char *methodName);

jint getIntFromJava(JNIEnv *env, const char *className, const char *fieldName);

bool getBooleanFromJava(JNIEnv *env, const char *className, const char *fieldName);

bool munprotect(size_t addr, size_t len);

bool flushCacheExt(Size addr, Size len);

}


#endif //SANDHOOK_UTILS_H
