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
#include "context.h"
#include "native_util.h"
#include "jni/hook_bridge.h"
#include "jni/native_api.h"
#include "jni/resources_hook.h"
#include "jni/dex_parser.h"
#include "symbol_cache.h"

using namespace lsplant;


namespace lspd {
    std::unique_ptr<Context> Context::instance_;
    std::unique_ptr<ConfigBridge> ConfigBridge::instance_;

    Context::PreloadedDex::PreloadedDex(int fd, std::size_t size) {
        LOGD("Context::PreloadedDex::PreloadedDex: fd={}, size={}", fd, size);
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

    void Context::InitArtHooker(JNIEnv *env, const lsplant::InitInfo &initInfo) {
        if (!lsplant::Init(env, initInfo)) {
            LOGE("Failed to init lsplant");
            return;
        }
    }

    void Context::InitHooks(JNIEnv *env) {
        auto path_list = JNI_GetObjectFieldOf(env, inject_class_loader_, "pathList",
                                              "Ldalvik/system/DexPathList;");
        if (!path_list) {
            LOGE("Failed to get path list");
            return;
        }
        const auto elements = JNI_Cast<jobjectArray>(
                JNI_GetObjectFieldOf(env, path_list, "dexElements",
                                     "[Ldalvik/system/DexPathList$Element;"));
        if (!elements) {
            LOGE("Failed to get elements");
            return;
        }
        for (const auto &element: elements) {
            if (!element)
                continue;
            auto java_dex_file = JNI_GetObjectFieldOf(env, element, "dexFile",
                                                      "Ldalvik/system/DexFile;");
            if (!java_dex_file) {
                LOGE("Failed to get java dex file");
                return;
            }
            auto cookie = JNI_GetObjectFieldOf(env, java_dex_file, "mCookie", "Ljava/lang/Object;");
            if (!cookie) {
                LOGE("Failed to get cookie");
                return;
            }
            lsplant::MakeDexFileTrusted(env, cookie);
        }
        RegisterResourcesHook(env);
        RegisterHookBridge(env);
        RegisterNativeAPI(env);
        RegisterDexParserBridge(env);
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
        LOGE("Class {} not found", class_name);
        return {env, nullptr};
    }
}  // namespace lspd
