#include <string>
#include <vector>
#include <unistd.h>
#include <mntent.h>
#include <jni.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <inject/config_manager.h>
#include <native_hook/native_hook.h>
#include <native_hook/resource_hook.h>
#include "java_hook/java_hook.h"
#include "include/logging.h"
#include "include/fd_utils-inl.h"

extern "C"
{
#include "HookMain.h"
}

jobject gInjectDexClassLoader;

static bool isInited = false;

static FileDescriptorTable *gClosedFdTable = nullptr;

void closeFilesBeforeForkNative(JNIEnv *, jclass) {
    // FIXME what if gClosedFdTable is not null
    gClosedFdTable = FileDescriptorTable::Create();
}

void reopenFilesAfterForkNative(JNIEnv *, jclass) {
    if (!gClosedFdTable) {
        LOGE("gClosedFdTable is null when reopening files");
        return;
    }
    gClosedFdTable->Reopen();
    delete gClosedFdTable;
    gClosedFdTable = nullptr;
}

jlong suspendAllThreads(JNIEnv *, jclass) {
    if (!suspendAll) {
        return 0;
    }
    ScopedSuspendAll *suspendAllObj = (ScopedSuspendAll *) malloc(sizeof(ScopedSuspendAll));
    suspendAll(suspendAllObj, "edxp_stop_gc", false);
    return reinterpret_cast<jlong>(suspendAllObj);
}

void resumeAllThreads(JNIEnv *, jclass, jlong obj) {
    if (!resumeAll) {
        return;
    }
    resumeAll(reinterpret_cast<ScopedSuspendAll *>(obj));
}

int waitForGcToComplete(JNIEnv *, jclass, jlong thread) {
    // if waitGc succeeded, it should return one of enum collector::GcType
    int gcType = waitGc(0, reinterpret_cast<void *>(thread));
    return gcType;
}

void setMethodNonCompilable(JNIEnv *env, jclass, jobject member) {
    if (!member) {
        LOGE("setNonCompilableNative: member is null");
        return;
    }
    void *artMethod = env->FromReflectedMethod(member);
    if (!artMethod) {
        LOGE("setNonCompilableNative: artMethod is null");
        return;
    }
    setNonCompilable(artMethod);
}

static constexpr uint32_t kAccFinal = 0x0010;

jboolean removeFinalFlag(JNIEnv *env, jclass, jclass clazz) {
    if (clazz) {
        jfieldID java_lang_Class_accessFlags = env->GetFieldID(
                env->FindClass("java/lang/Class"), "accessFlags", "I");
        jint access_flags = env->GetIntField(clazz, java_lang_Class_accessFlags);
        env->SetIntField(clazz, java_lang_Class_accessFlags, access_flags & ~kAccFinal);
        return true;
    }
    return false;
}

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
                "setMethodNonCompilable", "(Ljava/lang/Object;)V", (void *) setMethodNonCompilable
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
        },
        {
                "isBlackWhiteListEnabled", "()Z", (void *) is_black_white_list_enabled
        },
        {
                "isDynamicModulesEnabled", "()Z", (void *) is_dynamic_modules_enabled
        },
        {
                "isResourcesHookEnabled",  "()Z", (void *) is_resources_hook_enabled
        },
        {
                "isAppNeedHook", "(Ljava/lang/String;)Z", (void *) is_app_need_hook
        },
        {
                "getInstallerPkgName", "()Ljava/lang/String;", (void *) get_installer_pkg_name
        },
        {
                "closeFilesBeforeForkNative", "()V", (void *) closeFilesBeforeForkNative
        },
        {
                "reopenFilesAfterForkNative", "()V", (void *) reopenFilesAfterForkNative
        },
        {
                "deoptMethodNative", "(Ljava/lang/Object;)V", (void *) deoptimize_method
        },
        {
                "suspendAllThreads", "()J", (void *) suspendAllThreads
        },
        {
                "resumeAllThreads", "(J)V", (void *) resumeAllThreads
        },
        {
                "waitForGcToComplete", "(J)I", (void *) waitForGcToComplete
        },
        {
                "initXResourcesNative", "()Z", (void *) XposedBridge_initXResourcesNative
        },
        {
                "removeFinalFlagNative", "(Ljava/lang/Class;)Z", (void *) removeFinalFlag
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
        env->RegisterNatives(entry_class, hookMethods, NELEM(hookMethods));
        isInited = true;
        LOGD("RegisterNatives succeed for HookEntry.");
    } else {
        LOGE("HookEntry class is null. %d", getpid());
    }

    //load lib sandhook
    void* lib_sandhook;
    if (sizeof(void*) == 8) {
        lib_sandhook = dlopen("/system/lib64/libsandhook.edxp.so", RTLD_NOW);
    } else {
        lib_sandhook = dlopen("/system/lib/libsandhook.edxp.so", RTLD_NOW);
    }
    if (!lib_sandhook) {
        LOGW("libsandhook open failed. %s", dlerror());
        return;
    }
    bool* (*jni_load)(JNIEnv*, jclass, jclass) = reinterpret_cast<bool *(*)(JNIEnv *, jclass,
                                                                            jclass)>(dlsym(lib_sandhook, "JNI_Load_Ex"));

    jclass sandhook_class = findClassFromLoader(env, myClassLoader, CLASS_SAND_HOOK);
    jclass nevercall_class = findClassFromLoader(env, myClassLoader, CLASS_NEVER_CALL);
    if (!sandhook_class || !nevercall_class) { // fail-fast
        return;
    }
    if (!jni_load(env, sandhook_class, nevercall_class)) {
        LOGE("SandHook: HookEntry class error. %d", getpid());
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