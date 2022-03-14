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

#include "hook_bridge.h"
#include "native_util.h"
#include "lsplant.hpp"
#include "unordered_map"
#include <shared_mutex>
#include <set>

using namespace lsplant;

namespace {

struct HookItem {
    jobject backup {nullptr};
    jobjectArray callbacks {nullptr};
    std::multiset<jint> priorities {};
};

std::shared_mutex hooked_lock;
// Rehashing invalidates iterators, changes ordering between elements, and changes which buckets elements appear in, but does not invalidate pointers or references to elements.
std::unordered_map<jmethodID, HookItem> hooked_methods;

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
                LOGV("New hook took %lldus",
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
            hook_item = &found->second;
        }
    }
    if (!hook_item) {
        std::unique_lock lk(hooked_lock);
        hook_item = &hooked_methods[target];
        if (!hook_item->callbacks) {
            newHook = true;
            hook_item->callbacks = (jobjectArray) env->NewGlobalRef(
                    env->NewObjectArray(1, env->FindClass("[Ljava/lang/Object;"),
                                        env->NewObjectArray(0, JNI_FindClass(env, "java/lang/Object"), nullptr)));
        }
    }
    if (newHook) {
        auto init = env->GetMethodID(hooker, "<init>", "(Ljava/lang/reflect/Executable;[[Ljava/lang/Object;)V");
        auto callback_method = env->ToReflectedMethod(hooker, env->GetMethodID(hooker, "callback",
                                                                               "([Ljava/lang/Object;)Ljava/lang/Object;"),
                                                      false);
        auto hooker_object = env->NewObject(hooker, init, hookMethod, hook_item->callbacks);
        hook_item->backup = lsplant::Hook(env, hookMethod, hooker_object, callback_method);
        env->DeleteLocalRef(hooker_object);
    }
    env->MonitorEnter(hook_item->callbacks);
    auto insert_point = hook_item->priorities.emplace(priority);
    auto old_array = (jobjectArray) env->GetObjectArrayElement(hook_item->callbacks, 0);
    auto new_array = env->NewObjectArray(static_cast<jint>(hook_item->priorities.size()), env->FindClass("java/lang/Object"), nullptr);
    for (auto [i, current, passed] = std::make_tuple(0, hook_item->priorities.begin(), false); current != hook_item->priorities.end(); ++current, ++i) {
        if (current == insert_point) {
            env->SetObjectArrayElement(new_array, i, callback);
            passed = true;
        } else {
            auto element = env->GetObjectArrayElement(old_array, i - passed);
            env->SetObjectArrayElement(new_array, i, element);
            env->DeleteLocalRef(element);
        }
    }
    env->SetObjectArrayElement(hook_item->callbacks, 0, new_array);
    env->DeleteLocalRef(old_array);
    env->DeleteLocalRef(new_array);
    env->MonitorExit(hook_item->callbacks);
    return hook_item->backup ? JNI_TRUE : JNI_FALSE;
}

LSP_DEF_NATIVE_METHOD(jboolean, HookBridge, unhookMethod, jobject hookMethod, jobject callback) {
    auto target = env->FromReflectedMethod(hookMethod);
    HookItem * hook_item = nullptr;
    {
        std::shared_lock lk(hooked_lock);
        if (auto found = hooked_methods.find(target); found != hooked_methods.end()) {
            hook_item = &found->second;
        }
    }
    if (!hook_item) return JNI_FALSE;
    env->MonitorEnter(hook_item->callbacks);
    auto old_array = (jobjectArray) env->GetObjectArrayElement(hook_item->callbacks, 0);
    auto new_array = env->NewObjectArray(static_cast<jint>(hook_item->priorities.size() - 1), env->FindClass("java/lang/Object"), nullptr);
    auto to_remove = hook_item->priorities.end();
    for (auto [i, current, passed] = std::make_tuple(0, hook_item->priorities.begin(), false); current != hook_item->priorities.end(); ++current, ++i) {
        auto element = env->GetObjectArrayElement(old_array, i);
        if (env->IsSameObject(element, callback)) {
            to_remove = current;
            passed = true;
        } else {
            if (i - passed >= hook_item->priorities.size() - 1) {
                return JNI_FALSE;
            }
            env->SetObjectArrayElement(new_array, i - passed, element);
        }
        env->DeleteLocalRef(element);
    }
    bool removed = false;
    if (to_remove != hook_item->priorities.end()) {
        hook_item->priorities.erase(to_remove);
        env->SetObjectArrayElement(hook_item->callbacks, 0, new_array);
        removed = true;
    }
    env->DeleteLocalRef(old_array);
    env->DeleteLocalRef(new_array);
    env->MonitorExit(hook_item->callbacks);
    return removed ? JNI_TRUE : JNI_FALSE;
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
            hook_item = &found->second;
        }
    }
    jobject to_call = hookMethod;
    if (hook_item && hook_item->backup) {
        to_call = hook_item->backup;
    }
    return env->CallObjectMethod(to_call, invoke, thiz, args);
}

static JNINativeMethod gMethods[] = {
    LSP_NATIVE_METHOD(HookBridge, hookMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Class;ILjava/lang/Object;)Z"),
    LSP_NATIVE_METHOD(HookBridge, unhookMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Object;)Z"),
    LSP_NATIVE_METHOD(HookBridge, deoptimizeMethod, "(Ljava/lang/reflect/Executable;)Z"),
    LSP_NATIVE_METHOD(HookBridge, invokeOriginalMethod, "(Ljava/lang/reflect/Executable;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
};

void RegisterHookBridge(JNIEnv *env) {
    auto method = env->FindClass("java/lang/reflect/Method");
    invoke = env->GetMethodID(
            method, "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    env->DeleteLocalRef(method);
    REGISTER_LSP_NATIVE_METHODS(HookBridge);
}
} // namespace lspd
