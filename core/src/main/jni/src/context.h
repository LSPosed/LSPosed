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

#pragma once

#include <utility>
#include <unistd.h>
#include <vector>
#include <string>
#include <tuple>
#include <string_view>
#include "utils.h"
#include "utils/jni_helper.hpp"

namespace lspd {
    class Context {

    public:
        inline static Context *GetInstance() {
            return instance_.get();
        }

        inline static std::unique_ptr<Context> ReleaseInstance() {
            return std::move(instance_);
        }

        inline jobject GetCurrentClassLoader() const { return inject_class_loader_; }

        inline lsplant::ScopedLocalRef<jclass>
        FindClassFromCurrentLoader(JNIEnv *env, std::string_view className) const {
            return FindClassFromLoader(env, GetCurrentClassLoader(), className);
        };

        void OnNativeForkAndSpecializePre(JNIEnv *env, jint uid, jintArray &gids, jstring nice_name,
                                          jboolean is_child_zygote, jstring app_data_dir);

        void OnNativeForkAndSpecializePost(JNIEnv *env, jstring nice_name, jstring app_data_dir);

        void OnNativeForkSystemServerPost(JNIEnv *env);

        void OnNativeForkSystemServerPre(JNIEnv *env);

        void Init();

    private:
        inline static std::unique_ptr<Context> instance_ = std::make_unique<Context>();
        jobject inject_class_loader_ = nullptr;
        jclass entry_class_ = nullptr;
        bool skip_ = false;

        struct PreloadedDex {

            PreloadedDex() : addr_(nullptr), size_(0) {}

            PreloadedDex(const PreloadedDex &) = delete;

            PreloadedDex &operator=(const PreloadedDex &) = delete;

            PreloadedDex(int fd, std::size_t size);

            PreloadedDex &operator=(PreloadedDex &&other) {
                addr_ = other.addr_;
                size_ = other.size_;
                other.addr_ = nullptr;
                other.size_ = 0;
                return *this;
            }

            PreloadedDex(PreloadedDex &&other) : addr_(other.addr_), size_(other.size_) {
                other.addr_ = nullptr;
                other.size_ = 0;
            };

            // Use with caution!
            PreloadedDex(void* addr, size_t size) : addr_(addr), size_(size) {};

            operator bool() const { return addr_ && size_; }

            auto size() const { return size_; }

            auto data() const { return addr_; }

            ~PreloadedDex();

        private:
            void *addr_;
            std::size_t size_;
        };

        Context() {}

        void LoadDex(JNIEnv *env, int fd, size_t size);

        void Init(JNIEnv *env);

        static lsplant::ScopedLocalRef<jclass> FindClassFromLoader(JNIEnv *env, jobject class_loader,
                                                          std::string_view class_name);

        static void setAllowUnload(bool unload);

        template<typename ...Args>
        void FindAndCall(JNIEnv *env, std::string_view method_name, std::string_view method_sig,
                         Args &&... args) const;

        friend std::unique_ptr<Context> std::make_unique<Context>();
    };

}
