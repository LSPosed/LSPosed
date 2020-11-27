#ifndef RIRU_H
#define RIRU_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <jni.h>
#include <sys/types.h>

// ---------------------------------------------------------

typedef void(onModuleLoaded_v9)();

typedef int(shouldSkipUid_v9)(int uid);

typedef void(nativeForkAndSpecializePre_v9)(
        JNIEnv *env, jclass cls, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp, jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs);

typedef void(nativeForkAndSpecializePost_v9)(JNIEnv *env, jclass cls, jint res);

typedef void(nativeForkSystemServerPre_v9)(
        JNIEnv *env, jclass cls, uid_t *uid, gid_t *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jlong *permittedCapabilities, jlong *effectiveCapabilities);

typedef void(nativeForkSystemServerPost_v9)(JNIEnv *env, jclass cls, jint res);

typedef void(nativeSpecializeAppProcessPre_v9)(
        JNIEnv *env, jclass cls, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs);

typedef void(nativeSpecializeAppProcessPost_v9)(JNIEnv *env, jclass cls);

typedef struct {
    int supportHide;
    int version;
    const char *versionName;
    onModuleLoaded_v9 *onModuleLoaded;
    shouldSkipUid_v9 *shouldSkipUid;
    nativeForkAndSpecializePre_v9 *forkAndSpecializePre;
    nativeForkAndSpecializePost_v9 *forkAndSpecializePost;
    nativeForkSystemServerPre_v9 *forkSystemServerPre;
    nativeForkSystemServerPost_v9 *forkSystemServerPost;
    nativeSpecializeAppProcessPre_v9 *specializeAppProcessPre;
    nativeSpecializeAppProcessPost_v9 *specializeAppProcessPost;
} RiruModuleInfoV9;

// ---------------------------------------------------------

typedef void *(RiruGetFunc_v9)(uint32_t token, const char *name);

typedef void (RiruSetFunc_v9)(uint32_t token, const char *name, void *func);

typedef void *(RiruGetJNINativeMethodFunc_v9)(uint32_t token, const char *className, const char *name, const char *signature);

typedef void (RiruSetJNINativeMethodFunc_v9)(uint32_t token, const char *className, const char *name, const char *signature, void *func);

typedef const JNINativeMethod *(RiruGetOriginalJNINativeMethodFunc_v9)(const char *className, const char *name, const char *signature);

typedef void *(RiruGetGlobalValue_v9)(const char *key);

typedef void(RiruPutGlobalValue_v9)(const char *key, void *value);

typedef struct {

    uint32_t token;
    RiruGetFunc_v9 *getFunc;
    RiruGetJNINativeMethodFunc_v9 *getJNINativeMethodFunc;
    RiruSetFunc_v9 *setFunc;
    RiruSetJNINativeMethodFunc_v9 *setJNINativeMethodFunc;
    RiruGetOriginalJNINativeMethodFunc_v9 *getOriginalJNINativeMethodFunc;
    RiruGetGlobalValue_v9 *getGlobalValue;
    RiruPutGlobalValue_v9 *putGlobalValue;
} RiruApiV9;

typedef void *(RiruInit_t)(void *);

#ifdef RIRU_MODULE
#define RIRU_EXPORT __attribute__((visibility("default"))) __attribute__((used))

/*
 * Init will be called three times.
 *
 * The first time:
 *   Returns the highest version number supported by both Riru and the module.
 *
 *   arg: (int *) Riru's API version
 *   returns: (int *) the highest possible API version
 *
 * The second time:
 *   Returns the RiruModuleX struct created by the module (X is the return of the first call).
 *
 *   arg: (RiruApiVX *) RiruApi strcut, this pointer can be saved for further use
 *   returns: (RiruModuleX *) RiruModule strcut
 *
 * The second time:
 *   Let the module to cleanup (such as RiruModuleX struct created before).
 *
 *   arg: null
 *   returns: (ignored)
 *
 */
void* init(void *arg) RIRU_EXPORT;

extern int riru_api_version;
extern RiruApiV9 *riru_api_v9;

inline void *riru_get_func(const char *name) {
    if (riru_api_version == 9) {
        return riru_api_v9->getFunc(riru_api_v9->token, name);
    }
    return NULL;
}

inline void *riru_get_native_method_func(const char *className, const char *name, const char *signature) {
    if (riru_api_version == 9) {
        return riru_api_v9->getJNINativeMethodFunc(riru_api_v9->token, className, name, signature);
    }
    return NULL;
}

inline const JNINativeMethod *riru_get_original_native_methods(const char *className, const char *name, const char *signature) {
    if (riru_api_version == 9) {
        return riru_api_v9->getOriginalJNINativeMethodFunc(className, name, signature);
    }
    return NULL;
}

inline void riru_set_func(const char *name, void *func) {
    if (riru_api_version == 9) {
        riru_api_v9->setFunc(riru_api_v9->token, name, func);
    }
}

inline void riru_set_native_method_func(const char *className, const char *name, const char *signature,
                                 void *func) {
    if (riru_api_version == 9) {
        riru_api_v9->setJNINativeMethodFunc(riru_api_v9->token, className, name, signature, func);
    }
}

inline void *riru_get_global_value(const char *key) {
    if (riru_api_version == 9) {
        return riru_api_v9->getGlobalValue(key);
    }
    return NULL;
}

inline void riru_put_global_value(const char *key, void *value) {
    if (riru_api_version == 9) {
        riru_api_v9->putGlobalValue(key, value);
    }
}

#endif

#ifdef __cplusplus
}
#endif

#endif //RIRU_H