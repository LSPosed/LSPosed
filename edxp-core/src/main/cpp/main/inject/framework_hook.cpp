

#include <java_hook/java_hook.h>
#include <unistd.h>
#include <include/logging.h>
#include "framework_hook.h"
#include "include/misc.h"
#include "config_manager.h"

#define SYSTEM_SERVER_DATA_DIR "/data/user/0/android"

static jclass sEntryClass;
static jstring sAppDataDir;
static jstring sNiceName;

void prepareJavaEnv(JNIEnv *env) {
    loadDexAndInit(env, INJECT_DEX_PATH);
    sEntryClass = findClassFromLoader(env, gInjectDexClassLoader, ENTRY_CLASS_NAME);
}

void findAndCall(JNIEnv *env, const char *methodName, const char *methodSig, ...) {
    if (!sEntryClass) {
        LOGE("cannot call method %s, entry class is null", methodName);
        return;
    }
    jmethodID mid = env->GetStaticMethodID(sEntryClass, methodName, methodSig);
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        LOGE("method %s not found in entry class", methodName);
        mid = NULL;
    }
    if (mid) {
        va_list args;
        va_start(args, methodSig);
        env->functions->CallStaticVoidMethodV(env, sEntryClass, mid, args);
        va_end(args);
    } else {
        LOGE("method %s id is null", methodName);
    }
}

void onNativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t uid, gid_t gid, jintArray gids,
                                 jint runtime_flags, jobjectArray rlimits,
                                 jlong permittedCapabilities, jlong effectiveCapabilities) {
    sAppDataDir = env->NewStringUTF(SYSTEM_SERVER_DATA_DIR);
    bool is_black_white_list_mode = is_black_white_list_enabled();
    bool is_dynamic_modules_mode = is_dynamic_modules_enabled();
    if (is_black_white_list_mode && is_dynamic_modules_mode) {
        // when black/white list is on, never inject into zygote if dynamic modules mode is on
        return;
    }
    prepareJavaEnv(env);
    // jump to java code
    findAndCall(env, "forkSystemServerPre", "(II[II[[IJJ)V", uid, gid, gids, runtime_flags,
                rlimits, permittedCapabilities, effectiveCapabilities);
}


int onNativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        prepareJavaEnv(env);
        // only do work in child since findAndCall would print log
        findAndCall(env, "forkSystemServerPost", "(I)V", res);
    } else {
        // in zygote process, res is child zygote pid
        // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
    }
    return 0;
}

void onNativeForkAndSpecializePre(JNIEnv *env, jclass clazz,
                                  jint uid, jint gid,
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
                                  jstring appDataDir) {
    sAppDataDir = appDataDir;
    sNiceName = se_name;
    bool is_black_white_list_mode = is_black_white_list_enabled();
    bool is_dynamic_modules_mode = is_dynamic_modules_enabled();
    if (is_black_white_list_mode && is_dynamic_modules_mode) {
        // when black/white list is on, never inject into zygote if dynamic modules mode is on
        return;
    }
    prepareJavaEnv(env);
    findAndCall(env, "forkAndSpecializePre",
                "(II[II[[IILjava/lang/String;Ljava/lang/String;[I[IZLjava/lang/String;Ljava/lang/String;)V",
                uid, gid, gids, runtime_flags, rlimits,
                _mount_external, se_info, se_name, fdsToClose, fdsToIgnore,
                is_child_zygote, instructionSet, appDataDir);
}

int onNativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
    if (res == 0) {
        prepareJavaEnv(env);
        findAndCall(env, "forkAndSpecializePost", "(ILjava/lang/String;Ljava/lang/String;)V", res, sAppDataDir, sNiceName);
    } else {
        // in zygote process, res is child zygote pid
        // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
    }
    return 0;
}