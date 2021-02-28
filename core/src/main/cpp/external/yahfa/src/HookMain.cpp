#include "jni.h"
#include <cstring>
#include <sys/mman.h>
#include <cstdlib>

#include "common.h"
#include "trampoline.h"
#include "HookMain.h"

int SDKVersion;
static int OFFSET_entry_point_from_interpreter_in_ArtMethod;
int OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
static int OFFSET_ArtMehod_in_Object;
static int OFFSET_access_flags_in_ArtMethod;
static size_t ArtMethodSize;
static uint32_t kAccCompileDontBother = 0x01000000;
static uint32_t kAccPublic = 0x0001;  // class, field, method, ic
static uint32_t kAccPrivate = 0x0002;  // field, method, ic
static uint32_t kAccProtected = 0x0004;  // field, method, ic
static uint32_t kAccStatic = 0x0008;  // field, method, ic
static uint32_t kAccFastInterpreterToInterpreterInvoke = 0x40000000;


static jfieldID fieldArtMethod = nullptr;

static inline uint32_t read32(void *addr) {
    return *((uint32_t *) addr);
}

static inline void write32(void *addr, uint32_t value) {
    *((uint32_t *) addr) = value;
}

static inline void *readAddr(void *addr) {
    return *((void **) addr);
}

static inline void writeAddr(void *addr, void *value) {
    *((void **) addr) = value;
}

extern "C" void Java_lab_galaxy_yahfa_HookMain_init(JNIEnv *env, jclass clazz, jint sdkVersion) {
    SDKVersion = sdkVersion;
    jclass classExecutable;
    LOGI("init to SDK %d", sdkVersion);
    switch (sdkVersion) {
        case __ANDROID_API_S__:
            classExecutable = env->FindClass("java/lang/reflect/Executable");
            fieldArtMethod = env->GetFieldID(classExecutable, "artMethod", "J");
            kAccCompileDontBother = 0x02000000;
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 3 + 2 * 2) + pointer_size;
            ArtMethodSize = roundUpToPtrSize(4 * 3 + 2 * 2) + pointer_size * 2;
            break;
        case __ANDROID_API_R__:
            classExecutable = env->FindClass("java/lang/reflect/Executable");
            fieldArtMethod = env->GetFieldID(classExecutable, "artMethod", "J");
        case __ANDROID_API_Q__:
        case __ANDROID_API_P__:
            kAccCompileDontBother = 0x02000000;
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size;
            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
            break;
        case __ANDROID_API_O_MR1__:
            kAccCompileDontBother = 0x02000000;
        case __ANDROID_API_O__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;
            break;
        case __ANDROID_API_N_MR1__:
        case __ANDROID_API_N__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4; // sizeof(GcRoot<mirror::Class>) = 4
            // ptr_sized_fields_ is rounded up to pointer_size in ArtMethod
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;

            ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 4;
            break;
        case __ANDROID_API_M__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            ArtMethodSize = roundUpToPtrSize(4 * 7) + pointer_size * 3;
            break;
        case __ANDROID_API_L_MR1__:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(
                    OFFSET_ArtMehod_in_Object + 4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            ArtMethodSize = OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 3;
            break;
        case __ANDROID_API_L__:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4 * 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + 8 * 2;
            ArtMethodSize = OFFSET_ArtMehod_in_Object + 4 * 4 + 8 * 4 + 4 * 4;
            break;
        default:
            LOGE("not compatible with SDK %d", sdkVersion);
            break;
    }

    setupTrampoline();
}

void setNonCompilable(void *method) {
    if (SDKVersion < __ANDROID_API_N__) {
        return;
    }
    uint32_t access_flags = read32((char *) method + OFFSET_access_flags_in_ArtMethod);
    LOGI("setNonCompilable: access flags is 0x%x", access_flags);
    access_flags |= kAccCompileDontBother;
    write32((char *) method + OFFSET_access_flags_in_ArtMethod, access_flags);
}

void setPrivate(void *method) {
    uint32_t access_flags = read32((char *) method + OFFSET_access_flags_in_ArtMethod);
    if (!(access_flags & kAccStatic)) {
        LOGI("setPrivate: access flags is 0x%x", access_flags);
        access_flags |= kAccPrivate;
        access_flags &= ~kAccProtected;
        access_flags &= ~kAccPublic;
        write32((char *) method + OFFSET_access_flags_in_ArtMethod, access_flags);
    }
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
    if (SDKVersion >= __ANDROID_API_N__) {
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
        setPrivate(backupMethod);
    }

    // replace entry point
    void *newEntrypoint = genTrampoline(hookMethod);
    LOGI("origin ep is %p, new ep is %p",
         readAddr((char *) targetMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod),
         newEntrypoint
    );
    if (newEntrypoint) {
        writeAddr((char *) targetMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod,
                  newEntrypoint);
    } else {
        LOGE("failed to allocate space for trampoline of target method");
        return 1;
    }

    if (OFFSET_entry_point_from_interpreter_in_ArtMethod != 0) {
        writeAddr((char *) targetMethod + OFFSET_entry_point_from_interpreter_in_ArtMethod,
                  readAddr((char *) hookMethod + OFFSET_entry_point_from_interpreter_in_ArtMethod));

    }

    if (SDKVersion >= __ANDROID_API_Q__) {
        uint32_t access_flags = read32((char *) targetMethod + OFFSET_access_flags_in_ArtMethod);
        // On API 29 whether to use the fast path or not is cached in the ART method structure
        access_flags &= ~kAccFastInterpreterToInterpreterInvoke;
        write32((char *) targetMethod + OFFSET_access_flags_in_ArtMethod, access_flags);
    }

    LOGI("hook and backup done");
    hookCount += 1;
    return 0;
}

void *getArtMethodYahfa(JNIEnv *env, jobject jmethod) {
    void *artMethod = nullptr;

    if (jmethod == nullptr) {
        return artMethod;
    }

    if (SDKVersion >= __ANDROID_API_R__) {
        artMethod = (void *) env->GetLongField(jmethod, fieldArtMethod);
    } else {
        artMethod = (void *) env->FromReflectedMethod(jmethod);
    }

    LOGI("ArtMethod: %p", artMethod);
    return artMethod;
}

extern "C" jobject Java_lab_galaxy_yahfa_HookMain_findMethodNative(JNIEnv *env, jclass clazz,
                                                        jclass targetClass, jstring methodName,
                                                        jstring methodSig) {
    const char *c_methodName = env->GetStringUTFChars(methodName, nullptr);
    const char *c_methodSig = env->GetStringUTFChars(methodSig, nullptr);
    jobject ret = nullptr;


    //Try both GetMethodID and GetStaticMethodID -- Whatever works :)
    jmethodID method = env->GetMethodID(targetClass, c_methodName, c_methodSig);
    if (!env->ExceptionCheck()) {
        ret = env->ToReflectedMethod(targetClass, method, JNI_FALSE);
    } else {
        env->ExceptionClear();
        method = env->GetStaticMethodID(targetClass, c_methodName, c_methodSig);
        if (!env->ExceptionCheck()) {
            ret = env->ToReflectedMethod(targetClass, method, JNI_TRUE);
        } else {
            env->ExceptionClear();
        }
    }

    env->ReleaseStringUTFChars(methodName, c_methodName);
    env->ReleaseStringUTFChars(methodSig, c_methodSig);
    return ret;
}

extern "C" jboolean Java_lab_galaxy_yahfa_HookMain_backupAndHookNative(JNIEnv *env, jclass clazz,
                                                            jobject target, jobject hook,
                                                            jobject backup) {

    if (!doBackupAndHook(env,
                         getArtMethodYahfa(env, target),
                         getArtMethodYahfa(env, hook),
                         getArtMethodYahfa(env, backup)
    )) {
        env->NewGlobalRef(hook); // keep a global ref so that the hook method would not be GCed
        if (backup) env->NewGlobalRef(backup);
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}
