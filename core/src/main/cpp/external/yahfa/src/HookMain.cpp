#include "jni.h"
#include <cstring>
#include <sys/mman.h>
#include <cstdlib>

#include "common.h"
#include "trampoline.h"
#include "HookMain.h"

int SDKVersion;
size_t OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;

namespace yahfa {
    namespace {
        constexpr size_t OFFSET_access_flags_in_ArtMethod = 4;
        constexpr uint32_t kAccCompileDontBother = 0x02000000;
        constexpr uint32_t kAccFastInterpreterToInterpreterInvoke = 0x40000000;

        size_t ArtMethodSize;
        uint32_t kAccPreCompiled = 0x00200000;

        jfieldID fieldArtMethod = nullptr;

        constexpr inline uint32_t read32(void *addr) {
            return *((uint32_t *) addr);
        }

        constexpr inline void write32(void *addr, uint32_t value) {
            *((uint32_t *) addr) = value;
        }

        constexpr inline void *readAddr(void *addr) {
            return *((void **) addr);
        }

        constexpr inline void writeAddr(void *addr, void *value) {
            *((void **) addr) = value;
        }

        void setNonCompilable(void *method) {
            uint32_t access_flags = getAccessFlags(method);
            LOGI("setNonCompilable: access flags is 0x%x", access_flags);
            access_flags |= kAccCompileDontBother;
            if (SDKVersion >= __ANDROID_API_R__)
                access_flags &= ~kAccPreCompiled;
            setAccessFlags(method, access_flags);
        }

        void setPrivate(void *method) {
            uint32_t access_flags = getAccessFlags(method);
            if (!(access_flags & kAccStatic)) {
                LOGI("setPrivate: access flags is 0x%x", access_flags);
                access_flags |= kAccPrivate;
                access_flags &= ~kAccProtected;
                access_flags &= ~kAccPublic;
                setAccessFlags(method, access_flags);
            }
        }

        int doBackupAndHook(void *targetMethod, void *hookMethod, void *backupMethod) {
            LOGI("target method is at %p, hook method is at %p, backup method is at %p",
                 targetMethod, hookMethod, backupMethod);


            // set kAccCompileDontBother for a method we do not want the compiler to compile
            // so that we don't need to worry about hotness_count_
            setNonCompilable(targetMethod);
            setNonCompilable(hookMethod);

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
                 readAddr((char *) targetMethod +
                          OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod),
                 newEntrypoint
            );
            if (newEntrypoint) {
                writeAddr((char *) targetMethod +
                          OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod,
                          newEntrypoint);
            } else {
                LOGE("failed to allocate space for trampoline of target method");
                return 1;
            }

            if (SDKVersion >= __ANDROID_API_Q__) {
                uint32_t access_flags = getAccessFlags(targetMethod);
                // On API 29 whether to use the fast path or not is cached in the ART method structure
                access_flags &= ~kAccFastInterpreterToInterpreterInvoke;
                setAccessFlags(targetMethod, access_flags);
            }

            LOGI("hook and backup done");
            return 0;
        }

    }

    void init(JNIEnv *env, [[maybe_unused]] jclass clazz, jint sdkVersion) {
        SDKVersion = sdkVersion;
        jclass classExecutable = env->FindClass("java/lang/reflect/Executable");
        fieldArtMethod = env->GetFieldID(classExecutable, "artMethod", "J");
        env->DeleteLocalRef(classExecutable);
        LOGI("init to SDK %d", sdkVersion);
        switch (sdkVersion) {
            case __ANDROID_API_S__:
                OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                        roundUpToPtrSize(4 * 3 + 2 * 2) + pointer_size;
                ArtMethodSize = roundUpToPtrSize(4 * 3 + 2 * 2) + pointer_size * 2;
                kAccPreCompiled = 0x00800000;
                break;
            case __ANDROID_API_R__:
            case __ANDROID_API_Q__:
            case __ANDROID_API_P__:
                OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                        roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size;
                ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
                break;
            case __ANDROID_API_O_MR1__:
                OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod =
                        roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 2;
                ArtMethodSize = roundUpToPtrSize(4 * 4 + 2 * 2) + pointer_size * 3;
                break;
            default:
                LOGE("not compatible with SDK %d", sdkVersion);
                break;
        }

        setupTrampoline();
    }

    void *getArtMethod(JNIEnv *env, jobject jmethod) {
        if (jmethod == nullptr) {
            return nullptr;
        } else {
            return (void *) env->GetLongField(jmethod, fieldArtMethod);
        }
    }

    uint32_t getAccessFlags(void *art_method) {

        return read32((char *) art_method + OFFSET_access_flags_in_ArtMethod);
        // On API 29 whether to use the fast path or not is cached in the ART method structure
    }

    void setAccessFlags(void *art_method, uint32_t access_flags) {
        write32((char *) art_method + OFFSET_access_flags_in_ArtMethod, access_flags);
    }

    jobject findMethodNative(JNIEnv *env, [[maybe_unused]] jclass clazz,
                             jclass targetClass,
                             jstring methodName,
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

    jboolean backupAndHookNative(JNIEnv *env, [[maybe_unused]] jclass clazz,
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
}
