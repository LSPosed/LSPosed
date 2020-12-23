#include "includes/sandhook.h"
#include "includes/cast_art_method.h"
#include "includes/trampoline_manager.h"
#include "includes/hide_api.h"
#include "includes/cast_compiler_options.h"
#include "includes/log.h"
#include "includes/native_hook.h"
#include "includes/elf_util.h"
#include "includes/never_call.h"
#include <jni.h>

SandHook::TrampolineManager &trampolineManager = SandHook::TrampolineManager::get();

extern "C" int SDK_INT = 0;
extern "C" bool DEBUG = false;

enum HookMode {
    AUTO = 0,
    INLINE = 1,
    REPLACE = 2
};

HookMode gHookMode = AUTO;

void ensureMethodCached(art::mirror::ArtMethod *hookMethod, art::mirror::ArtMethod *backupMethod) {
    if (SDK_INT >= ANDROID_P)
        return;

    SandHook::StopTheWorld stopTheWorld;

    uint32_t index = backupMethod->getDexMethodIndex();
    if (SDK_INT < ANDROID_O2) {
        hookMethod->setDexCacheResolveItem(index, backupMethod);
    } else {
        int cacheSize = 1024;
        Size slotIndex = index % cacheSize;
        Size newCachedMethodsArray = reinterpret_cast<Size>(calloc(cacheSize, BYTE_POINT * 2));
        unsigned int one = 1;
        memcpy(reinterpret_cast<void *>(newCachedMethodsArray + BYTE_POINT), &one, 4);
        memcpy(reinterpret_cast<void *>(newCachedMethodsArray + BYTE_POINT * 2 * slotIndex),
               (&backupMethod),
               BYTE_POINT
        );
        memcpy(reinterpret_cast<void *>(newCachedMethodsArray + BYTE_POINT * 2 * slotIndex + BYTE_POINT),
               &index,
               4
        );
        hookMethod->setDexCacheResolveList(&newCachedMethodsArray);
    }
}

void ensureDeclareClass(JNIEnv *env, jclass type, jobject originMethod,
                         jobject backupMethod) {
    if (originMethod == NULL || backupMethod == NULL)
        return;
    art::mirror::ArtMethod* origin = getArtMethod(env, originMethod);
    art::mirror::ArtMethod* backup = getArtMethod(env, backupMethod);
    if (origin->getDeclaringClass() != backup->getDeclaringClass()) {
        LOGW("declaring class has been moved!");
        backup->setDeclaringClass(origin->getDeclaringClass());
    }
}

bool doHookWithReplacement(JNIEnv* env,
                           art::mirror::ArtMethod *originMethod,
                           art::mirror::ArtMethod *hookMethod,
                           art::mirror::ArtMethod *backupMethod) {

    if (!hookMethod->compile(env)) {
        hookMethod->disableCompilable();
    }

    if (SDK_INT > ANDROID_N && SDK_INT < ANDROID_Q) {
        forceProcessProfiles();
    }
    if ((SDK_INT >= ANDROID_N && SDK_INT <= ANDROID_P)
        || (SDK_INT >= ANDROID_Q && !originMethod->isAbstract())) {
        originMethod->setHotnessCount(0);
    }

    if (backupMethod != nullptr) {
        originMethod->backup(backupMethod);
        backupMethod->disableCompilable();
        if (!backupMethod->isStatic()) {
            backupMethod->setPrivate();
        }
        backupMethod->flushCache();
    }

    originMethod->disableCompilable();
    hookMethod->disableCompilable();
    hookMethod->flushCache();

    originMethod->disableInterpreterForO();
    originMethod->disableFastInterpreterForQ();

    SandHook::HookTrampoline* hookTrampoline = trampolineManager.installReplacementTrampoline(originMethod, hookMethod, backupMethod);
    if (hookTrampoline != nullptr) {
        originMethod->setQuickCodeEntry(hookTrampoline->replacement->getCode());
        void* entryPointFormInterpreter = hookMethod->getInterpreterCodeEntry();
        if (entryPointFormInterpreter != NULL) {
            originMethod->setInterpreterCodeEntry(entryPointFormInterpreter);
        }
        if (hookTrampoline->callOrigin != nullptr) {
            backupMethod->setQuickCodeEntry(hookTrampoline->callOrigin->getCode());
            backupMethod->flushCache();
        }
        originMethod->flushCache();
        return true;
    } else {
        return false;
    }
}

bool doHookWithInline(JNIEnv* env,
                      art::mirror::ArtMethod *originMethod,
                      art::mirror::ArtMethod *hookMethod,
                      art::mirror::ArtMethod *backupMethod) {

    //fix >= 8.1
    if (!hookMethod->compile(env)) {
        hookMethod->disableCompilable();
    }

    originMethod->disableCompilable();
    if (SDK_INT > ANDROID_N && SDK_INT < ANDROID_Q) {
        forceProcessProfiles();
    }
    if ((SDK_INT >= ANDROID_N && SDK_INT <= ANDROID_P)
        || (SDK_INT >= ANDROID_Q && !originMethod->isAbstract())) {
        originMethod->setHotnessCount(0);
    }
    originMethod->flushCache();

    SandHook::HookTrampoline* hookTrampoline = trampolineManager.installInlineTrampoline(originMethod, hookMethod, backupMethod);

    if (hookTrampoline == nullptr)
        return false;

    hookMethod->flushCache();
    if (hookTrampoline->callOrigin != nullptr) {
        //backup
        originMethod->backup(backupMethod);
        backupMethod->setQuickCodeEntry(hookTrampoline->callOrigin->getCode());
        backupMethod->disableCompilable();
        if (!backupMethod->isStatic()) {
            backupMethod->setPrivate();
        }
        backupMethod->flushCache();
    }
    return true;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_initNative(JNIEnv *env, jclass type, jint sdk, jboolean debug) {
    SDK_INT = sdk;
    DEBUG = debug;
    SandHook::CastCompilerOptions::init(env);
    initHideApi(env);
    SandHook::CastArtMethod::init(env);
    trampolineManager.init(SandHook::CastArtMethod::entryPointQuickCompiled->getOffset());
    return JNI_TRUE;

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_swift_sandhook_SandHook_hookMethod(JNIEnv *env, jclass type, jobject originMethod,
                                            jobject hookMethod, jobject backupMethod, jint hookMode) {

    art::mirror::ArtMethod* origin = getArtMethod(env, originMethod);
    art::mirror::ArtMethod* hook = getArtMethod(env, hookMethod);
    art::mirror::ArtMethod* backup = backupMethod == NULL ? nullptr : getArtMethod(env,
                                                                                   backupMethod);

    bool isInlineHook = false;

    int mode = reinterpret_cast<int>(hookMode);

    if (mode == INLINE) {
        if (!origin->isCompiled()) {
            if (SDK_INT >= ANDROID_N) {
                isInlineHook = origin->compile(env);
            }
        } else {
            isInlineHook = true;
        }
        goto label_hook;
    } else if (mode == REPLACE) {
        isInlineHook = false;
        goto label_hook;
    }

    if (origin->isAbstract()) {
        isInlineHook = false;
    } else if (gHookMode != AUTO) {
        if (gHookMode == INLINE) {
            isInlineHook = origin->compile(env);
        } else {
            isInlineHook = false;
        }
    } else if (SDK_INT >= ANDROID_O) {
        isInlineHook = false;
    } else if (!origin->isCompiled()) {
        if (SDK_INT >= ANDROID_N) {
            isInlineHook = origin->compile(env);
        } else {
            isInlineHook = false;
        }
    } else {
        isInlineHook = true;
    }


label_hook:
    //suspend other threads
    SandHook::StopTheWorld stopTheWorld;
    if (isInlineHook && trampolineManager.canSafeInline(origin)) {
        return doHookWithInline(env, origin, hook, backup) ? INLINE : -1;
    } else {
        return doHookWithReplacement(env, origin, hook, backup) ? REPLACE : -1;
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_SandHook_ensureMethodCached(JNIEnv *env, jclass type, jobject hook,
                                                    jobject backup) {
    art::mirror::ArtMethod* hookeMethod = getArtMethod(env, hook);
    art::mirror::ArtMethod* backupMethod = backup == NULL ? nullptr : getArtMethod(env, backup);
    ensureMethodCached(hookeMethod, backupMethod);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_compileMethod(JNIEnv *env, jclass type, jobject member) {

    if (member == NULL)
        return JNI_FALSE;
    art::mirror::ArtMethod* method = getArtMethod(env, member);

    if (method == nullptr)
        return JNI_FALSE;

    if (!method->isCompiled()) {
        SandHook::StopTheWorld stopTheWorld;
        if (!method->compile(env)) {
            if (SDK_INT >= ANDROID_N) {
                method->disableCompilable();
                method->flushCache();
            }
            return JNI_FALSE;
        } else {
            return JNI_TRUE;
        }
    } else {
        return JNI_TRUE;
    }

}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_deCompileMethod(JNIEnv *env, jclass type, jobject member, jboolean disableJit) {

    if (member == NULL)
        return JNI_FALSE;
    art::mirror::ArtMethod* method = getArtMethod(env, member);

    if (method == nullptr)
        return JNI_FALSE;

    if (disableJit) {
        method->disableCompilable();
    }

    if (method->isCompiled()) {
        SandHook::StopTheWorld stopTheWorld;
        if (SDK_INT >= ANDROID_N) {
            method->disableCompilable();
        }
        return static_cast<jboolean>(method->deCompile());
    } else {
        return JNI_TRUE;
    }

}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_swift_sandhook_SandHook_getObjectNative(JNIEnv *env, jclass type, jlong thread,
                                                 jlong address) {
    return getJavaObject(env, thread ? reinterpret_cast<void *>(thread) : getCurrentThread(), reinterpret_cast<void *>(address));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_canGetObject(JNIEnv *env, jclass type) {
    return static_cast<jboolean>(canGetObject());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_SandHook_setHookMode(JNIEnv *env, jclass type, jint mode) {
    gHookMode = static_cast<HookMode>(mode);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_SandHook_setInlineSafeCheck(JNIEnv *env, jclass type, jboolean check) {
    trampolineManager.inlineSecurityCheck = check;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_SandHook_skipAllSafeCheck(JNIEnv *env, jclass type, jboolean skip) {
    trampolineManager.skipAllCheck = skip;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_is64Bit(JNIEnv *env, jclass type) {
    return static_cast<jboolean>(BYTE_POINT == 8);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_disableVMInline(JNIEnv *env, jclass type) {
    if (SDK_INT < ANDROID_N)
        return JNI_FALSE;
    replaceUpdateCompilerOptionsQ();
    art::CompilerOptions* compilerOptions = getGlobalCompilerOptions();
    if (compilerOptions == nullptr)
        return JNI_FALSE;
    return static_cast<jboolean>(disableJitInline(compilerOptions));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_disableDex2oatInline(JNIEnv *env, jclass type, jboolean disableDex2oat) {
    return static_cast<jboolean>(SandHook::NativeHook::hookDex2oat(disableDex2oat));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_setNativeEntry(JNIEnv *env, jclass type, jobject origin, jobject hook, jlong jniTrampoline) {
    if (origin == nullptr || hook == NULL)
        return JNI_FALSE;
    art::mirror::ArtMethod* hookMethod = getArtMethod(env, hook);
    art::mirror::ArtMethod* originMethod = getArtMethod(env, origin);
    originMethod->backup(hookMethod);
    hookMethod->setNative();
    hookMethod->setQuickCodeEntry(SandHook::CastArtMethod::genericJniStub);
    hookMethod->setJniCodeEntry(reinterpret_cast<void *>(jniTrampoline));
    hookMethod->disableCompilable();
    hookMethod->flushCache();
    return JNI_TRUE;
}


static jclass class_pending_hook = nullptr;
static jmethodID method_class_init = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_swift_sandhook_SandHook_initForPendingHook(JNIEnv *env, jclass type) {
    class_pending_hook = static_cast<jclass>(env->NewGlobalRef(
            env->FindClass("com/swift/sandhook/PendingHookHandler")));
    method_class_init = env->GetStaticMethodID(class_pending_hook, "onClassInit", "(J)V");
    auto class_init_handler = [](void *clazz_ptr) {
        attachAndGetEvn()->CallStaticVoidMethod(class_pending_hook, method_class_init, (jlong) clazz_ptr);
        attachAndGetEvn()->ExceptionClear();
    };
    return static_cast<jboolean>(hookClassInit(class_init_handler));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_ClassNeverCall_neverCallNative(JNIEnv *env, jobject instance) {
    int a = 1 + 1;
    int b = a + 1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_ClassNeverCall_neverCallNative2(JNIEnv *env, jobject instance) {
    int a = 4 + 3;
    int b = 9 + 6;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_swift_sandhook_test_TestClass_jni_1test(JNIEnv *env, jobject instance) {
    int a = 1 + 1;
    int b = a + 1;
}

//native hook
extern "C"
JNIEXPORT bool nativeHookNoBackup(void* origin, void* hook) {

    if (origin == nullptr || hook == nullptr)
        return false;

    SandHook::StopTheWorld stopTheWorld;

    return trampolineManager.installNativeHookTrampolineNoBackup(origin, hook) != nullptr;

}

extern "C"
JNIEXPORT void* findSym(const char *elf, const char *sym_name) {
    SandHook::ElfImg elfImg(elf);
    return reinterpret_cast<void *>(elfImg.getSymbAddress(sym_name));
}

static JNINativeMethod jniSandHook[] = {
        {
                "initNative",
                "(IZ)Z",
                (void *) Java_com_swift_sandhook_SandHook_initNative
        },
        {
                "hookMethod",
                "(Ljava/lang/reflect/Member;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;I)I",
                (void *) Java_com_swift_sandhook_SandHook_hookMethod
        },
        {
                "ensureMethodCached",
                "(Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",
                (void *) Java_com_swift_sandhook_SandHook_ensureMethodCached
        },
        {
                "ensureDeclareClass",
                "(Ljava/lang/reflect/Member;Ljava/lang/reflect/Method;)V",
                (void *) ensureDeclareClass
        },
        {
                "compileMethod",
                "(Ljava/lang/reflect/Member;)Z",
                (void *) Java_com_swift_sandhook_SandHook_compileMethod
        },
        {
                "deCompileMethod",
                "(Ljava/lang/reflect/Member;Z)Z",
                (void *) Java_com_swift_sandhook_SandHook_deCompileMethod
        },
        {
                "getObjectNative",
                "(JJ)Ljava/lang/Object;",
                (void *) Java_com_swift_sandhook_SandHook_getObjectNative
        },
        {
                "canGetObject",
                "()Z",
                (void *) Java_com_swift_sandhook_SandHook_canGetObject
        },
        {
                "setHookMode",
                "(I)V",
                (void *) Java_com_swift_sandhook_SandHook_setHookMode
        },
        {
                "setInlineSafeCheck",
                "(Z)V",
                (void *) Java_com_swift_sandhook_SandHook_setInlineSafeCheck
        },
        {
                "skipAllSafeCheck",
                "(Z)V",
                (void *) Java_com_swift_sandhook_SandHook_skipAllSafeCheck
        },
        {
                "is64Bit",
                "()Z",
                (void *) Java_com_swift_sandhook_SandHook_is64Bit
        },
        {
                "disableVMInline",
                "()Z",
                (void *) Java_com_swift_sandhook_SandHook_disableVMInline
        },
        {
                "disableDex2oatInline",
                "(Z)Z",
                (void *) Java_com_swift_sandhook_SandHook_disableDex2oatInline
        },
        {
                "setNativeEntry",
                "(Ljava/lang/reflect/Member;Ljava/lang/reflect/Member;J)Z",
                (void *) Java_com_swift_sandhook_SandHook_setNativeEntry
        },
        {
                "initForPendingHook",
                "()Z",
                (void *) Java_com_swift_sandhook_SandHook_initForPendingHook
        }
};

static JNINativeMethod jniNeverCall[] = {
        {
                "neverCallNative",
                "()V",
                (void *) Java_com_swift_sandhook_ClassNeverCall_neverCallNative
        },
        {
                "neverCallNative2",
                "()V",
                (void *) Java_com_swift_sandhook_ClassNeverCall_neverCallNative2
        }
};

static bool registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *jniMethods, int methods) {
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        return false;
    }
    return env->RegisterNatives(clazz, jniMethods, methods) >= 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    const char* CLASS_SAND_HOOK = "com/swift/sandhook/SandHook";
    const char* CLASS_NEVER_CALL = "com/swift/sandhook/ClassNeverCall";

    int jniMethodSize = sizeof(JNINativeMethod);

    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    if (!registerNativeMethods(env, CLASS_SAND_HOOK, jniSandHook, sizeof(jniSandHook) / jniMethodSize)) {
        return -1;
    }

    if (!registerNativeMethods(env, CLASS_NEVER_CALL, jniNeverCall, sizeof(jniNeverCall) / jniMethodSize)) {
        return -1;
    }

    LOGW("JNI Loaded");

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT bool JNI_Load_Ex(JNIEnv* env, jclass classSandHook, jclass classNeverCall) {
    int jniMethodSize = sizeof(JNINativeMethod);

    if (env == nullptr || classSandHook == nullptr || classNeverCall == nullptr)
        return false;

    if (env->RegisterNatives(classSandHook, jniSandHook, sizeof(jniSandHook) / jniMethodSize) < 0) {
        return false;
    }

    if (env->RegisterNatives(classNeverCall, jniNeverCall, sizeof(jniNeverCall) / jniMethodSize) < 0) {
        return false;
    }

    LOGW("JNI Loaded");
    return true;
}