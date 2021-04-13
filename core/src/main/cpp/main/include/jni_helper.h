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

#pragma once

#include <jni.h>
#include "macros.h"
#include <string>
#include "logging.h"
#include "base/object.h"

#define JNI_START JNIEnv *env, [[maybe_unused]] jclass clazz

class JUTFString {
public:
    inline JUTFString(JNIEnv *env, jstring jstr) : JUTFString(env, jstr, nullptr) {
    }

    inline JUTFString(JNIEnv *env, jstring jstr, const char *default_cstr) : env_(env),
                                                                             jstr_(jstr) {
        if (env_ && jstr_) cstr_ = env_->GetStringUTFChars(jstr, nullptr);
        else cstr_ = default_cstr;
    }

    inline operator const char *() const { return cstr_; }

    inline operator const std::string() const { return cstr_; }

    inline operator const bool() const { return cstr_ != nullptr; }

    inline auto get() const { return cstr_; }

    inline ~JUTFString() {
        if (env_ && jstr_) env_->ReleaseStringUTFChars(jstr_, cstr_);
    }

    JUTFString(JUTFString &&other)
            : env_(std::move(other.env_)), jstr_(std::move(other.jstr_)),
              cstr_(std::move(other.cstr_)) {
        other.cstr_ = nullptr;
    }

    JUTFString &
    operator=(JUTFString &&other) {
        if (&other != this) {
            env_ = std::move(other.env_);
            jstr_ = std::move(other.jstr_);
            cstr_ = std::move(other.cstr_);
            other.cstr_ = nullptr;
        }
        return *this;
    }

private:
    JNIEnv *env_;
    jstring jstr_;
    const char *cstr_;

    JUTFString(const JUTFString &) = delete;

    JUTFString &operator=(const JUTFString &) = delete;
};

template<typename T>
concept JObject = std::is_base_of_v<std::remove_pointer_t<jobject>, std::remove_pointer_t<T>>;

template<JObject T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv *env, T localRef) : mEnv(env), mLocalRef(localRef) {
    }

    ScopedLocalRef(ScopedLocalRef &&s) noexcept: mEnv(s.mEnv), mLocalRef(s.release()) {
    }

    template<JObject U>
    ScopedLocalRef(ScopedLocalRef<U> &&s) noexcept: mEnv(s.mEnv), mLocalRef((T) s.release()) {
    }

    explicit ScopedLocalRef(JNIEnv *env) : mEnv(env), mLocalRef(nullptr) {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset(T ptr = nullptr) {
        if (ptr != mLocalRef) {
            if (mLocalRef != nullptr) {
                mEnv->DeleteLocalRef(mLocalRef);
            }
            mLocalRef = ptr;
        }
    }

    [[nodiscard]] T release() {
        T localRef = mLocalRef;
        mLocalRef = nullptr;
        return localRef;
    }

    T get() const {
        return mLocalRef;
    }

    // We do not expose an empty constructor as it can easily lead to errors
    // using common idioms, e.g.:
    //   ScopedLocalRef<...> ref;
    //   ref.reset(...);
    // Move assignment operator.
    ScopedLocalRef &operator=(ScopedLocalRef &&s) noexcept {
        reset(s.release());
        mEnv = s.mEnv;
        return *this;
    }

    operator bool() const {
        return mLocalRef;
    }

    template<JObject U>
    friend
    class ScopedLocalRef;

private:
    JNIEnv *mEnv;
    T mLocalRef;
    DISALLOW_COPY_AND_ASSIGN(ScopedLocalRef);
};


template<typename T, typename U>
concept ScopeOrRaw = std::is_same_v<T, U> || std::is_same_v<ScopedLocalRef<T>, U>;
template<typename T>
concept ScopeOrClass = ScopeOrRaw<jclass, T>;
template<typename T>
concept ScopeOrObject = ScopeOrRaw<jobject, T>;

inline ScopedLocalRef<jstring> ClearException(JNIEnv *env) {
    if (auto exception = env->ExceptionOccurred()) {
        env->ExceptionClear();
        static jmethodID toString = env->GetMethodID(env->FindClass("java/lang/Object"), "toString",
                                                     "()Ljava/lang/String;");
        auto str = (jstring) env->CallObjectMethod(exception, toString);
        env->DeleteLocalRef(exception);
        return {env, str};
    }
    return {env, nullptr};
}

template<typename T>
[[maybe_unused]]
inline auto unwrap_scope(T &&x) {
    if constexpr (std::is_same_v<std::decay_t<T>, std::string_view>) return x.data();
    else if constexpr (lspd::is_instance<std::decay_t<T>, ScopedLocalRef>::value) return x.get();
    else return std::forward<T>(x);
}

template<typename T>
[[maybe_unused]]
inline auto wrap_scope(JNIEnv *env, T &&x) {
    if constexpr (std::is_convertible_v<T, jobject>) {
        return ScopedLocalRef(env, std::forward<T>(x));
    } else return x;
}

template<typename Func, typename ...Args>
requires(std::is_function_v<Func>)
[[maybe_unused]]
inline auto JNI_SafeInvoke(JNIEnv *env, Func JNIEnv::*f, Args &&... args) {
    struct finally {
        finally(JNIEnv *env) : env_(env) {}

        ~finally() {
            if (auto exception = ClearException(env_)) {
                LOGE("%s", JUTFString(env_, exception.get()).get());
            }
        }

        JNIEnv *env_;
    } _(env);

    if constexpr (!std::is_same_v<void, std::invoke_result_t<Func, decltype(unwrap_scope(
            std::forward<Args>(args)))...>>)
        return wrap_scope(env, (env->*f)(unwrap_scope(std::forward<Args>(args))...));
    else (env->*f)(unwrap_scope(std::forward<Args>(args))...);
}

[[maybe_unused]]
inline auto JNI_FindClass(JNIEnv *env, std::string_view name) {
    return JNI_SafeInvoke(env, &JNIEnv::FindClass, name);
}

template<ScopeOrObject Object>
[[maybe_unused]]
inline auto JNI_GetObjectClass(JNIEnv *env, const Object &obj) {
    return JNI_SafeInvoke(env, &JNIEnv::GetObjectClass, obj);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto
JNI_GetFieldID(JNIEnv *env, const Class &clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetFieldID, clazz, name, sig);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto JNI_GetObjectField(JNIEnv *env, const Class &clazz, jfieldID fieldId) {
    return JNI_SafeInvoke(env, &JNIEnv::GetObjectField, clazz, fieldId);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto
JNI_GetMethodID(JNIEnv *env, const Class &clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetMethodID, clazz, name, sig);
}

template<ScopeOrObject Object, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallObjectMethod(JNIEnv *env, const Object &obj, Args &&... args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallObjectMethod, obj, std::forward<Args>(args)...);
}

template<ScopeOrObject Object, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallVoidMethod(JNIEnv *env, const Object &obj, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallVoidMethod, obj, std::forward<Args>(args)...);
}

template<ScopeOrObject Object, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallBooleanMethod(JNIEnv *env, const Object &obj, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallBooleanMethod, obj, std::forward<Args>(args)...);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto
JNI_GetStaticFieldID(JNIEnv *env, const Class &clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticFieldID, clazz, name, sig);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto JNI_GetStaticObjectField(JNIEnv *env, const Class &clazz, jfieldID fieldId) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticObjectField, clazz, fieldId);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto
JNI_GetStaticMethodID(JNIEnv *env, const Class &clazz, std::string_view name,
                      std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticMethodID, clazz, name, sig);
}

template<ScopeOrClass Class, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticVoidMethod(JNIEnv *env, const Class &clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticVoidMethod, clazz, std::forward<Args>(args)...);
}

template<ScopeOrClass Class, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticObjectMethod(JNIEnv *env, const Class &clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticObjectMethod, clazz, std::forward<Args>(args)...);
}

template<ScopeOrClass Class, typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticIntMethod(JNIEnv *env, const Class &clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticIntMethod, clazz, std::forward<Args>(args)...);
}

template<ScopeOrRaw<jarray> Array>
[[maybe_unused]]
inline auto JNI_GetArrayLength(JNIEnv *env, const Array &array) {
    return JNI_SafeInvoke(env, &JNIEnv::GetArrayLength, array);
}

template<ScopeOrClass Class, typename ...Args>
[[maybe_unused]]
inline auto JNI_NewObject(JNIEnv *env, const Class &clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::NewObject, clazz, std::forward<Args>(args)...);
}

template<ScopeOrClass Class>
[[maybe_unused]]
inline auto
JNI_RegisterNatives(JNIEnv *env, const Class &clazz, const JNINativeMethod *methods, jint size) {
    return JNI_SafeInvoke(env, &JNIEnv::RegisterNatives, clazz, methods, size);
}

template<typename T>
[[maybe_unused]]
inline auto JNI_NewGlobalRef(JNIEnv *env, T &&x) requires(std::is_convertible_v<T, jobject>){
    return (T) env->NewGlobalRef(std::forward<T>(x));
}

template<typename T>
[[maybe_unused]]
inline auto
JNI_NewGlobalRef(JNIEnv *env, const ScopedLocalRef<T> &x) requires(
        std::is_convertible_v<T, jobject>){
    return (T) env->NewGlobalRef(x.get());
}
