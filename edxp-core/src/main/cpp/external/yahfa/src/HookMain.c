#include "jni.h"
#include <string.h>
#include <sys/mman.h>
#include <stdlib.h>
#include <stdbool.h>

#include "common.h"
#include "env.h"
#include "trampoline.h"
#include "HookMain.h"

int SDKVersion;
static int OFFSET_entry_point_from_interpreter_in_ArtMethod;
int OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
static int OFFSET_dex_method_index_in_ArtMethod;
static int OFFSET_dex_cache_resolved_methods_in_ArtMethod;
static int OFFSET_array_in_PointerArray;
static int OFFSET_ArtMehod_in_Object;
static int OFFSET_access_flags_in_ArtMethod;
static size_t ArtMethodSize;
static int kAccNative = 0x0100;
static int kAccCompileDontBother = 0x01000000;
static int kAccFastInterpreterToInterpreterInvoke = 0x40000000;

static inline uint16_t read16(void *addr) {
    return *((uint16_t *) addr);
}

static inline uint32_t read32(void *addr) {
    return *((uint32_t *) addr);
}

static inline void write32(void *addr, uint32_t value) {
    *((uint32_t *) addr) = value;
}

void Java_lab_galaxy_yahfa_HookMain_init(JNIEnv *env, jclass clazz, jint sdkVersion) {
    int i;
    SDKVersion = sdkVersion;
    LOGI("init to SDK %d", sdkVersion);
    switch (sdkVersion) {
        case ANDROID_Q:
        case ANDROID_P:
            kAccCompileDontBother = 0x02000000;
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_dex_method_index_in_ArtMethod = 4 * 3;
            OFFSET_array_in_PointerArray = 0;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size;
            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
            break;
        case ANDROID_O2:
            kAccCompileDontBother = 0x02000000;
        case ANDROID_O:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_dex_method_index_in_ArtMethod = 4 * 3;
            OFFSET_dex_cache_resolved_methods_in_ArtMethod = roundUpToPtrSize(4 * 4 + 2 * 2);
            OFFSET_array_in_PointerArray = 0;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;
            break;
        case ANDROID_N2:
        case ANDROID_N:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4; // sizeof(GcRoot<mirror::Class>) = 4
            OFFSET_dex_method_index_in_ArtMethod = 4 * 3;
            OFFSET_dex_cache_resolved_methods_in_ArtMethod = roundUpToPtrSize(4 * 4 + 2 * 2);
            OFFSET_array_in_PointerArray = 0;

            // ptr_sized_fields_ is rounded up to pointer_size in ArtMethod
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;

            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 4;
            break;
        case ANDROID_M:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            OFFSET_dex_method_index_in_ArtMethod = 4 * 5;
            OFFSET_dex_cache_resolved_methods_in_ArtMethod = 4;
            OFFSET_array_in_PointerArray = 4 * 3;
            ArtMethodSize = roundUpToPtrSize(4 * 7) + pointer_size * 3;
            break;
        case ANDROID_L2:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(
                    OFFSET_ArtMehod_in_Object + 4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            OFFSET_dex_method_index_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4 * 5;
            OFFSET_dex_cache_resolved_methods_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4;
            OFFSET_array_in_PointerArray = 12;
            ArtMethodSize = OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 3;
            break;
        case ANDROID_L:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4 * 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + 8 * 2;
            OFFSET_dex_method_index_in_ArtMethod =
                    OFFSET_ArtMehod_in_Object + 4 * 4 + 8 * 4 + 4 * 2;
            OFFSET_dex_cache_resolved_methods_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4;
            OFFSET_array_in_PointerArray = 12;
            ArtMethodSize = OFFSET_ArtMehod_in_Object + 4 * 4 + 8 * 4 + 4 * 4;
            break;
        default:
            LOGE("not compatible with SDK %d", sdkVersion);
            break;
    }

    setupTrampoline();
}

void setNonCompilable(void *method) {
    if (SDKVersion < ANDROID_N) {
        return;
    }
    int access_flags = read32((char *) method + OFFSET_access_flags_in_ArtMethod);
    LOGI("setNonCompilable: access flags is 0x%x", access_flags);
    access_flags |= kAccCompileDontBother;
    write32((char *) method + OFFSET_access_flags_in_ArtMethod, access_flags);
}

bool setNativeFlag(void *method, bool isNative) {
    int access_flags = read32((char *) method + OFFSET_access_flags_in_ArtMethod);
    LOGI("setNativeFlag: access flags is 0x%x", access_flags);
    int old_access_flags = access_flags;
    if (isNative) {
        access_flags |= kAccNative;
        if (SDKVersion >= ANDROID_Q) {
            // On API 29 whether to use the fast path or not is cached in the ART method structure
            access_flags &= ~kAccFastInterpreterToInterpreterInvoke;
        }
    } else {
        access_flags &= ~kAccNative;
    }
    if (access_flags != old_access_flags) {
        write32((char *) method + OFFSET_access_flags_in_ArtMethod, access_flags);
        return true;
    }
    return false;
}

static int doBackupAndHook(JNIEnv *env, void *targetMethod, void *hookMethod, void *backupMethod) {
    if (hookCount >= hookCap) {
        LOGI("not enough capacity. Allocating...");
        if (doInitHookCap(DEFAULT_CAP)) {
            LOGE("cannot hook method");
            return 1;
        }
        LOGI("Allocating done");
    }

    LOGI("target method is at %p, hook method is at %p, backup method is at %p",
         targetMethod, hookMethod, backupMethod);


    // set kAccCompileDontBother for a method we do not want the compiler to compile
    // so that we don't need to worry about hotness_count_
    if (SDKVersion >= ANDROID_N) {
        setNonCompilable(targetMethod);
        setNonCompilable(hookMethod);
    }

    if (backupMethod) {// do method backup
        // have to copy the whole target ArtMethod here
        // if the target method calls other methods which are to be resolved
        // then ToDexPC would be invoked for the caller(origin method)
        // in which case ToDexPC would use the entrypoint as a base for mapping pc to dex offset
        // so any changes to the target method's entrypoint would result in a wrong dex offset
        // and artQuickResolutionTrampoline would fail for methods called by the origin method
        memcpy(backupMethod, targetMethod, ArtMethodSize);
    }

    // replace entry point
    void *newEntrypoint = genTrampoline(hookMethod);
    LOGI("origin ep is %p, new ep is %p",
         readAddr((char *) targetMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod),
         newEntrypoint
    );
    if (newEntrypoint) {
        memcpy((char *) targetMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod,
               &newEntrypoint,
               pointer_size);
    } else {
        LOGE("failed to allocate space for trampoline of target method");
        return 1;
    }

    if (OFFSET_entry_point_from_interpreter_in_ArtMethod != 0) {
        memcpy((char *) targetMethod + OFFSET_entry_point_from_interpreter_in_ArtMethod,
               (char *) hookMethod + OFFSET_entry_point_from_interpreter_in_ArtMethod,
               pointer_size);
    }

    // set the target method to native so that Android O wouldn't invoke it with interpreter
    if (SDKVersion >= ANDROID_O) {
        setNativeFlag(targetMethod, true);
        LOGI("access flags is 0x%x", access_flags);
    }

    LOGI("hook and backup done");
    hookCount += 1;
    return 0;
}

static void ensureMethodCached(void *hookMethod, void *backupMethod,
                               void *hookClassResolvedMethods) {
    void *dexCacheResolvedMethods;
    // then we get the dex method index of the static backup method
    int methodIndex = read32(
            (void *) ((char *) backupMethod + OFFSET_dex_method_index_in_ArtMethod));

    // todo fixme
    if (methodIndex >= 512) {
        LOGW("methodIndex = %d", methodIndex);
    }

    // update the cached method manually
    // first we find the array of cached methods
    dexCacheResolvedMethods = hookClassResolvedMethods;

    if (!dexCacheResolvedMethods) {
        LOGE("dexCacheResolvedMethods is null");
        return;
    }

    // finally the addr of backup method is put at the corresponding location in cached methods array
    if (SDKVersion >= ANDROID_O2) {
        // array of MethodDexCacheType is used as dexCacheResolvedMethods in Android 8.1
        // struct:
        // struct NativeDexCachePair<T> = { T*, size_t idx }
        // MethodDexCachePair = NativeDexCachePair<ArtMethod> = { ArtMethod*, size_t idx }
        // MethodDexCacheType = std::atomic<MethodDexCachePair>
        memcpy((char *) dexCacheResolvedMethods + OFFSET_array_in_PointerArray +
               pointer_size * 2 * methodIndex,
               (&backupMethod),
               pointer_size
        );
        memcpy((char *) dexCacheResolvedMethods + OFFSET_array_in_PointerArray +
               pointer_size * 2 * methodIndex + pointer_size,
               &methodIndex,
               pointer_size
        );
    } else {
        memcpy((char *) dexCacheResolvedMethods + OFFSET_array_in_PointerArray +
               pointer_size * methodIndex,
               (&backupMethod),
               pointer_size);
    }
}

jobject Java_lab_galaxy_yahfa_HookMain_findMethodNative(JNIEnv *env, jclass clazz,
                                                        jclass targetClass, jstring methodName,
                                                        jstring methodSig) {
    const char *c_methodName = (*env)->GetStringUTFChars(env, methodName, NULL);
    const char *c_methodSig = (*env)->GetStringUTFChars(env, methodSig, NULL);
    jobject ret = NULL;


    //Try both GetMethodID and GetStaticMethodID -- Whatever works :)
    jmethodID method = (*env)->GetMethodID(env, targetClass, c_methodName, c_methodSig);
    if (!(*env)->ExceptionCheck(env)) {
        ret = (*env)->ToReflectedMethod(env, targetClass, method, JNI_FALSE);
    } else {
        (*env)->ExceptionClear(env);
        method = (*env)->GetStaticMethodID(env, targetClass, c_methodName, c_methodSig);
        if (!(*env)->ExceptionCheck(env)) {
            ret = (*env)->ToReflectedMethod(env, targetClass, method, JNI_TRUE);
        } else {
            (*env)->ExceptionClear(env);
        }
    }

    (*env)->ReleaseStringUTFChars(env, methodName, c_methodName);
    (*env)->ReleaseStringUTFChars(env, methodSig, c_methodSig);
    return ret;
}

jboolean Java_lab_galaxy_yahfa_HookMain_backupAndHookNative(JNIEnv *env, jclass clazz,
                                                            jobject target, jobject hook,
                                                            jobject backup) {

    if (!doBackupAndHook(env,
                         (void *) (*env)->FromReflectedMethod(env, target),
                         (void *) (*env)->FromReflectedMethod(env, hook),
                         backup == NULL ? NULL : (void *) (*env)->FromReflectedMethod(env, backup)
    )) {
        (*env)->NewGlobalRef(env,
                             hook); // keep a global ref so that the hook method would not be GCed
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

void Java_lab_galaxy_yahfa_HookMain_ensureMethodCached(JNIEnv *env, jclass clazz,
                                                       jobject hook,
                                                       jobject backup) {
    ensureMethodCached((void *) (*env)->FromReflectedMethod(env, hook),
                       backup == NULL ? NULL : (void *) (*env)->FromReflectedMethod(env, backup),
                       getResolvedMethodsAddr(env, hook));
}

static void *getResolvedMethodsAddr(JNIEnv *env, jobject hook) {
    // get backup class
    jclass methodClass = (*env)->FindClass(env, "java/lang/reflect/Method");
    jmethodID getClassMid = (*env)->GetMethodID(env, methodClass, "getDeclaringClass",
                                                "()Ljava/lang/Class;");
    jclass backupClass = (*env)->CallObjectMethod(env, hook, getClassMid);
    // get dexCache of backup class
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jfieldID dexCacheFid = (*env)->GetFieldID(env, classClass, "dexCache", "Ljava/lang/Object;");
    jobject dexCacheObj = (*env)->GetObjectField(env, backupClass, dexCacheFid);
    // get resolvedMethods address
    jclass dexCacheClass = (*env)->GetObjectClass(env, dexCacheObj);
    if (SDKVersion >= ANDROID_N) {
        jfieldID resolvedMethodsFid = (*env)->GetFieldID(env, dexCacheClass, "resolvedMethods",
                                                         "J");
        return (void *) (*env)->GetLongField(env, dexCacheObj, resolvedMethodsFid);
    } else if (SDKVersion >= ANDROID_L) {
        LOGE("this should has been done in java world: %d", SDKVersion);
        return 0;
    } else {
        LOGE("not compatible with SDK %d", SDKVersion);
        return 0;
    }
}
