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

#include <vector>
#include <string>
#include "JNIHelper.h"
#include <utility>
#include <art/runtime/native/native_util.h>
#include <filesystem>
#include <unordered_set>
#include <unordered_map>
#include "config.h"
#include "utils.h"

namespace lspd {

    class ConfigManager {
    private:
        inline static const auto kPrimaryInstallerPkgName = "io.github.lsposed.manager"s;
        inline static const auto kXposedInjectDexPath = "lspd.dex";

    public:
        static void Init();

        inline static ConfigManager *GetInstance() {
            return instances_[current_user_].get();
        }

        inline auto IsInitialized() const { return initialized_; }

        inline static void SetCurrentUser(uid_t user) {
            if (auto instance = instances_.find(user);
                    instance == instances_.end() || !instance->second) {
                instances_[user] = std::make_unique<ConfigManager>(user);
            } else if (instance->second->NeedUpdateConfig()) {
                instances_[user] = std::make_unique<ConfigManager>(user,
                                                                   instance->second->IsInitialized());
            }
            current_user_ = user;
        }

        inline static auto ReleaseInstances() {
            return std::move(instances_);
        }

        inline const auto &GetVariant() const { return variant_; }

        inline const auto &IsResourcesHookEnabled() const { return resources_hook_enabled_; }

        inline const auto &IsNoModuleLogEnabled() const { return no_module_log_enabled_; }

        inline const auto &GetInstallerPackageName() const { return installer_pkg_name_; }

        inline const auto &GetDataPathPrefix() const { return data_path_prefix_; }

        inline static const auto &GetMiscPath() { return misc_path_; }

        inline static auto GetFrameworkPath(const std::string &suffix = {}) {
            return misc_path_ / "framework" / suffix;
        }

        inline static auto GetCachePath(const std::string &suffix = {}) {
            return misc_path_ / "cache" / suffix;
        }

        inline auto GetConfigPath(const std::string &suffix = {}) const {
            return base_config_path_ / "conf" / suffix;
        }

        inline static auto GetLogPath(const std::string &suffix = {}) {
            return misc_path_ / "log" / suffix;
        }

        inline const auto &GetBaseConfigPath() const { return base_config_path_; }

        inline auto GetPrefsPath(const std::string &pkg_name) const {
            return base_config_path_ / "prefs" / pkg_name;
        }

        inline static auto GetVariantPath() {
            return misc_path_ / "variant";
        }

        inline static std::filesystem::path GetSelinuxStatusPath() {
            return "/sys/fs/selinux/enforce";
        }

        inline static auto GetModulesLogPath() {
            return GetLogPath("modules.log");
        }

        std::vector<std::string> GetAppModuleList(const std::string &pkg_name) const;

        bool NeedUpdateConfig() const {
            return last_write_time_ < GetLastWriteTime();
        }

        void EnsurePermission(const std::string &pkg_name, uid_t uid) const;

        static const auto &GetInjectDexPath() { return inject_dex_path_; };

        bool IsInstaller(const std::string &pkg_name) const {
            return pkg_name == installer_pkg_name_ || pkg_name == kPrimaryInstallerPkgName;
        }

        bool IsPermissive() const {
            return selinux_permissive_;
        }


    private:
        inline static std::unordered_map<uid_t, std::unique_ptr<ConfigManager>> instances_{};
        inline static uid_t current_user_ = 0u;
        inline static std::filesystem::path misc_path_;   // /data/misc/lspd_xxxx
        inline static std::filesystem::path inject_dex_path_;

        const uid_t user_;
        const int variant_;
        const std::filesystem::path data_path_prefix_;   // /data/user_de/{user}
        const std::filesystem::path base_config_path_;   // /data/misc/lspd_xxxx/{user}
        const bool initialized_ = false;
        const std::filesystem::path installer_pkg_name_;
        const bool no_module_log_enabled_ = false;
        const bool resources_hook_enabled_ = false;
        const bool selinux_permissive_ = false;

        const std::unordered_map<std::string, std::pair<std::string, std::unordered_set<std::string>>> modules_list_;

        const std::filesystem::file_time_type last_write_time_;

        ConfigManager(uid_t uid, bool initialized = false);

        std::string RetrieveInstallerPkgName() const;

        static std::string GetPackageNameFromBaseApkPath(const std::filesystem::path &path);

        std::remove_const_t<decltype(modules_list_)> GetModuleList();

        std::filesystem::file_time_type GetLastWriteTime() const;

        bool InitConfigPath() const;

        friend std::unique_ptr<ConfigManager> std::make_unique<ConfigManager>(uid_t &);

        friend std::unique_ptr<ConfigManager> std::make_unique<ConfigManager>(uid_t &, bool &&);

        std::filesystem::path RetrieveBaseConfigPath() const;

        static int ReadInt(const std::filesystem::path &dir);
    };

} // namespace lspd

