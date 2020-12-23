//
// Created by swift on 2019/1/21.
//
#include "../includes/hide_api.h"
#include "../includes/arch.h"
#include "../includes/elf_util.h"
#include "../includes/log.h"
#include "../includes/utils.h"
#include "../includes/trampoline_manager.h"

extern int SDK_INT;

extern "C" {


    void* jitCompilerHandle = nullptr;
    bool (*jitCompileMethod)(void*, void*, void*, bool) = nullptr;
    bool (*jitCompileMethodQ)(void*, void*, void*, bool, bool) = nullptr;

    void (*innerSuspendVM)() = nullptr;
    void (*innerResumeVM)() = nullptr;

    jobject (*addWeakGlobalRef)(JavaVM *, void *, void *) = nullptr;

    art::jit::JitCompiler** globalJitCompileHandlerAddr = nullptr;

    //for Android Q
    void (**origin_jit_update_options)(void *) = nullptr;

    void (*profileSaver_ForceProcessProfiles)() = nullptr;

    jfieldID fieldArtMethod = nullptr;

// paths
    const char* art_lib_path;
    const char* jit_lib_path;

    JavaVM* jvm;

    void *(*hook_native)(void* origin, void *replace) = nullptr;

    void (*class_init_callback)(void*) = nullptr;

    void (*backup_fixup_static_trampolines)(void *, void *) = nullptr;

    void initHideApi(JNIEnv* env) {

        env->GetJavaVM(&jvm);

        if (BYTE_POINT == 8) {
            if (SDK_INT >= ANDROID_Q) {
                art_lib_path = "/lib64/libart.so";
                jit_lib_path = "/lib64/libart-compiler.so";
            } else {
                art_lib_path = "/system/lib64/libart.so";
                jit_lib_path = "/system/lib64/libart-compiler.so";
            }
        } else {
            if (SDK_INT >= ANDROID_Q) {
                art_lib_path = "/lib/libart.so";
                jit_lib_path = "/lib/libart-compiler.so";
             } else {
                art_lib_path = "/system/lib/libart.so";
                jit_lib_path = "/system/lib/libart-compiler.so";
            }
        }

        //init compile
        if (SDK_INT >= ANDROID_N) {
            globalJitCompileHandlerAddr = reinterpret_cast<art::jit::JitCompiler **>(getSymCompat(art_lib_path, "_ZN3art3jit3Jit20jit_compiler_handle_E"));
            if (SDK_INT >= ANDROID_Q) {
                jitCompileMethodQ = reinterpret_cast<bool (*)(void *, void *, void *, bool,
                                                         bool)>(getSymCompat(jit_lib_path, "jit_compile_method"));
            } else {
                jitCompileMethod = reinterpret_cast<bool (*)(void *, void *, void *,
                                                             bool)>(getSymCompat(jit_lib_path,
                                                                                 "jit_compile_method"));
            }
            auto jit_load = getSymCompat(jit_lib_path, "jit_load");
            if (jit_load) {
                if (SDK_INT >= ANDROID_Q) {
                    // Android 10：void* jit_load()
                    // Android 11: JitCompilerInterface* jit_load()
                    jitCompilerHandle = reinterpret_cast<void*(*)()>(jit_load)();
                } else {
                    // void* jit_load(bool* generate_debug_info)
                    bool generate_debug_info = false;
                    jitCompilerHandle = reinterpret_cast<void*(*)(void*)>(jit_load)(&generate_debug_info);
                }
            } else {
                jitCompilerHandle = getGlobalJitCompiler();
            }

            if (jitCompilerHandle != nullptr) {
                art::CompilerOptions* compilerOptions = getCompilerOptions(
                        reinterpret_cast<art::jit::JitCompiler *>(jitCompilerHandle));
                disableJitInline(compilerOptions);
            }

        }


        //init suspend
        innerSuspendVM = reinterpret_cast<void (*)()>(getSymCompat(art_lib_path,
                                                                         "_ZN3art3Dbg9SuspendVMEv"));
        innerResumeVM = reinterpret_cast<void (*)()>(getSymCompat(art_lib_path,
                                                                        "_ZN3art3Dbg8ResumeVMEv"));


        //init for getObject & JitCompiler
        const char* add_weak_ref_sym;
        if (SDK_INT < ANDROID_M) {
            add_weak_ref_sym = "_ZN3art9JavaVMExt22AddWeakGlobalReferenceEPNS_6ThreadEPNS_6mirror6ObjectE";
        } else if (SDK_INT < ANDROID_N) {
            add_weak_ref_sym = "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadEPNS_6mirror6ObjectE";
        } else  {
            add_weak_ref_sym = SDK_INT <= ANDROID_N2
                                           ? "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadEPNS_6mirror6ObjectE"
                                           : "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE";
        }

        addWeakGlobalRef = reinterpret_cast<jobject (*)(JavaVM *, void *,
                                                   void *)>(getSymCompat(art_lib_path, add_weak_ref_sym));

        if (SDK_INT >= ANDROID_Q) {
            origin_jit_update_options = reinterpret_cast<void (**)(void *)>(getSymCompat(art_lib_path, "_ZN3art3jit3Jit20jit_update_options_E"));
        }

        if (SDK_INT > ANDROID_N) {
            profileSaver_ForceProcessProfiles = reinterpret_cast<void (*)()>(getSymCompat(art_lib_path, "_ZN3art12ProfileSaver20ForceProcessProfilesEv"));
        }

        if (SDK_INT >=ANDROID_R) {
            auto classExecutable = env->FindClass("java/lang/reflect/Executable");
            fieldArtMethod = env->GetFieldID(classExecutable, "artMethod", "J");
        }

    }

    bool canCompile() {
        if (SDK_INT >= ANDROID_R)
            return false;
        if (getGlobalJitCompiler() == nullptr) {
            LOGE("JIT not init!");
            return false;
        }
        JNIEnv *env;
        jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        return getBooleanFromJava(env, "com/swift/sandhook/SandHookConfig",
                                  "compiler");
    }

    bool compileMethod(void* artMethod, void* thread) {
        if (jitCompilerHandle == nullptr)
            return false;
        if (!canCompile()) return false;

        //backup thread flag and state because of jit compile function will modify thread state
        uint32_t old_flag_and_state = *((uint32_t *) thread);
        bool ret;
        if (SDK_INT >= ANDROID_Q) {
            if (jitCompileMethodQ == nullptr) {
                return false;
            }
            ret = jitCompileMethodQ(jitCompilerHandle, artMethod, thread, false, false);
        } else {
            if (jitCompileMethod == nullptr) {
                return false;
            }
            ret= jitCompileMethod(jitCompilerHandle, artMethod, thread, false);
        }
        memcpy(thread, &old_flag_and_state, 4);
        return ret;
    }

    void suspendVM() {
        if (innerSuspendVM == nullptr || innerResumeVM == nullptr)
            return;
        innerSuspendVM();
    }

    void resumeVM() {
        if (innerSuspendVM == nullptr || innerResumeVM == nullptr)
            return;
        innerResumeVM();
    }

    bool canGetObject() {
        return addWeakGlobalRef != nullptr;
    }

    void *getCurrentThread() {
        return __get_tls()[TLS_SLOT_ART_THREAD];
    }

    jobject getJavaObject(JNIEnv* env, void* thread, void* address) {
        if (addWeakGlobalRef == nullptr)
            return nullptr;

        jobject object = addWeakGlobalRef(jvm, thread, address);
        if (object == nullptr)
            return nullptr;

        jobject result = env->NewLocalRef(object);
        env->DeleteWeakGlobalRef(object);

        return result;
    }

    art::jit::JitCompiler* getGlobalJitCompiler() {
        if (SDK_INT < ANDROID_N)
            return nullptr;
        if (globalJitCompileHandlerAddr == nullptr)
            return nullptr;
        return *globalJitCompileHandlerAddr;
    }

    art::CompilerOptions* getCompilerOptions(art::jit::JitCompiler* compiler) {
        if (compiler == nullptr)
            return nullptr;
        return compiler->compilerOptions.get();
    }

    art::CompilerOptions* getGlobalCompilerOptions() {
        return getCompilerOptions(getGlobalJitCompiler());
    }

    bool disableJitInline(art::CompilerOptions* compilerOptions) {
        if (compilerOptions == nullptr)
            return false;
        size_t originOptions = compilerOptions->getInlineMaxCodeUnits();
        //maybe a real inlineMaxCodeUnits
        if (originOptions > 0 && originOptions <= 1024) {
            compilerOptions->setInlineMaxCodeUnits(0);
            return true;
        } else {
            return false;
        }
    }

    void* getInterpreterBridge(bool isNative) {
        SandHook::ElfImg libart(art_lib_path);
        if (isNative) {
            return reinterpret_cast<void *>(libart.getSymbAddress("art_quick_generic_jni_trampoline"));
        } else {
            return reinterpret_cast<void *>(libart.getSymbAddress("art_quick_to_interpreter_bridge"));
        }
    }

    //to replace jit_update_option
    void fake_jit_update_options(void* handle) {
        //do nothing
        LOGW("android q: art request update compiler options");
   }

    bool replaceUpdateCompilerOptionsQ() {
        if (SDK_INT < ANDROID_Q)
            return false;
        if (origin_jit_update_options == nullptr
            || *origin_jit_update_options == nullptr)
            return false;
        *origin_jit_update_options = fake_jit_update_options;
        return true;
    }

    bool forceProcessProfiles() {
        if (profileSaver_ForceProcessProfiles == nullptr)
            return false;
        profileSaver_ForceProcessProfiles();
        return true;
    }

    void replaceFixupStaticTrampolines(void *thiz, void *clazz_ptr) {
        backup_fixup_static_trampolines(thiz, clazz_ptr);
        if (class_init_callback) {
            class_init_callback(clazz_ptr);
        }
    }

    bool hookClassInit(void(*callback)(void*)) {
        void* symFixupStaticTrampolines = getSymCompat(art_lib_path, "_ZN3art11ClassLinker22FixupStaticTrampolinesENS_6ObjPtrINS_6mirror5ClassEEE");

        if (symFixupStaticTrampolines == nullptr) {
            //huawei lon-al00 android 7.0 api level 24
            symFixupStaticTrampolines = getSymCompat(art_lib_path,
                                                     "_ZN3art11ClassLinker22FixupStaticTrampolinesEPNS_6mirror5ClassE");
        }
        if (symFixupStaticTrampolines == nullptr || hook_native == nullptr)
            return false;
        backup_fixup_static_trampolines = reinterpret_cast<void (*)(void *, void *)>(hook_native(
                symFixupStaticTrampolines, (void *) replaceFixupStaticTrampolines));
        if (backup_fixup_static_trampolines) {
            class_init_callback = callback;
            return true;
        } else {
            return false;
        }
    }

    JNIEnv *getEnv() {
        JNIEnv *env;
        jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        return env;
    }

    JNIEnv *attachAndGetEvn() {
        JNIEnv *env = getEnv();
        if (env == nullptr) {
            jvm->AttachCurrentThread(&env, nullptr);
        }
        return env;
    }

    static bool isIndexId(jmethodID mid) {
        return (reinterpret_cast<uintptr_t>(mid) % 2) != 0;
    }

    ArtMethod* getArtMethod(JNIEnv *env, jobject method) {
        if (SDK_INT >= ANDROID_R) {
            return reinterpret_cast<ArtMethod *>(env->GetLongField(method, fieldArtMethod));
        } else {
            jmethodID methodId = env->FromReflectedMethod(method);
            return reinterpret_cast<ArtMethod *>(methodId);
        }
    }

}

