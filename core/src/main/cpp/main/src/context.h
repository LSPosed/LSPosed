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

#include <utility>
#include <unistd.h>
#include <vector>
#include <string>
#include <tuple>
#include <string_view>
#include "utils.h"
#include "jni_helper.h"

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

        void CallOnPostFixupStaticTrampolines(void *class_ptr);

        inline ScopedLocalRef<jclass> FindClassFromCurrentLoader(JNIEnv *env, std::string_view className) const {
            return FindClassFromLoader(env, GetCurrentClassLoader(), className);
        };

        void OnNativeForkAndSpecializePre(JNIEnv *env, jint uid, jstring nice_name, jboolean is_child_zygote, jstring app_data_dir);

        void OnNativeForkAndSpecializePost(JNIEnv *env);

        void OnNativeForkSystemServerPost(JNIEnv *env, jint res);

        void OnNativeForkSystemServerPre(JNIEnv *env);

        void PreLoadDex(std::string_view dex_paths);

    private:
        inline static std::unique_ptr<Context> instance_ = std::make_unique<Context>();
        jobject inject_class_loader_ = nullptr;
        jclass entry_class_ = nullptr;
        jstring app_data_dir_ = nullptr;
        jstring nice_name_ = nullptr;
        JavaVM *vm_ = nullptr;
        jclass class_linker_class_ = nullptr;
        jmethodID post_fixup_static_mid_ = nullptr;
        bool skip_ = false;
        std::vector<std::byte> dex{};

        Context() {}

        void LoadDex(JNIEnv *env);

        void Init(JNIEnv *env);

        static ScopedLocalRef<jclass> FindClassFromLoader(JNIEnv *env, jobject class_loader,
                                          std::string_view class_name);
        static void setAllowUnload(bool unload);

        template<typename ...Args>
        void FindAndCall(JNIEnv *env, std::string_view method_name, std::string_view method_sig, Args&&... args) const;

        friend std::unique_ptr<Context> std::make_unique<Context>();
    };

}
