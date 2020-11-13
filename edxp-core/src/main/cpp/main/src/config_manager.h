
#pragma once

#include <vector>
#include <string>
#include "JNIHelper.h"
#include <utility>
#include <art/runtime/native/native_util.h>
#include <filesystem>
#include <unordered_set>

namespace edxp {

    static const std::string kPrimaryInstallerPkgName = "org.meowcat.edxposed.manager";
    static const std::string kLegacyInstallerPkgName = "de.robv.android.xposed.installer";
    static const std::string kXposedPropPath = "/system/framework/edconfig.jar";

    class ConfigManager {
    public:

        inline static ConfigManager *GetInstance() {
            if (!instance_) {
                instance_ = std::make_unique<ConfigManager>();
            }
            return instance_.get();
        }

        inline static std::unique_ptr<ConfigManager> ReleaseInstance() {
            return std::move(instance_);
        }

        inline auto IsBlackWhiteListEnabled() const { return black_white_list_enabled_; }

        inline auto IsDynamicModulesEnabled() const { return dynamic_modules_enabled_; }

        inline auto IsResourcesHookEnabled() const { return resources_hook_enabled_; }

        inline auto IsDeoptBootImageEnabled() const { return deopt_boot_image_enabled_; }

        inline auto IsNoModuleLogEnabled() const { return no_module_log_enabled_; }

        inline auto IsHiddenAPIBypassEnabled() const { return hidden_api_bypass_enabled_; }

        inline auto GetInstallerPackageName() const { return installer_pkg_name_; }

        inline auto GetXposedPropPath() const { return kXposedPropPath; }

        inline auto GetLibSandHookName() const { return kLibSandHookName; }

        inline auto GetLibWhaleName() const { return kLibWhaleName; }

        inline auto GetDataPathPrefix() const { return data_path_prefix_; }

        inline auto GetConfigPath(const std::string &suffix) const {
            return data_path_prefix_ / installer_pkg_name_ / "conf" / suffix;
        }

        inline auto GetModulesList() const { return modules_list_.get(); }

        bool IsAppNeedHook(const std::string &app_data_dir);

        bool UpdateModuleList();

        static std::tuple<bool, uid_t, std::string>
        GetAppInfoFromDir(const std::string &app_data_dir);

    private:
        inline static std::unique_ptr<ConfigManager> instance_ = nullptr;
        uid_t last_user_ = false;
        bool use_prot_storage_ = true;
        std::filesystem::path data_path_prefix_;
        std::filesystem::path installer_pkg_name_;
        std::filesystem::path base_config_path_;
        std::filesystem::path blacklist_path_;
        std::filesystem::path whitelist_path_;
        std::filesystem::path use_whitelist_path_;
        bool black_white_list_enabled_ = false;
        bool dynamic_modules_enabled_ = false;
        bool deopt_boot_image_enabled_ = false;
        bool no_module_log_enabled_ = false;
        bool resources_hook_enabled_ = false;
        // snapshot at boot
        bool use_white_list_snapshot_ = false;
        std::unordered_set<std::string> white_list_default_;
        std::unordered_set<std::string> black_list_default_;
        bool hidden_api_bypass_enabled_ = false;

        std::unique_ptr<std::string> modules_list_ = nullptr;

        std::filesystem::file_time_type last_write_time_;

        ConfigManager();

        void UpdateConfigPath(const uid_t user);

        void SnapshotBlackWhiteList();

        std::string RetrieveInstallerPkgName() const;

        friend std::unique_ptr<ConfigManager> std::make_unique<ConfigManager>();
    };

} // namespace edxp

