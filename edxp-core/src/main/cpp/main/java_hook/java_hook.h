#ifndef _JAVAHOOK_H
#define _JAVAHOOK_H

#include <jni.h>
#include <unistd.h>

#define NELEM(x) (sizeof(x)/sizeof((x)[0]))

extern jobject gInjectDexClassLoader;

void loadDexAndInit(JNIEnv *env, const char *dexPath);

jclass findClassFromLoader(JNIEnv *env, jobject classLoader, const char *className);

#endif // _JAVAHOOK_H