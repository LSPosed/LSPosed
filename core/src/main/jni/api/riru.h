#ifndef RIRU_H
#define RIRU_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <jni.h>
#include <sys/types.h>
#include <stddef.h>

// ---------------------------------------------------------

typedef void(onModuleLoaded_v9)();

#ifndef RIRU_MODULE
typedef int(shouldSkipUid_v9)(int uid);
#endif

typedef void(nativeForkAndSpecializePre_v9)(
        JNIEnv *env, jclass cls, jint *uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp,
        jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs,
        jboolean *bindMountAppStorageDirs);

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
#ifndef RIRU_MODULE
    shouldSkipUid_v9 *shouldSkipUid;
#else
    void *unused;
#endif
    nativeForkAndSpecializePre_v9 *forkAndSpecializePre;
    nativeForkAndSpecializePost_v9 *forkAndSpecializePost;
    nativeForkSystemServerPre_v9 *forkSystemServerPre;
    nativeForkSystemServerPost_v9 *forkSystemServerPost;
    nativeSpecializeAppProcessPre_v9 *specializeAppProcessPre;
    nativeSpecializeAppProcessPost_v9 *specializeAppProcessPost;
} RiruModuleInfo;

typedef struct {
    int moduleApiVersion;
    RiruModuleInfo moduleInfo;
} RiruVersionedModuleInfo;

// ---------------------------------------------------------

typedef struct {
    int riruApiVersion;
    void *unused;
    const char *magiskModulePath;
    int *allowUnload;
} Riru;

typedef RiruVersionedModuleInfo *(RiruInit_t)(Riru *);

#ifdef RIRU_MODULE
#define RIRUD_ADDRESS "rirud"


#if __cplusplus < 201103L
#define RIRU_EXPORT __attribute__((visibility("default"))) __attribute__((used))
#else
#define RIRU_EXPORT [[gnu::visibility("default")]] [[gnu::used]]
#endif

RIRU_EXPORT RiruVersionedModuleInfo *init(Riru *riru) ;

extern int riru_api_version;
extern const char *riru_magisk_module_path;
extern int *riru_allow_unload;

#if !__cplusplus && __STDC_VERSION__ < 199409L
#define RIRU_INLINE __attribute__((weak)) __inline__
#elif !__cplusplus
#define RIRU_INLINE  __attribute__((weak)) inline extern
#else
#define RIRU_INLINE inline
#endif

RIRU_INLINE const char *riru_get_magisk_module_path() {
    if (riru_api_version >= 24) {
        return riru_magisk_module_path;
    }
    return NULL;
}

RIRU_INLINE void riru_set_unload_allowed(int allowed) {
    if (riru_api_version >= 25 && riru_allow_unload) {
        *riru_allow_unload = allowed;
    }
}
#undef RIRU_INLINE

#endif

#ifdef __cplusplus
}
#endif

#endif //RIRU_H
