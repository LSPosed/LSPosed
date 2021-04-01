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
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv *env, T localRef) : mEnv(env), mLocalRef(localRef) {
    }

    ScopedLocalRef(ScopedLocalRef &&s) noexcept: mEnv(s.mEnv), mLocalRef(s.release()) {
    }

    explicit ScopedLocalRef(JNIEnv *env) : mEnv(env), mLocalRef(nullptr) {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset(T ptr = NULL) {
        if (ptr != mLocalRef) {
            if (mLocalRef != NULL) {
                mEnv->DeleteLocalRef(mLocalRef);
            }
            mLocalRef = ptr;
        }
    }

    T release() __attribute__((warn_unused_result)) {
        T localRef = mLocalRef;
        mLocalRef = NULL;
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

    // Allows "if (scoped_ref == nullptr)"
    bool operator==(std::nullptr_t) const {
        return mLocalRef == nullptr;
    }

    // Allows "if (scoped_ref != nullptr)"
    bool operator!=(std::nullptr_t) const {
        return mLocalRef != nullptr;
    }

    operator bool() const {
        return mLocalRef;
    }

private:
    JNIEnv *mEnv;
    T mLocalRef;
    DISALLOW_COPY_AND_ASSIGN(ScopedLocalRef);
};

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
inline auto unwrap_sv(T &&x) {
    if constexpr (std::is_same_v<std::decay_t<T>, std::string_view>) return x.data();
    else return std::forward<T>(x);
}

template<typename Func, typename ...Args>
[[maybe_unused]]
inline auto JNI_SafeInvoke(JNIEnv *env, Func f, Args &&... args) {
    static_assert(std::is_member_function_pointer_v<Func>);
    struct finally {
        finally(JNIEnv *env) : env_(env) {}

        ~finally() {
            if (auto exception = ClearException(env_)) {
                LOGE("%s", JUTFString(env_, exception.get()).get());
            }
        }

        JNIEnv *env_;
    } _(env);

    return (env->*f)(unwrap_sv(std::forward<Args>(args))...);
}

[[maybe_unused]]
inline auto JNI_FindClass(JNIEnv *env, std::string_view name) {
    return JNI_SafeInvoke(env, &JNIEnv::FindClass, name);
}

[[maybe_unused]]
inline auto JNI_GetObjectClass(JNIEnv *env, jobject obj) {
    return JNI_SafeInvoke(env, &JNIEnv::GetObjectClass, obj);
}

[[maybe_unused]]
inline auto JNI_GetFieldID(JNIEnv *env, jclass clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetFieldID, clazz, name, sig);
}

[[maybe_unused]]
inline auto JNI_GetObjectField(JNIEnv *env, jclass clazz, jfieldID fieldId) {
    return JNI_SafeInvoke(env, &JNIEnv::GetObjectField, clazz, fieldId);
}

[[maybe_unused]]
inline auto
JNI_GetMethodID(JNIEnv *env, jclass clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetMethodID, clazz, name, sig);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallObjectMethod(JNIEnv *env, jobject obj, Args &&... args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallObjectMethod, obj, std::forward<Args>(args)...);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallVoidMethod(JNIEnv *env, jobject obj, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallVoidMethod, obj, std::forward<Args>(args)...);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallBooleanMethod(JNIEnv *env, jobject obj, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallBooleanMethod, obj, std::forward<Args>(args)...);
}

[[maybe_unused]]
inline auto
JNI_GetStaticFieldID(JNIEnv *env, jclass clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticFieldID, clazz, name, sig);
}

[[maybe_unused]]
inline auto JNI_GetStaticObjectField(JNIEnv *env, jclass clazz, jfieldID fieldId) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticObjectField, clazz, fieldId);
}

[[maybe_unused]]
inline auto
JNI_GetStaticMethodID(JNIEnv *env, jclass clazz, std::string_view name, std::string_view sig) {
    return JNI_SafeInvoke(env, &JNIEnv::GetStaticMethodID, clazz, name, sig);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticVoidMethod(JNIEnv *env, jclass clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticVoidMethod, clazz, std::forward<Args>(args)...);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticObjectMethod(JNIEnv *env, jclass clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticObjectMethod, clazz, std::forward<Args>(args)...);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_CallStaticIntMethod(JNIEnv *env, jclass clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::CallStaticIntMethod, clazz, std::forward<Args>(args)...);
}

[[maybe_unused]]
inline auto JNI_GetArrayLength(JNIEnv *env, jarray array) {
    return JNI_SafeInvoke(env, &JNIEnv::GetArrayLength, array);
}

template<typename ...Args>
[[maybe_unused]]
inline auto JNI_NewObject(JNIEnv *env, jclass clazz, Args &&...args) {
    return JNI_SafeInvoke(env, &JNIEnv::NewObject, clazz, std::forward<Args>(args)...);
}

[[maybe_unused]]
inline auto
JNI_RegisterNatives(JNIEnv *env, jclass clazz, const JNINativeMethod *methods, jint size) {
    return JNI_SafeInvoke(env, &JNIEnv::RegisterNatives, clazz, methods, size);
}
