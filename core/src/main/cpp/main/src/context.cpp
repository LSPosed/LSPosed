/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#include <jni.h>
#include "jni_helper.h"
#include "jni/art_class_linker.h"
#include "jni/yahfa.h"
#include "jni/resources_hook.h"
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>
#include "jni/pending_hooks.h"
#include "context.h"
#include "native_hook.h"
#include "jni/native_api.h"
#include "service.h"
#include "symbol_cache.h"

namespace lspd {
    extern int *allowUnload;

    constexpr int FIRST_ISOLATED_UID = 99000;
    constexpr int LAST_ISOLATED_UID = 99999;
    constexpr int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    constexpr int LAST_APP_ZYGOTE_ISOLATED_UID = 98999;
    constexpr int SHARED_RELRO_UID = 1037;
    constexpr int PER_USER_RANGE = 100000;

    void Context::CallOnPostFixupStaticTrampolines(void *class_ptr) {
        if (UNLIKELY(!class_ptr || !class_linker_class_ || !post_fixup_static_mid_)) {
            return;
        }

        JNIEnv *env;
        vm_->GetEnv((void **) (&env), JNI_VERSION_1_4);
        art::JNIEnvExt env_ext(env);
        ScopedLocalRef clazz(env, env_ext.NewLocalRefer(class_ptr));
        if (clazz) {
            JNI_CallStaticVoidMethod(env, class_linker_class_, post_fixup_static_mid_, clazz.get());
        }
    }

    void Context::PreLoadDex(std::string_view dex_path) {
        if (LIKELY(!dex.empty())) return;

        FILE *f = fopen(dex_path.data(), "rb");
        if (!f) {
            LOGE("Fail to open dex from %s", dex_path.data());
            return;
        }
        fseek(f, 0, SEEK_END);
        dex.resize(ftell(f));
        rewind(f);
        if (dex.size() != fread(dex.data(), sizeof(decltype(dex)::value_type), dex.size(), f)) {
            LOGE("Read dex failed");
            dex.resize(0);
        }
        fclose(f);

        LOGI("Loaded %s with size %zu", dex_path.data(), dex.size());
    }

    void Context::LoadDex(JNIEnv *env) {
        auto classloader = JNI_FindClass(env, "java/lang/ClassLoader");
        auto getsyscl_mid = JNI_GetStaticMethodID(
                env, classloader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
        auto sys_classloader = JNI_CallStaticObjectMethod(env, classloader, getsyscl_mid);
        if (UNLIKELY(!sys_classloader)) {
            LOGE("getSystemClassLoader failed!!!");
            return;
        }
        auto in_memory_classloader = JNI_FindClass(env, "dalvik/system/InMemoryDexClassLoader");
        auto initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                       "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        auto byte_buffer_class = JNI_FindClass(env, "java/nio/ByteBuffer");
        auto dex_buffer = env->NewDirectByteBuffer(dex.data(), dex.size());
        if (auto my_cl = JNI_NewObject(env, in_memory_classloader, initMid,
                                       dex_buffer, sys_classloader)) {
            inject_class_loader_ = JNI_NewGlobalRef(env, my_cl);
        } else {
            LOGE("InMemoryDexClassLoader creation failed!!!");
            return;
        }

        env->DeleteLocalRef(dex_buffer);

        env->GetJavaVM(&vm_);
    }

    void Context::Init(JNIEnv *env) {
        if (auto class_linker_class = FindClassFromCurrentLoader(env, kClassLinkerClassName)) {
            class_linker_class_ = JNI_NewGlobalRef(env, class_linker_class);
        }
        post_fixup_static_mid_ = JNI_GetStaticMethodID(env, class_linker_class_,
                                                       "onPostFixupStaticTrampolines",
                                                       "(Ljava/lang/Class;)V");

        if (auto entry_class = FindClassFromLoader(env, GetCurrentClassLoader(), kEntryClassName)) {
            entry_class_ = JNI_NewGlobalRef(env, entry_class);
        }

        RegisterResourcesHook(env);
        RegisterArtClassLinker(env);
        RegisterYahfa(env);
        RegisterPendingHooks(env);
        RegisterNativeAPI(env);
    }

    ScopedLocalRef<jclass>
    Context::FindClassFromLoader(JNIEnv *env, jobject class_loader, std::string_view class_name) {
        if (class_loader == nullptr) return {env, nullptr};
        static auto clz = JNI_NewGlobalRef(env, JNI_FindClass(env, "dalvik/system/DexClassLoader"));
        static jmethodID mid = JNI_GetMethodID(env, clz, "loadClass",
                                               "(Ljava/lang/String;)Ljava/lang/Class;");
        if (!mid) {
            mid = JNI_GetMethodID(env, clz, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        if (LIKELY(mid)) {
            auto target = JNI_CallObjectMethod(env, class_loader, mid,
                                               env->NewStringUTF(class_name.data()));
            if (target) {
                return target;
            }
        } else {
            LOGE("No loadClass/findClass method found");
        }
        LOGE("Class %s not found", class_name.data());
        return {env, nullptr};
    }

    template<typename ...Args>
    void
    Context::FindAndCall(JNIEnv *env, std::string_view method_name, std::string_view method_sig,
                         Args &&... args) const {
        if (UNLIKELY(!entry_class_)) {
            LOGE("cannot call method %s, entry class is null", method_name.data());
            return;
        }
        jmethodID mid = JNI_GetStaticMethodID(env, entry_class_, method_name, method_sig);
        if (LIKELY(mid)) {
            JNI_CallStaticVoidMethod(env, entry_class_, mid, std::forward<Args>(args)...);
        } else {
            LOGE("method %s id is null", method_name.data());
        }
    }

    void
    Context::OnNativeForkSystemServerPre(JNIEnv *env) {
        Service::instance()->InitService(env);
        skip_ = !sym_initialized;
        setAllowUnload(skip_);
    }

    void
    Context::OnNativeForkSystemServerPost(JNIEnv *env, jint res) {
        if (res != 0) return;
        if (!skip_) {
            LoadDex(env);
            Service::instance()->HookBridge(*this, env);
            auto binder = Service::instance()->RequestBinderForSystemServer(env);
            if (binder) {
                InstallInlineHooks();
                Init(env);
                FindAndCall(env, "forkSystemServerPost", "(Landroid/os/IBinder;)V", binder);
            } else skip_ = true;
        }
        setAllowUnload(skip_);
    }

    void Context::OnNativeForkAndSpecializePre(JNIEnv *env,
                                               jint uid,
                                               jstring nice_name,
                                               jboolean is_child_zygote,
                                               jstring app_data_dir) {
        Service::instance()->InitService(env);
        const auto app_id = uid % PER_USER_RANGE;
        nice_name_ = nice_name;
        JUTFString process_name(env, nice_name);
        skip_ = !sym_initialized;
        if (!skip_ && !app_data_dir) {
            LOGD("skip injecting into %s because it has no data dir", process_name.get());
            skip_ = true;
        }
        if (!skip_ && is_child_zygote) {
            skip_ = true;
            LOGD("skip injecting into %s because it's a child zygote", process_name.get());
        }

        if (!skip_ && ((app_id >= FIRST_ISOLATED_UID && app_id <= LAST_ISOLATED_UID) ||
                       (app_id >= FIRST_APP_ZYGOTE_ISOLATED_UID &&
                        app_id <= LAST_APP_ZYGOTE_ISOLATED_UID) ||
                       app_id == SHARED_RELRO_UID)) {
            skip_ = true;
            LOGI("skip injecting into %s because it's isolated", process_name.get());
        }
        setAllowUnload(skip_);
    }

    void
    Context::OnNativeForkAndSpecializePost(JNIEnv *env) {
        const JUTFString process_name(env, nice_name_);
        auto binder = skip_ ? ScopedLocalRef<jobject>{env, nullptr}
                            : Service::instance()->RequestBinder(env, nice_name_);
        if (binder) {
            InstallInlineHooks();
            LoadDex(env);
            Init(env);
            LOGD("Done prepare");
            FindAndCall(env, "forkAndSpecializePost",
                        "(Ljava/lang/String;Ljava/lang/String;Landroid/os/IBinder;)V",
                        app_data_dir_, nice_name_,
                        binder);
            LOGD("injected xposed into %s", process_name.get());
            setAllowUnload(false);
        } else {
            auto context = Context::ReleaseInstance();
            auto service = Service::ReleaseInstance();
            LOGD("skipped %s", process_name.get());
            setAllowUnload(true);
        }
    }

    void Context::setAllowUnload(bool unload) {
        if (allowUnload) {
            *allowUnload = unload ? 1 : 0;
        }
    }
}
