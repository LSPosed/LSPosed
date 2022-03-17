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
#include "native_hook.h"
#include "native_util.h"
#include "jni/hook_bridge.h"
#include "jni/native_api.h"
#include "jni/resources_hook.h"
#include "symbol_cache.h"

using namespace lsplant;

namespace lspd {
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

    void Context::InitHooks(JNIEnv *env, const lsplant::InitInfo& initInfo) {
        if (!lsplant::Init(env, initInfo)) {
            return;
        }
        if (auto entry_class = FindClassFromLoader(env, GetCurrentClassLoader(),
                                                   kEntryClassName)) {
            entry_class_ = JNI_NewGlobalRef(env, entry_class);
        }

        RegisterResourcesHook(env);
        RegisterHookBridge(env);
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
}  // namespace lspd
