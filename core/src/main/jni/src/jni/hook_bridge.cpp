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
#include <mutex>
#include <set>

using namespace lsplant;

namespace {

struct HookItem {
    jobject backup {nullptr};
    std::multimap<jint, jobject, std::greater<>> callbacks {};
};

std::shared_mutex hooked_lock;
std::recursive_mutex backup_lock;
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
        std::unique_lock lk(backup_lock);
        hook_item->backup = lsplant::Hook(env, hookMethod, hooker_object, callback_method);
        env->DeleteLocalRef(hooker_object);
    }
    jobject backup = nullptr;
    {
        std::unique_lock lk(backup_lock);
        backup = hook_item->backup;
    }
    if (!backup) return JNI_FALSE;
    JNIMonitor monitor(env, backup);
    hook_item->callbacks.emplace(std::make_pair(priority, env->NewGlobalRef(callback)));
    return JNI_TRUE;
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
    jobject backup = nullptr;
    {
        std::unique_lock lk(backup_lock);
        backup = hook_item->backup;
    }
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
    if (hook_item) {
        std::unique_lock lk(backup_lock);
        if (hook_item->backup) to_call = hook_item->backup;
    }
    return env->CallObjectMethod(to_call, invoke, thiz, args);
}

LSP_DEF_NATIVE_METHOD(jobject, HookBridge, allocateObject, jclass cls) {
    return env->AllocObject(clazz);
}

LSP_DEF_NATIVE_METHOD(jobject, HookBridge, invokeSpecialMethod, jobject method, jcharArray shorty,
                      jclass cls, jobject thiz, jobjectArray args) {
    static auto* const get_int = env->GetMethodID(env->FindClass("java/lang/Integer"), "intValue", "()I");
    static auto* const get_double = env->GetMethodID(env->FindClass("java/lang/Double"), "doubleValue", "()D");
    static auto* const get_long = env->GetMethodID(env->FindClass("java/lang/Long"), "longValue", "()J");
    static auto* const get_float = env->GetMethodID(env->FindClass("java/lang/Float"), "floatValue", "()F");
    static auto* const get_short = env->GetMethodID(env->FindClass("java/lang/Short"), "shortValue", "()S");
    static auto* const get_byte = env->GetMethodID(env->FindClass("java/lang/Byte"), "byteValue", "()B");
    static auto* const get_char = env->GetMethodID(env->FindClass("java/lang/Character"), "charValue", "()C");
    static auto* const get_boolean = env->GetMethodID(env->FindClass("java/lang/Boolean"), "booleanValue", "()Z");
    static auto* const set_int = env->GetStaticMethodID(env->FindClass("java/lang/Integer"), "valueOf", "(I)Ljava/lang/Integer;");
    static auto* const set_double = env->GetStaticMethodID(env->FindClass("java/lang/Double"), "valueOf", "(D)Ljava/lang/Double;");
    static auto* const set_long = env->GetStaticMethodID(env->FindClass("java/lang/Long"), "valueOf", "(J)Ljava/lang/Long;");
    static auto* const set_float = env->GetStaticMethodID(env->FindClass("java/lang/Float"), "valueOf", "(F)Ljava/lang/Float;");
    static auto* const set_short = env->GetStaticMethodID(env->FindClass("java/lang/Short"), "valueOf", "(S)Ljava/lang/Short;");
    static auto* const set_byte = env->GetStaticMethodID(env->FindClass("java/lang/Byte"), "valueOf", "(B)Ljava/lang/Byte;");
    static auto* const set_char = env->GetStaticMethodID(env->FindClass("java/lang/Character"), "valueOf", "(C)Ljava/lang/Character;");
    static auto* const set_boolean = env->GetStaticMethodID(env->FindClass("java/lang/Boolean"), "valueOf", "(Z)Ljava/lang/Boolean;");

    auto target = env->FromReflectedMethod(method);
    auto param_len = env->GetArrayLength(shorty) - 1;
    if (env->GetArrayLength(args) != param_len) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "args.length != parameters.length");
        return nullptr;
    }
    if (thiz == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "this == null");
        return nullptr;
    }
    std::vector<jvalue> a(param_len);
    auto *const shorty_char = env->GetCharArrayElements(shorty, nullptr);
    for (jint i = 0; i != param_len; ++i) {
        jobject element = nullptr;
        switch(shorty_char[i + 1]) {
            case 'I':
                a[i].i = env->CallIntMethod(element = env->GetObjectArrayElement(args, i), get_int);
                break;
            case 'D':
                a[i].d = env->CallDoubleMethod(element = env->GetObjectArrayElement(args, i), get_double);
                break;
            case 'J':
                a[i].j = env->CallLongMethod(element = env->GetObjectArrayElement(args, i), get_long);
                break;
            case 'F':
                a[i].f = env->CallFloatMethod(element = env->GetObjectArrayElement(args, i), get_float);
                break;
            case 'S':
                a[i].s = env->CallShortMethod(element = env->GetObjectArrayElement(args, i), get_short);
                break;
            case 'B':
                a[i].b = env->CallByteMethod(element = env->GetObjectArrayElement(args, i), get_byte);
                break;
            case 'C':
                a[i].c = env->CallCharMethod(element = env->GetObjectArrayElement(args, i), get_char);
                break;
            case 'Z':
                a[i].z = env->CallBooleanMethod(element = env->GetObjectArrayElement(args, i), get_boolean);
                break;
            default:
            case 'L':
                a[i].l = env->GetObjectArrayElement(args, i);
                element = nullptr;
                break;
        }
        if (element) env->DeleteLocalRef(element);
        if (env->ExceptionCheck()) return nullptr;
    }
    jobject value = nullptr;
    switch(shorty_char[0]) {
        case 'I':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_int, env->CallNonvirtualIntMethodA(thiz, cls, target, a.data()));
            break;
        case 'D':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_double, env->CallNonvirtualDoubleMethodA(thiz, cls, target, a.data()));
            break;
        case 'J':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_long, env->CallNonvirtualLongMethodA(thiz, cls, target, a.data()));
            break;
        case 'F':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_float, env->CallNonvirtualFloatMethodA(thiz, cls, target, a.data()));
            break;
        case 'S':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_short, env->CallNonvirtualShortMethodA(thiz, cls, target, a.data()));
            break;
        case 'B':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_byte, env->CallNonvirtualByteMethodA(thiz, cls, target, a.data()));
            break;
        case 'C':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_char, env->CallNonvirtualCharMethodA(thiz, cls, target, a.data()));
            break;
        case 'Z':
            value = env->CallStaticObjectMethod(jclass{nullptr}, set_boolean, env->CallNonvirtualBooleanMethodA(thiz, cls, target, a.data()));
            break;
        case 'L':
            value = env->CallNonvirtualObjectMethodA(thiz, cls, target, a.data());
            break;
        default:
        case 'V':
            env->CallNonvirtualVoidMethodA(thiz, cls, target, a.data());
            break;
    }
    env->ReleaseCharArrayElements(shorty, shorty_char, JNI_ABORT);
    return value;
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
    jobject backup = nullptr;
    {
        std::unique_lock lk(backup_lock);
        backup = hook_item->backup;
    }
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
    LSP_NATIVE_METHOD(HookBridge, invokeSpecialMethod, "(Ljava/lang/reflect/Executable;[CLjava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
    LSP_NATIVE_METHOD(HookBridge, allocateObject, "(Ljava/lang/Class;)Ljava/lang/Object;"),
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
