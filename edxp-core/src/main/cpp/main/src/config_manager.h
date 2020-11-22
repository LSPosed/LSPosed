
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

namespace edxp {

    static const std::string kPrimaryInstallerPkgName = "org.meowcat.edxposed.manager";
    static const std::string kLegacyInstallerPkgName = "de.robv.android.xposed.installer";
    static const std::string kXposedPropPath = "/system/framework/edconfig.jar";

    class ConfigManager {
    public:
        inline static ConfigManager *GetInstance() {
            return instances_[current_user].get();
        }

        inline static void SetCurrentUser(uid_t user) {
            if (auto instance = instances_.find(user);
                    !instance->second || instance->second->NeedUpdateConfig()) {
                instances_[user] = std::make_unique<ConfigManager>(user);
            }
        }

        inline static auto ReleaseInstances() {
            return std::move(instances_);
        }

        inline auto IsBlackWhiteListEnabled() const {
            return black_list_enable_ || white_list_enable_;
        }

        inline auto IsResourcesHookEnabled() const { return resources_hook_enabled_; }

        inline auto IsDeoptBootImageEnabled() const { return deopt_boot_image_enabled_; }

        inline auto IsNoModuleLogEnabled() const { return no_module_log_enabled_; }

        inline auto IsHiddenAPIBypassEnabled() const { return hidden_api_bypass_enabled_; }

        inline auto GetInstallerPackageName() const { return installer_pkg_name_; }

        inline auto GetXposedPropPath() const { return kXposedPropPath; }

        inline auto GetLibSandHookName() const { return kLibSandHookName; }

        inline auto GetDataPathPrefix() const { return data_path_prefix_; }

        inline auto GetConfigPath(const std::string &suffix) const {
            return data_path_prefix_ / installer_pkg_name_ / "conf" / suffix;
        }

        std::vector<std::string> GetAppModuleList(const std::string &pkg_name) const;

        bool IsAppNeedHook(const std::string &pkg_name) const;

        bool NeedUpdateConfig() const {
            return last_write_time_ < GetLastWriteTime();
        }


    private:
        inline static std::unordered_map<uid_t, std::unique_ptr<ConfigManager>> instances_{};
        inline static uid_t current_user = 0u;
        inline static bool use_prot_storage_ = GetAndroidApiLevel() >= __ANDROID_API_N__;

        const uid_t user_;
        const std::filesystem::path data_path_prefix_;
        const std::filesystem::path installer_pkg_name_;
        const bool black_list_enable_ = false;
        const bool white_list_enable_ = false;
        const bool deopt_boot_image_enabled_ = false;
        const bool no_module_log_enabled_ = false;
        const bool resources_hook_enabled_ = false;
        const bool hidden_api_bypass_enabled_ = false;
        // snapshot at boot
        const std::unordered_set<std::string> white_list_;
        const std::unordered_set<std::string> black_list_;

        const std::vector<std::pair<std::string, std::unordered_set<std::string>>> modules_list_;

        const std::filesystem::file_time_type last_write_time_;

        ConfigManager(uid_t uid);

        static std::unordered_set<std::string> GetAppList(const std::filesystem::path &dir);

        std::string RetrieveInstallerPkgName() const;

        static std::string GetPackageNameFromBaseApkPath(const std::filesystem::path &path);

        std::remove_const_t<decltype(modules_list_)> GetModuleList();

        std::filesystem::file_time_type GetLastWriteTime() const;

        friend std::unique_ptr<ConfigManager> std::make_unique<ConfigManager>(uid_t &);

    };

} // namespace edxp

