#include "jni.h"
#include <sys/mman.h>
#include <cstdlib>
#include <unordered_map>

#include "common.h"
#include "HookMain.h"
extern "C" {
#include "trampoline.h"
}

int SDKVersion;
static uint32_t OFFSET_entry_point_from_interpreter_in_ArtMethod;
static uint32_t OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
static uint32_t OFFSET_ArtMehod_in_Object;
static uint32_t OFFSET_access_flags_in_ArtMethod;
static uint32_t kAccCompileDontBother = 0x01000000;

static jfieldID fieldArtMethod = nullptr;
//static std::unordered_map<void*, void*> replaced_entrypoint;

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
    *((void **)addr) = value;
}

extern "C" void Java_lab_galaxy_yahfa_HookMain_init(JNIEnv *env, jclass clazz, jint sdkVersion) {
    SDKVersion = sdkVersion;
    jclass classExecutable;
    LOGI("init to SDK %d", sdkVersion);
    switch (sdkVersion) {
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
            break;
        case __ANDROID_API_O_MR1__:
            kAccCompileDontBother = 0x02000000;
        case __ANDROID_API_O__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
            break;
        case __ANDROID_API_N_MR1__:
        case __ANDROID_API_N__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_access_flags_in_ArtMethod = 4; // sizeof(GcRoot<mirror::Class>) = 4

            // ptr_sized_fields_ is rounded up to pointer_size in ArtMethod
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;

            break;
        case __ANDROID_API_M__:
            OFFSET_ArtMehod_in_Object = 0;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            break;
        case __ANDROID_API_L_MR1__:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = roundUpToPtrSize(
                    OFFSET_ArtMehod_in_Object + 4 * 7);
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + pointer_size * 2;
            break;
        case __ANDROID_API_L__:
            OFFSET_ArtMehod_in_Object = 4 * 2;
            OFFSET_entry_point_from_interpreter_in_ArtMethod = OFFSET_ArtMehod_in_Object + 4 * 4;
            OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                    OFFSET_entry_point_from_interpreter_in_ArtMethod + 8 * 2;
            break;
        default:
            LOGE("not compatible with SDK %d", sdkVersion);
            break;
    }

    setupTrampoline(OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod);
}

static uint32_t getFlags(void *method_) {
    char* method = (char*)method_;
    uint32_t access_flags = read32(method + OFFSET_access_flags_in_ArtMethod);
    return access_flags;
}

static void setFlags(void *method_, uint32_t access_flags) {
    char* method = (char*)method_;
    write32(method + OFFSET_access_flags_in_ArtMethod, access_flags);
}

void setNonCompilable(void *method) {
    if (SDKVersion < __ANDROID_API_N__) {
        return;
    }
    uint32_t access_flags = getFlags(method);
    uint32_t old_flags = access_flags;
    access_flags |= kAccCompileDontBother;
    setFlags(method, access_flags);
    LOGI("setNonCompilable: change access flags from 0x%x to 0x%x", old_flags, access_flags);
}

void *getEntryPoint(void* method) {
    return readAddr((char *) method + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod);
}

static int replaceMethod(void *fromMethod, void *toMethod, int isBackup) {
    // replace entry point
    void *newEntrypoint = nullptr;
    if(isBackup) {
        void *originEntrypoint = readAddr((char *) toMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod);
        // entry point hardcoded
        newEntrypoint = genTrampoline(toMethod, originEntrypoint);
    }
    else {
        // entry point from ArtMethod struct
        newEntrypoint = genTrampoline(toMethod, nullptr);
    }

    void* fromEntrypoint = (char *) fromMethod + OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
    //replaced_entrypoint[fromEntrypoint] = newEntrypoint;

    LOGI("replace entry point from %p to %p",
         readAddr(fromEntrypoint),
         newEntrypoint
    );
    if (newEntrypoint) {
        writeAddr(fromEntrypoint,
                newEntrypoint);
    } else {
        LOGE("failed to allocate space for trampoline of target method");
        return 1;
    }

    // For pre Android M devices, should be not used by EdXposed.
    if (OFFSET_entry_point_from_interpreter_in_ArtMethod != 0) {
        void *interpEntrypoint = readAddr((char *) toMethod + OFFSET_entry_point_from_interpreter_in_ArtMethod);
        writeAddr(fromEntrypoint,
                interpEntrypoint);
    }

    return 0;
}

static int doBackupAndHook(void *targetMethod, void *hookMethod, void *backupMethod) {
    LOGI("target method is at %p, hook method is at %p, backup method is at %p",
         targetMethod, hookMethod, backupMethod);

    int res = 0;

    // set kAccCompileDontBother for a method we do not want the compiler to compile
    // so that we don't need to worry about hotness_count_
    if (SDKVersion >= __ANDROID_API_N__) {
        setNonCompilable(targetMethod);
//        setNonCompilable(hookMethod);
        if(backupMethod) setNonCompilable(backupMethod);
    }

    if (backupMethod) {// do method backup
        // we use the same way as hooking target method
        // hook backup method and redirect back to the original target method
        // the only difference is that the entry point is now hardcoded
        // instead of reading from ArtMethod struct since it's overwritten
        res += replaceMethod(backupMethod, targetMethod, 1);
    }

    res += replaceMethod(targetMethod, hookMethod, 0);

    LOGI("hook and backup done");
    return res;
}

void *getArtMethod(JNIEnv *env, jobject jmethod) {
    void *artMethod = nullptr;

    if (jmethod == nullptr) {
        return artMethod;
    }

    if (SDKVersion == __ANDROID_API_R__) {
        artMethod = (void *) env->GetLongField(jmethod, fieldArtMethod);
    } else {
        artMethod = (void *) env->FromReflectedMethod(jmethod);
    }

    LOGI("HookMain: getArtMethod: %p", artMethod);
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

    if (!doBackupAndHook(getArtMethod(env, target),
                         getArtMethod(env, hook),
                         getArtMethod(env, backup)
    )) {
        env->NewGlobalRef(hook); // keep a global ref so that the hook method would not be GCed
        if (backup) env->NewGlobalRef(backup);
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}
