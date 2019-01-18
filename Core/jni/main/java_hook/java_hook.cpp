#include <string>
#include <vector>
#include <unistd.h>
#include <mntent.h>
#include <jni.h>
#include <fcntl.h>
#include <dlfcn.h>
#include "java_hook/java_hook.h"
#include "include/logging.h"

extern "C"
{
#include "../yahfa/HookMain.h"
}

jobject gInjectDexClassLoader;

static bool isInited = false;

static JNINativeMethod hookMethods[] = {
        {
                "init",
                "(I)V",
                (void *) Java_lab_galaxy_yahfa_HookMain_init
        },
        {
                "backupAndHookNative",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Z",
                (void *) Java_lab_galaxy_yahfa_HookMain_backupAndHookNative
        },
        {
                "findMethodNative",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
                (void *) Java_lab_galaxy_yahfa_HookMain_findMethodNative
        },
        {
                "ensureMethodCached",
                "(Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",
                (void *) Java_lab_galaxy_yahfa_HookMain_ensureMethodCached
        }
};

void loadDexAndInit(JNIEnv *env, const char *dexPath) {
    if (isInited) {
        return;
    }
    jclass clzClassLoader = env->FindClass("java/lang/ClassLoader");
    LOGD("java/lang/ClassLoader: %p", clzClassLoader);
    jmethodID mdgetSystemClassLoader = env->GetStaticMethodID(clzClassLoader,
                                                              "getSystemClassLoader",
                                                              "()Ljava/lang/ClassLoader;");
    LOGD("java/lang/ClassLoader.getSystemClassLoader method: %p", mdgetSystemClassLoader);
    jobject systemClassLoader = env->CallStaticObjectMethod(clzClassLoader, mdgetSystemClassLoader);
    LOGD("java/lang/ClassLoader.getSystemClassLoader method result: %p", systemClassLoader);
    if (NULL == systemClassLoader) {
        LOGE("getSystemClassLoader failed!!!");
        return;
    }
    jclass clzPathClassLoader = env->FindClass("dalvik/system/PathClassLoader");
    LOGD("dalvik/system/PathClassLoader: %p", clzClassLoader);
    jmethodID mdinitPathCL = env->GetMethodID(clzPathClassLoader, "<init>",
                                              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    LOGD("dalvik/system/PathClassLoader.<init>: %p", clzClassLoader);
    jstring jarpath_str = env->NewStringUTF(dexPath);
    jobject myClassLoader = env->NewObject(clzPathClassLoader, mdinitPathCL,
                                           jarpath_str, NULL, systemClassLoader);
    if (NULL == myClassLoader) {
        LOGE("PathClassLoader creation failed!!!");
        return;
    }
    gInjectDexClassLoader = env->NewGlobalRef(myClassLoader);
    LOGD("PathClassLoader created: %p", myClassLoader);
    LOGD("PathClassLoader loading dexPath[%s]\n", dexPath);
    jclass entry_class = findClassFromLoader(env, myClassLoader, ENTRY_CLASS_NAME);
    if (NULL != entry_class) {
        LOGD("HookEntry Class %p", entry_class);
        env->RegisterNatives(entry_class, hookMethods, 4);
        isInited = true;
        LOGD("RegisterNatives succeed for HookEntry.");
    } else {
        LOGE("HookEntry class is null. %d", getpid());
    }
}

jstring getThrowableMessage(JNIEnv *env, jobject throwable) {
    if (!throwable) {
        LOGE("throwable is null.");
        return NULL;
    }
    jclass jthrowableClass = env->GetObjectClass(throwable);
    jmethodID getMsgMid = env->GetMethodID(jthrowableClass, "getMessage", "()Ljava/lang/String;");
    if (getMsgMid == 0) {
        LOGE("get Throwable.getMessage method id failed.");
        return NULL;
    }
    return (jstring) env->CallObjectMethod(throwable, getMsgMid);
}

jclass findClassFromLoader(JNIEnv *env, jobject classLoader, const char *className) {
    jclass clz = env->GetObjectClass(classLoader);
    jmethodID mid = env->GetMethodID(clz, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (env->ExceptionOccurred()) {
        LOGE("loadClass method not found");
        env->ExceptionClear();
    } else {
        LOGD("loadClass method %p", mid);
    }
    jclass ret = NULL;
    if (!mid) {
        mid = env->GetMethodID(clz, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        if (env->ExceptionOccurred()) {
            LOGE("findClass method not found");
            env->ExceptionClear();
        } else {
            LOGD("findClass method %p", mid);
        }
    }
    if (mid) {
        jstring className_str = env->NewStringUTF(className);
        jobject tmp = env->CallObjectMethod(classLoader, mid, className_str);
        jthrowable exception = env->ExceptionOccurred();
        if (exception) {
            jstring message = getThrowableMessage(env, exception);
            const char *message_char = env->GetStringUTFChars(message, JNI_FALSE);
            LOGE("Error when findClass %s: %s", className, message_char);
            env->ReleaseStringUTFChars(message, message_char);
            env->ExceptionClear();
        }
        if (NULL != tmp) {
            LOGD("findClassFromLoader %p", tmp);
            ret = (jclass) tmp;
        }
    } else {
        LOGE("no method found");
    }
    if (ret == NULL) {
        LOGE("class %s not found.", className);
    }
    return ret;
}