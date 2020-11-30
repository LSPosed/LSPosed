#include <jni.h>
#include <android-base/macros.h>
#include <JNIHelper.h>
#include <android-base/logging.h>
#include <jni/edxp_config_manager.h>
#include <jni/art_class_linker.h>
#include <jni/art_heap.h>
#include <jni/edxp_yahfa.h>
#include <jni/edxp_resources_hook.h>
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>
#include <art/runtime/mirror/class.h>
#include <android-base/strings.h>
#include <nativehelper/scoped_local_ref.h>
#include <jni/edxp_pending_hooks.h>
#include <fstream>
#include <sstream>
#include "edxp_context.h"
#include "config_manager.h"
#include "art/runtime/runtime.h"
#include "art/runtime/gc/heap.h"
#include "native_hook.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-value"

namespace edxp {
    namespace fs = std::filesystem;

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

    void Context::PreLoadDex(JNIEnv *env, const std::string &dex_path) {
        if (LIKELY(!dexes.empty())) return;
        std::vector<std::string> paths;
        {
            std::istringstream is(dex_path);
            std::string path;
            while (std::getline(is, path, ':')) {
                paths.emplace_back(std::move(path));
            }
        }
        for (const auto &path: paths) {
            std::ifstream is(path, std::ios::binary);
            if (!is.good()) {
                LOGE("Cannot load path %s", path.c_str());
            }
            dexes.emplace_back(std::istreambuf_iterator<char>(is),
                               std::istreambuf_iterator<char>());
            LOGD("Loaded %s with size %zu", path.c_str(), dexes.back().size());
        }
    }

    void Context::InjectDexAndInit(JNIEnv *env) {
        if (LIKELY(initialized_)) {
            return;
        }

        jclass classloader = JNI_FindClass(env, "java/lang/ClassLoader");
        jmethodID getsyscl_mid = JNI_GetStaticMethodID(
                env, classloader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
        jobject sys_classloader = JNI_CallStaticObjectMethod(env, classloader, getsyscl_mid);
        if (UNLIKELY(!sys_classloader)) {
            LOGE("getSystemClassLoader failed!!!");
            return;
        }
        jclass in_memory_classloader = JNI_FindClass(env, "dalvik/system/InMemoryDexClassLoader");
        jmethodID initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                            "([Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        jclass byte_buffer_class = JNI_FindClass(env, "java/nio/ByteBuffer");
        jmethodID byte_buffer_wrap = JNI_GetStaticMethodID(env, byte_buffer_class, "wrap",
                                                           "([B)Ljava/nio/ByteBuffer;");
        auto buffer_array = env->NewObjectArray(dexes.size(), byte_buffer_class, nullptr);
        for (size_t i = 0; i != dexes.size(); ++i) {
            const auto dex = dexes.at(i);
            auto byte_array = env->NewByteArray(dex.size());
            env->SetByteArrayRegion(byte_array, 0, dex.size(),
                                    dex.data());
            auto buffer = JNI_CallStaticObjectMethod(env, byte_buffer_class, byte_buffer_wrap,
                                                     byte_array);
            env->SetObjectArrayElement(buffer_array, i, buffer);
        }
        jobject my_cl = env->NewObject(in_memory_classloader, initMid,
                                       buffer_array, sys_classloader);
        env->DeleteLocalRef(classloader);
        env->DeleteLocalRef(sys_classloader);
        env->DeleteLocalRef(in_memory_classloader);
        env->DeleteLocalRef(byte_buffer_class);

        if (UNLIKELY(my_cl == nullptr)) {
            LOGE("InMemoryDexClassLoader creation failed!!!");
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
    Context::FindClassFromLoader(JNIEnv *env, jobject class_loader, const char *class_name) {
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
            LOGE("No loadClass/findClass method found");
        }
        LOGE("Class %s not found", class_name);
        return ret;
    }


    inline void Context::PrepareJavaEnv(JNIEnv *env) {
        InjectDexAndInit(env);
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
    Context::OnNativeForkSystemServerPre(JNIEnv *env, [[maybe_unused]] jclass clazz, uid_t uid,
                                         gid_t gid,
                                         jintArray gids,
                                         jint runtime_flags, jobjectArray rlimits,
                                         jlong permitted_capabilities,
                                         jlong effective_capabilities) {
        ConfigManager::SetCurrentUser(0u);
        app_modules_list_ = ConfigManager::GetInstance()->GetAppModuleList(
                "android"); // I don't think we need this, but anyway
        skip_ = false;
        if (!ConfigManager::GetInstance()->IsAppNeedHook("android")) {
            skip_ = true;
            LOGD("skip injecting xposed into android because it's whitelisted/blacklisted");
        }
        if (!skip_ && app_modules_list_.empty()) {
            skip_ = true;
            LOGD("skip injecting into android because no module hooks it");
        }
        PreLoadDex(env, kInjectDexPath);
    }


    int
    Context::OnNativeForkSystemServerPost(JNIEnv *env, [[maybe_unused]] jclass clazz, jint res) {
        if (res == 0) {
            if (!skip_) {
                if (void *buf = mmap(nullptr, 1, PROT_READ | PROT_WRITE | PROT_EXEC,
                                     MAP_ANONYMOUS | MAP_PRIVATE, -1,
                                     0);
                        buf == MAP_FAILED) {
                    skip_ = true;
                    LOGE("skip injecting into android because sepolicy was not loaded properly");
                } else {
                    munmap(buf, 1);
                }
            }
            if (!skip_) {
                InstallInlineHooks();
                PrepareJavaEnv(env);
                // only do work in child since FindAndCall would print log
                FindAndCall(env, "forkSystemServerPost", "(I)V", res);
            } else {
                auto config_managers = ConfigManager::ReleaseInstances();
                auto context = Context::ReleaseInstance();
                LOGD("skipped android");
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
        return 0;
    }

    std::tuple<bool, uid_t, std::string>
    Context::GetAppInfoFromDir(JNIEnv *env, jstring dir) {
        uid_t uid = 0;
        JUTFString app_data_dir(env, dir);
        if (!app_data_dir) return {false, 0, {}};
        fs::path path(app_data_dir.get());
        std::vector<std::string> splits(path.begin(), path.end());
        if (splits.size() < 5u) {
            LOGE("can't parse %s", path.c_str());
            return {false, uid, {}};
        }
        const auto &uid_str = splits[3];
        const auto &package_name = splits[4];
        try {
            uid = stol(uid_str);
        } catch (const std::invalid_argument &ignored) {
            LOGE("can't parse %s", app_data_dir.get());
            return {false, uid, {}};
        }
        return {true, uid, package_name};
    }

    bool Context::ShouldSkipInject(const std::string &package_name, uid_t user, uid_t uid,
                                   bool info_res, const std::vector<std::string> &app_modules_list_,
                                   bool is_child_zygote) {
        const auto app_id = uid % PER_USER_RANGE;
        bool skip = false;
        if (!info_res) {
            LOGW("skip injecting into %s because it has no data dir", package_name.c_str());
            skip = true;
        }
        if (!skip && is_child_zygote) {
            skip = true;
            LOGW("skip injecting into %s because it's a child zygote", package_name.c_str());
        }

        if (!skip && ((app_id >= FIRST_ISOLATED_UID && app_id <= LAST_ISOLATED_UID) ||
                      (app_id >= FIRST_APP_ZYGOTE_ISOLATED_UID &&
                       app_id <= LAST_APP_ZYGOTE_ISOLATED_UID) ||
                      app_id == SHARED_RELRO_UID)) {
            skip = true;
            LOGW("skip injecting into %s because it's isolated", package_name.c_str());
        }

        if (!skip && !ConfigManager::GetInstance()->IsAppNeedHook(package_name)) {
            skip = true;
            LOGW("skip injecting xposed into %s because it's whitelisted/blacklisted",
                 package_name.c_str());
        }

        if (!skip && app_modules_list_.empty() &&
            package_name != ConfigManager::GetInstance()->GetInstallerPackageName()) {
            skip = true;
            LOGD("skip injecting xposed into %s because no module hooks it",
                 package_name.c_str());
        }
        return skip;
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
        const auto&[res, user, package_name] = GetAppInfoFromDir(env, app_data_dir);
        ConfigManager::SetCurrentUser(user);
        app_modules_list_ = ConfigManager::GetInstance()->GetAppModuleList(package_name);
        skip_ = ShouldSkipInject(package_name, user, uid, res, app_modules_list_, is_child_zygote);
        ConfigManager::GetInstance()->EnsurePermission(package_name, uid % PER_USER_RANGE);
        app_data_dir_ = app_data_dir;
        nice_name_ = nice_name;
        PreLoadDex(env, kInjectDexPath);
    }

    int
    Context::OnNativeForkAndSpecializePost(JNIEnv *env, [[maybe_unused]]jclass clazz, jint res) {
        if (res == 0) {
            const JUTFString process_name(env, nice_name_);
            if (!skip_) {
                InstallInlineHooks();
                PrepareJavaEnv(env);
                LOGD("Done prepare");
                FindAndCall(env, "forkAndSpecializePost",
                            "(ILjava/lang/String;Ljava/lang/String;)V",
                            res, app_data_dir_, nice_name_);
                LOGD("injected xposed into %s", process_name.get());
            } else {
                [[maybe_unused]] auto config_manager = ConfigManager::ReleaseInstances();
                auto context = Context::ReleaseInstance();
                LOGD("skipped %s", process_name.get());
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
        return 0;
    }

}

#pragma clang diagnostic pop