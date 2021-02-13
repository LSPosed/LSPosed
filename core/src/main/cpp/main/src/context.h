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

namespace lspd {
    enum Variant {
        NONE = 0,
        YAHFA = 1,
        SANDHOOK = 2,
    };

    class Context {

    public:
        inline static Context *GetInstance() {
            if (!instance_) {
                instance_ = std::make_unique<Context>();
            }
            return instance_.get();
        }

        inline static std::unique_ptr<Context> ReleaseInstance() {
            return std::move(instance_);
        }

        inline auto GetCurrentClassLoader() const { return inject_class_loader_; }

        void CallOnPostFixupStaticTrampolines(void *class_ptr);

        void PrepareJavaEnv(JNIEnv *env);

        void FindAndCall(JNIEnv *env, const char *method_name, const char *method_sig, ...) const;

        inline auto *GetJavaVM() const { return vm_; }

        inline void SetAppDataDir(jstring app_data_dir) { app_data_dir_ = app_data_dir; }

        inline void SetNiceName(jstring nice_name) { nice_name_ = nice_name; }

        inline auto GetAppDataDir() const { return app_data_dir_; }

        inline auto GetNiceName() const { return nice_name_; }

        inline auto GetAppModulesList() const { return app_modules_list_; }

        inline jclass FindClassFromLoader(JNIEnv *env, const std::string &className) const {
            return FindClassFromLoader(env, className.c_str());
        };

        inline jclass FindClassFromLoader(JNIEnv *env, const char *className) const {
            return FindClassFromLoader(env, GetCurrentClassLoader(), className);
        }

        void OnNativeForkAndSpecializePre(JNIEnv *env, jclass clazz, jint uid, jint gid,
                                          jintArray gids, jint runtime_flags, jobjectArray rlimits,
                                          jint mount_external,
                                          jstring se_info, jstring se_name, jintArray fds_to_close,
                                          jintArray fds_to_ignore, jboolean is_child_zygote,
                                          jstring instruction_set, jstring app_data_dir);

        int OnNativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res);

        int OnNativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res);

        void OnNativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t uid, gid_t gid,
                                         jintArray gids, jint runtime_flags, jobjectArray rlimits,
                                         jlong permitted_capabilities,
                                         jlong effective_capabilities);

        inline auto IsInitialized() const { return initialized_; }

        inline auto GetVariant() const { return variant_; };

    private:
        inline static std::unique_ptr<Context> instance_;
        bool initialized_ = false;
        Variant variant_ = NONE;
        jobject inject_class_loader_ = nullptr;
        jclass entry_class_ = nullptr;
        jstring app_data_dir_ = nullptr;
        jstring nice_name_ = nullptr;
        JavaVM *vm_ = nullptr;
        jclass class_linker_class_ = nullptr;
        jmethodID post_fixup_static_mid_ = nullptr;
        bool skip_ = false;
        std::vector<std::vector<signed char>> dexes;
        std::vector<std::string> app_modules_list_;

        Context() {}

        void PreLoadDex(const std::filesystem::path &dex_paths);

        void InjectDexAndInit(JNIEnv *env);

        inline jclass FindClassFromLoader(JNIEnv *env, jobject class_loader,
                                          const std::string &class_name) const {
            return FindClassFromLoader(env, class_loader, class_name.c_str());
        }

        static jclass
        FindClassFromLoader(JNIEnv *env, jobject class_loader, const char *class_name);

        static bool
        ShouldSkipInject(const std::string &package_name, uid_t user, uid_t uid, bool res,
                         const std::function<bool()>& empty_list,
                         bool is_child_zygote);

        static std::tuple<bool, uid_t, std::string> GetAppInfoFromDir(JNIEnv *env, jstring dir, jstring nice_name);

        friend std::unique_ptr<Context> std::make_unique<Context>();

        static void RegisterEdxpService(JNIEnv *env);
    };

}
