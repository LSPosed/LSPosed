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
 * Copyright (C) 2022 LSPosed Contributors
 */

#include "hook_bridge.h"
#include "native_util.h"
#include "lsplant.hpp"
#include <absl/container/flat_hash_map.h>
#include <memory>
#include <shared_mutex>
#include <set>

using namespace lsplant;

namespace {

struct HookItem {
    jobject backup {nullptr};
    std::multimap<jint, jobject, std::greater<>> callbacks {};
};

std::shared_mutex hooked_lock;
absl::flat_hash_map<jmethodID, std::unique_ptr<HookItem>> hooked_methods;

jmethodID invoke = nullptr;
}

namespace lspd {
LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, hookMethod, jobject hookMethod,
                      jclass hooker, jint priority, jobject callback) {
    bool newHook = false;
#ifndef NDEBUG
    struct finally {
        std::chrono::steady_clock::time_point start = std::chrono::steady_clock::now();
        bool &newHook;
        ~finally() {
            auto finish = std::chrono::steady_clock::now();
            if (newHook) {
                LOGV("New hook took {}us",
                     std::chrono::duration_cast<std::chrono::microseconds>(finish - start).count());
            }
        }
    } finally {
        .newHook = newHook
    };
#endif
    auto target = env->FromReflectedMethod(hookMethod);
    HookItem * hook_item = nullptr;
    {
        std::shared_lock lk(hooked_lock);
        if (auto found = hooked_methods.find(target); found != hooked_methods.end()) {
            hook_item = found->second.get();
        }
    }
    if (!hook_item) {
        std::unique_lock lk(hooked_lock);
        if (auto &ptr = hooked_methods[target]; !ptr) {
            ptr = std::make_unique<HookItem>();
            hook_item = ptr.get();
            newHook = true;
        } else {
            hook_item = ptr.get();
        }
    }
    if (newHook) {
        auto init = env->GetMethodID(hooker, "<init>", "(Ljava/lang/reflect/Executable;)V");
        auto callback_method = env->ToReflectedMethod(hooker, env->GetMethodID(hooker, "callback",
                                                                               "([Ljava/lang/Object;)Ljava/lang/Object;"),
                                                      false);
        auto hooker_object = env->NewObject(hooker, init, hookMethod);
        hook_item->backup = lsplant::Hook(env, hookMethod, hooker_object, callback_method);
        env->DeleteLocalRef(hooker_object);
    }
    JNIMonitor monitor(env, hook_item->backup);
    hook_item->callbacks.emplace(std::make_pair(priority, env->NewGlobalRef(callback)));
    return hook_item->backup ? JNI_TRUE : JNI_FALSE;
}

LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, unhookMethod, jobject hookMethod, jobject callback) {
    auto target = env->FromReflectedMethod(hookMethod);
    HookItem * hook_item = nullptr;
    {
        std::shared_lock lk(hooked_lock);
        if (auto found = hooked_methods.find(target); found != hooked_methods.end()) {
            hook_item = found->second.get();
        }
    }
    if (!hook_item) return JNI_FALSE;
    auto backup = hook_item->backup;
    if (!backup) return JNI_FALSE;
    JNIMonitor monitor(env, backup);
    for (auto i = hook_item->callbacks.begin(); i != hook_item->callbacks.end(); ++i) {
        if (env->IsSameObject(i->second, callback)) {
            hook_item->callbacks.erase(i);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, deoptimizeMethod, jobject hookMethod,
                      jclass hooker, jint priority, jobject callback) {
    return lsplant::Deoptimize(env, hookMethod);
}

LSP_DEF_NATIVE_METHOD(jobject, HookBridge, invokeOriginalMethod, jobject hookMethod,
                      jobject thiz, jobjectArray args) {
    auto target = env->FromReflectedMethod(hookMethod);
    HookItem * hook_item = nullptr;
    {
        std::shared_lock lk(hooked_lock);
        if (auto found = hooked_methods.find(target); found != hooked_methods.end()) {
            hook_item = found->second.get();
        }
    }
    jobject to_call = hookMethod;
    if (hook_item && hook_item->backup) {
        to_call = hook_item->backup;
    }
    return env->CallObjectMethod(to_call, invoke, thiz, args);
}

LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, instanceOf, jobject object, jclass expected_class) {
    return env->IsInstanceOf(object, expected_class);
}

LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, setTrusted, jobject cookie) {
    return lsplant::MakeDexFileTrusted(env, cookie);
}

LSP_DEF_NATIVE_METHOD(jobjectArray, HookBridge, callbackSnapshot, jobject method) {
    auto target = env->FromReflectedMethod(method);
    HookItem *hook_item = nullptr;
    {
        std::shared_lock lk(hooked_lock);
        if (auto found = hooked_methods.find(target); found != hooked_methods.end()) {
            hook_item = found->second.get();
        }
    }
    if (!hook_item) return nullptr;
    auto backup = hook_item->backup;
    if (!backup) return nullptr;
    JNIMonitor monitor(env, backup);
    auto res = env->NewObjectArray((jsize) hook_item->callbacks.size(), env->FindClass("java/lang/Object"), nullptr);
    for (jsize i = 0; auto callback: hook_item->callbacks) {
        env->SetObjectArrayElement(res, i++, env->NewLocalRef(callback.second));
    }
    return res;
}

static JNINativeMethod gMethods[] = {
    LSP_NATIVE_METHOD(HookBridge, hookMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Class;ILjava/lang/Object;)Z"),
    LSP_NATIVE_METHOD(HookBridge, unhookMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Object;)Z"),
    LSP_NATIVE_METHOD(HookBridge, deoptimizeMethod, "(Ljava/lang/reflect/Executable;)Z"),
    LSP_NATIVE_METHOD(HookBridge, invokeOriginalMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
    LSP_NATIVE_METHOD(HookBridge, instanceOf, "(Ljava/lang/Object;Ljava/lang/Class;)Z"),
    LSP_NATIVE_METHOD(HookBridge, setTrusted, "(Ljava/lang/Object;)Z"),
    LSP_NATIVE_METHOD(HookBridge, callbackSnapshot, "(Ljava/lang/reflect/Executable;)[Ljava/lang/Object;"),
};

void RegisterHookBridge(JNIEnv *env) {
    jclass method = env->FindClass("java/lang/reflect/Method");
    invoke = env->GetMethodID(
            method, "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    env->DeleteLocalRef(method);
    REGISTER_LSP_NATIVE_METHODS(HookBridge);
}
} // namespace lspd
