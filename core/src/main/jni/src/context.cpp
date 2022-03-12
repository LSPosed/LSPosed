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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#include <jni.h>
#include "config.h"
#include "utils/jni_helper.hpp"
#include "jni/resources_hook.h"
#include "context.h"
#include "native_hook.h"
#include "jni/native_api.h"
#include "service.h"
#include <sys/mman.h>
#include "symbol_cache.h"

#include <linux/fs.h>
#include <fcntl.h>

using namespace lsplant;

static_assert(FS_IOC_SETFLAGS == LP_SELECT(0x40046602, 0x40086602));

namespace lspd {
    extern int *allowUnload;

    constexpr int FIRST_ISOLATED_UID = 99000;
    constexpr int LAST_ISOLATED_UID = 99999;
    constexpr int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    constexpr int LAST_APP_ZYGOTE_ISOLATED_UID = 98999;
    constexpr int SHARED_RELRO_UID = 1037;
    constexpr int PER_USER_RANGE = 100000;

    static constexpr uid_t kAidInjected = INJECTED_AID;
    static constexpr uid_t kAidInet = 3003;

    Context::PreloadedDex::PreloadedDex(int fd, std::size_t size) {
        LOGD("Context::PreloadedDex::PreloadedDex: fd=%d, size=%zu", fd, size);
        auto *addr = mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0);

        if (addr != MAP_FAILED) {
            addr_ = addr;
            size_ = size;
        } else {
            PLOGE("Read dex");
        }
    }

    Context::PreloadedDex::~PreloadedDex() {
        if (*this) munmap(addr_, size_);
    }

    void Context::LoadDex(JNIEnv *env, int fd, size_t size) {
        LOGD("Context::LoadDex: %d", fd);
        // map fd to memory. fd should be created with ASharedMemory_create.
        auto dex = PreloadedDex(fd, size);  // for RAII...

        auto classloader = JNI_FindClass(env, "java/lang/ClassLoader");
        auto getsyscl_mid = JNI_GetStaticMethodID(
                env, classloader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
        auto sys_classloader = JNI_CallStaticObjectMethod(env, classloader, getsyscl_mid);
        if (!sys_classloader) [[unlikely]] {
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
    }

    void Context::Init() {
    }

    void Context::Init(JNIEnv *env) {
        if (auto entry_class = FindClassFromLoader(env, GetCurrentClassLoader(),
                                                   kEntryClassName)) {
            entry_class_ = JNI_NewGlobalRef(env, entry_class);
        }

        RegisterResourcesHook(env);
        RegisterNativeAPI(env);
    }

    ScopedLocalRef<jclass>
    Context::FindClassFromLoader(JNIEnv *env, jobject class_loader,
                                 std::string_view class_name) {
        if (class_loader == nullptr) return {env, nullptr};
        static auto clz = JNI_NewGlobalRef(env,
                                           JNI_FindClass(env, "dalvik/system/DexClassLoader"));
        static jmethodID mid = JNI_GetMethodID(env, clz, "loadClass",
                                               "(Ljava/lang/String;)Ljava/lang/Class;");
        if (!mid) {
            mid = JNI_GetMethodID(env, clz, "findClass",
                                  "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        if (mid) [[likely]] {
            auto target = JNI_CallObjectMethod(env, class_loader, mid,
                                               JNI_NewStringUTF(env, class_name.data()));
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
        if (!entry_class_) [[unlikely]] {
            LOGE("cannot call method %s, entry class is null", method_name.data());
            return;
        }
        jmethodID mid = JNI_GetStaticMethodID(env, entry_class_, method_name, method_sig);
        if (mid) [[likely]] {
            JNI_CallStaticVoidMethod(env, entry_class_, mid, std::forward<Args>(args)...);
        } else {
            LOGE("method %s id is null", method_name.data());
        }
    }

    void
    Context::OnNativeForkSystemServerPre(JNIEnv *env) {
        Service::instance()->InitService(env);
        skip_ = !symbol_cache->initialized.test(std::memory_order_acquire);
        if (skip_) [[unlikely]] {
            LOGW("skip system server due to symbol cache");
        }
        setAllowUnload(skip_);
    }

    void
    Context::OnNativeForkSystemServerPost(JNIEnv *env) {
        if (!skip_) {
            auto *instance = Service::instance();
            auto system_server_binder = instance->RequestSystemServerBinder(env);
            if (!system_server_binder) {
                LOGF("Failed to get system server binder, system server initialization failed. ");
                return;
            }

            auto application_binder = instance->RequestApplicationBinderFromSystemServer(env, system_server_binder);

            // Call application_binder directly if application binder is available,
            // or we proxy the request from system server binder
            auto [dex_fd, size]= instance->RequestLSPDex(env, application_binder ? application_binder : system_server_binder);
            LoadDex(env, dex_fd, size);
            close(dex_fd);
            instance->HookBridge(*this, env);

            if (application_binder) {
                InstallInlineHooks({

                });
                Init(env);
                FindAndCall(env, "forkSystemServerPost", "(Landroid/os/IBinder;)V", application_binder);
            } else {
                LOGI("skipped system server");
                GetArt(true);
            }
        }
    }

    void Context::OnNativeForkAndSpecializePre(JNIEnv *env,
                                               jint uid,
                                               jintArray &gids,
                                               jstring nice_name,
                                               jboolean is_child_zygote,
                                               jstring app_data_dir) {
        if (uid == kAidInjected) {
            int array_size = gids ? env->GetArrayLength(gids) : 0;
            auto region = std::make_unique<jint[]>(array_size + 1);
            auto *new_gids = env->NewIntArray(array_size + 1);
            if (gids) env->GetIntArrayRegion(gids, 0, array_size, region.get());
            region.get()[array_size] = kAidInet;
            env->SetIntArrayRegion(new_gids, 0, array_size + 1, region.get());
            if (gids) env->SetIntArrayRegion(gids, 0, 1, region.get() + array_size);
            gids = new_gids;
        }
        Service::instance()->InitService(env);
        const auto app_id = uid % PER_USER_RANGE;
        JUTFString process_name(env, nice_name);
        skip_ = !symbol_cache->initialized.test(std::memory_order_acquire);
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
    Context::OnNativeForkAndSpecializePost(JNIEnv *env, jstring nice_name,
                                           jstring app_data_dir) {
        const JUTFString process_name(env, nice_name);
        auto *instance = Service::instance();
        auto binder = skip_ ? ScopedLocalRef<jobject>{env, nullptr}
                            : instance->RequestBinder(env, nice_name);
        if (binder) {
            InstallInlineHooks({});
            auto [dex_fd, size] = instance->RequestLSPDex(env, binder);
            LoadDex(env, dex_fd, size);
            close(dex_fd);
            Init(env);
            LOGD("Done prepare");
            FindAndCall(env, "forkAndSpecializePost",
                        "(Ljava/lang/String;Ljava/lang/String;Landroid/os/IBinder;)V",
                        app_data_dir, nice_name,
                        binder);
            LOGD("injected xposed into %s", process_name.get());
            setAllowUnload(false);
        } else {
            auto context = Context::ReleaseInstance();
            auto service = Service::ReleaseInstance();
            GetArt(true);
            LOGD("skipped %s", process_name.get());
            setAllowUnload(true);
        }
    }

    void Context::setAllowUnload(bool unload) {
        if (allowUnload) {
            *allowUnload = unload ? 1 : 0;
        }
    }
}  // namespace lspd
