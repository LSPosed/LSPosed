
#include <jni.h>
#include <android-base/macros.h>
#include <JNIHelper.h>
#include <android-base/logging.h>
#include <jni/edxp_config_manager.h>
#include <jni/art_class_linker.h>
#include <jni/art_heap.h>
#include <jni/edxp_yahfa.h>
#include <jni/framework_zygote.h>
#include <jni/edxp_resources_hook.h>
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>
#include <art/runtime/mirror/class.h>
#include <android-base/strings.h>
#include <nativehelper/scoped_local_ref.h>
#include <jni/edxp_pending_hooks.h>
#include "edxp_context.h"
#include "config_manager.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"

namespace edxp {
    constexpr int FIRST_ISOLATED_UID = 99000;
    constexpr int LAST_ISOLATED_UID = 99999;
    constexpr int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    constexpr int LAST_APP_ZYGOTE_ISOLATED_UID = 98999;
    constexpr int SHARED_RELRO_UID = 1037;
    constexpr int PER_USER_RANGE = 100000;

    void Context::CallPostFixupStaticTrampolinesCallback(void *class_ptr, jmethodID callback_mid) {
        if (UNLIKELY(!callback_mid || !class_linker_class_)) {
            return;
        }
        if (!class_ptr) {
            return;
        }
        JNIEnv *env;
        vm_->GetEnv((void **) (&env), JNI_VERSION_1_4);
        art::JNIEnvExt env_ext(env);
        ScopedLocalRef clazz(env, env_ext.NewLocalRefer(class_ptr));
        if (clazz != nullptr) {
            JNI_CallStaticVoidMethod(env, class_linker_class_, callback_mid, clazz.get());
        }
    }

    void Context::CallOnPreFixupStaticTrampolines(void *class_ptr) {
        CallPostFixupStaticTrampolinesCallback(class_ptr, pre_fixup_static_mid_);
    }

    void Context::CallOnPostFixupStaticTrampolines(void *class_ptr) {
        CallPostFixupStaticTrampolinesCallback(class_ptr, post_fixup_static_mid_);
    }

    void Context::LoadDexAndInit(JNIEnv *env, const char *dex_path) {
        if (LIKELY(initialized_)) {
            return;
        }

        jclass classloader = JNI_FindClass(env, "java/lang/ClassLoader");
        jmethodID getsyscl_mid = JNI_GetStaticMethodID(
                env, classloader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
        jobject sys_classloader = JNI_CallStaticObjectMethod(env, classloader, getsyscl_mid);
        if (UNLIKELY(!sys_classloader)) {
            LOG(ERROR) << "getSystemClassLoader failed!!!";
            return;
        }
        jclass path_classloader = JNI_FindClass(env, "dalvik/system/PathClassLoader");
        jmethodID initMid = JNI_GetMethodID(env, path_classloader, "<init>",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
        jobject my_cl = JNI_NewObject(env, path_classloader, initMid, env->NewStringUTF(dex_path),
                                      nullptr, sys_classloader);

        env->DeleteLocalRef(classloader);
        env->DeleteLocalRef(sys_classloader);
        env->DeleteLocalRef(path_classloader);

        if (UNLIKELY(my_cl == nullptr)) {
            LOG(ERROR) << "PathClassLoader creation failed!!!";
            return;
        }

        inject_class_loader_ = env->NewGlobalRef(my_cl);

        env->DeleteLocalRef(my_cl);

        // initialize pending methods related
        env->GetJavaVM(&vm_);
        class_linker_class_ = (jclass) env->NewGlobalRef(
                FindClassFromLoader(env, kClassLinkerClassName));
        pre_fixup_static_mid_ = JNI_GetStaticMethodID(env, class_linker_class_,
                                                      "onPreFixupStaticTrampolines",
                                                      "(Ljava/lang/Class;)V");
        post_fixup_static_mid_ = JNI_GetStaticMethodID(env, class_linker_class_,
                                                       "onPostFixupStaticTrampolines",
                                                       "(Ljava/lang/Class;)V");

        entry_class_ = (jclass) (env->NewGlobalRef(
                FindClassFromLoader(env, GetCurrentClassLoader(), kEntryClassName)));

        RegisterEdxpResourcesHook(env);
        RegisterFrameworkZygote(env);
        RegisterConfigManagerMethods(env);
        RegisterArtClassLinker(env);
        RegisterArtHeap(env);
        RegisterEdxpYahfa(env);
        RegisterPendingHooks(env);

        // must call entry class's methods after all native methods registered
        if (LIKELY(entry_class_)) {
            jmethodID get_variant_mid = JNI_GetStaticMethodID(env, entry_class_,
                                                              "getEdxpVariant", "()I");
            if (LIKELY(get_variant_mid)) {
                int variant = JNI_CallStaticIntMethod(env, entry_class_, get_variant_mid);
                variant_ = static_cast<Variant>(variant);
            }
        }
//        LOGI("EdxpVariant: %d", variant_);

        initialized_ = true;

        if (variant_ == SANDHOOK) {
            //for SandHook variant
            ScopedDlHandle sandhook_handle(kLibSandHookPath.c_str());
            if (!sandhook_handle.IsValid()) {
                return;
            }
            typedef bool *(*TYPE_JNI_LOAD)(JNIEnv *, jclass, jclass);
            auto jni_load = sandhook_handle.DlSym<TYPE_JNI_LOAD>("JNI_Load_Ex");
            ScopedLocalRef sandhook_class(env, FindClassFromLoader(env, kSandHookClassName));
            ScopedLocalRef nevercall_class(env,
                                           FindClassFromLoader(env, kSandHookNeverCallClassName));
            if (sandhook_class == nullptr || nevercall_class == nullptr) { // fail-fast
                return;
            }
            if (!jni_load(env, sandhook_class.get(), nevercall_class.get())) {
                LOGE("SandHook: HookEntry class error. %d", getpid());
            }

        }
    }

    jclass
    Context::FindClassFromLoader(JNIEnv *env, jobject class_loader, const char *class_name) const {
        jclass clz = JNI_GetObjectClass(env, class_loader);
        jmethodID mid = JNI_GetMethodID(env, clz, "loadClass",
                                        "(Ljava/lang/String;)Ljava/lang/Class;");
        jclass ret = nullptr;
        if (!mid) {
            mid = JNI_GetMethodID(env, clz, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        if (LIKELY(mid)) {
            jobject target = JNI_CallObjectMethod(env, class_loader, mid,
                                                  env->NewStringUTF(class_name));
            if (target) {
                return (jclass) target;
            }
        } else {
            LOG(ERROR) << "No loadClass/findClass method found";
        }
        LOG(ERROR) << "Class " << class_name << " not found";
        return ret;
    }

    jclass Context::FindClassFromLoader(JNIEnv *env, const char *className) const {
        return FindClassFromLoader(env, GetCurrentClassLoader(), className);
    }

    inline void Context::PrepareJavaEnv(JNIEnv *env) {
        LoadDexAndInit(env, kInjectDexPath);
    }

    void Context::ReleaseJavaEnv(JNIEnv *env) {
        if (UNLIKELY(!instance_)) return;

        auto xposed_bridge_class = FindClassFromLoader(env, inject_class_loader_, kXposedBridgeClassName);
        if(LIKELY(xposed_bridge_class)){
            jmethodID clear_all_callbacks_method = JNI_GetStaticMethodID(env, xposed_bridge_class, "clearAllCallbacks",
                                                "()V");
            if(LIKELY(clear_all_callbacks_method)) {
                JNI_CallStaticVoidMethod(env, xposed_bridge_class, clear_all_callbacks_method);
            }
        }

        initialized_ = false;
        if (entry_class_) {
            env->DeleteGlobalRef(entry_class_);
            entry_class_ = nullptr;
        }
        if (class_linker_class_) {
            env->DeleteGlobalRef(class_linker_class_);
            class_linker_class_ = nullptr;
        }
        if (inject_class_loader_) {
            env->DeleteGlobalRef(inject_class_loader_);
            inject_class_loader_ = nullptr;
        }
        app_data_dir_ = nullptr;
        nice_name_ = nullptr;
        vm_ = nullptr;
        pre_fixup_static_mid_ = nullptr;
        post_fixup_static_mid_ = nullptr;

        auto systemClass    = env->FindClass("java/lang/System");
        auto systemGCMethod = env->GetStaticMethodID(systemClass, "gc", "()V");
        env->CallStaticVoidMethod(systemClass, systemGCMethod);
    }


    inline void Context::FindAndCall(JNIEnv *env, const char *method_name,
                                     const char *method_sig, ...) const {
        if (UNLIKELY(!entry_class_)) {
            LOGE("cannot call method %s, entry class is null", method_name);
            return;
        }
        jmethodID mid = JNI_GetStaticMethodID(env, entry_class_, method_name, method_sig);
        if (LIKELY(mid)) {
            va_list args;
            va_start(args, method_sig);
            env->CallStaticVoidMethodV(entry_class_, mid, args);
            va_end(args);
        } else {
            LOGE("method %s id is null", method_name);
        }
    }

    void
    Context::OnNativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t uid, gid_t gid,
                                         jintArray gids,
                                         jint runtime_flags, jobjectArray rlimits,
                                         jlong permitted_capabilities,
                                         jlong effective_capabilities) {
        app_data_dir_ = env->NewStringUTF(SYSTEM_SERVER_DATA_DIR);
        PrepareJavaEnv(env);
        // jump to java code
        FindAndCall(env, "forkSystemServerPre", "(II[II[[IJJ)V", uid, gid, gids, runtime_flags,
                    rlimits, permitted_capabilities, effective_capabilities);
    }


    int Context::OnNativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res) {
        if (res == 0) {
            PrepareJavaEnv(env);
            // only do work in child since FindAndCall would print log
            FindAndCall(env, "forkSystemServerPost", "(I)V", res);
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
        return 0;
    }

    std::tuple<bool, bool> Context::ShouldSkipInject(JNIEnv *env, jstring nice_name, jstring data_dir, jint uid,
                                   jboolean is_child_zygote) {
        const auto app_id = uid % PER_USER_RANGE;
        const JUTFString package_name(env, nice_name, "UNKNOWN");
        bool skip = false;
        bool release = true;
        if (is_child_zygote) {
            skip = true;
            release = false; // In Android R, calling XposedBridge.clearAllCallbacks cause crashes.
            LOGW("skip injecting into %s because it's a child zygote", package_name.get());
        }

        if ((app_id >= FIRST_ISOLATED_UID && app_id <= LAST_ISOLATED_UID) ||
            (app_id >= FIRST_APP_ZYGOTE_ISOLATED_UID && app_id <= LAST_APP_ZYGOTE_ISOLATED_UID) ||
            app_id == SHARED_RELRO_UID) {
            skip = true;
            release = false; // In Android R, calling XposedBridge.clearAllCallbacks cause crashes.
            LOGW("skip injecting into %s because it's isolated", package_name.get());
        }

        const JUTFString dir(env, data_dir);
        if(!dir || !ConfigManager::GetInstance()->IsAppNeedHook(dir)) {
            skip = true;
            LOGW("skip injecting xposed into %s because it's whitelisted/blacklisted",
                 package_name.get());
        }
        return {skip, release};
    }

    void Context::OnNativeForkAndSpecializePre(JNIEnv *env, jclass clazz,
                                               jint uid, jint gid,
                                               jintArray gids,
                                               jint runtime_flags,
                                               jobjectArray rlimits,
                                               jint mount_external,
                                               jstring se_info,
                                               jstring nice_name,
                                               jintArray fds_to_close,
                                               jintArray fds_to_ignore,
                                               jboolean is_child_zygote,
                                               jstring instruction_set,
                                               jstring app_data_dir) {
        std::tie(skip_, release_) = ShouldSkipInject(env, nice_name, app_data_dir, uid, is_child_zygote);
        app_data_dir_ = app_data_dir;
        nice_name_ = nice_name;
        PrepareJavaEnv(env);
        if(!skip_) {
            FindAndCall(env, "forkAndSpecializePre",
                        "(II[II[[IILjava/lang/String;Ljava/lang/String;[I[IZLjava/lang/String;Ljava/lang/String;)V",
                        uid, gid, gids, runtime_flags, rlimits,
                        mount_external, se_info, nice_name, fds_to_close, fds_to_ignore,
                        is_child_zygote, instruction_set, app_data_dir);
        }
    }

    int Context::OnNativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res) {
        if (res == 0) {
            const JUTFString package_name(env, nice_name_);
            if (!skip_) {
                PrepareJavaEnv(env);
                FindAndCall(env, "forkAndSpecializePost",
                            "(ILjava/lang/String;Ljava/lang/String;)V",
                            res, app_data_dir_, nice_name_);
                LOGD("injected xposed into %s", package_name.get());
            } else {
                if(release_)
                    ReleaseJavaEnv(env);
                LOGD("skipped %s", package_name.get());
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
        return 0;
    }

}

#pragma clang diagnostic pop