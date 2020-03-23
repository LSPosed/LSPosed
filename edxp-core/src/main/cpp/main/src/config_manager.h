
#pragma once

#include <vector>
#include <string>
#include <JNIHelper.h>
#include <art/runtime/native/native_util.h>

namespace edxp {

    static constexpr const char *kPrimaryInstallerPkgName = "org.meowcat.edxposed.manager";
    static constexpr const char *kLegacyInstallerPkgName = "de.robv.android.xposed.installer";

    class ConfigManager {
    public:

        static ConfigManager *GetInstance() {
            if (instance_ == 0) {
                instance_ = new ConfigManager();
            }
            return instance_;
        }

        bool IsBlackWhiteListEnabled() const;

        bool IsDynamicModulesEnabled() const;

        bool IsResourcesHookEnabled() const;

        bool IsDeoptBootImageEnabled() const;

        bool IsNoModuleLogEnabled() const;

        std::string GetInstallerPackageName() const;

        std::string GetDataPathPrefix() const;

        std::string GetConfigPath(const std::string &suffix) const;

        bool IsAppNeedHook(const std::string &app_data_dir);

    private:
        inline static ConfigManager *instance_;
        uid_t last_user_ = false;
        bool use_prot_storage_ = true;
        std::string data_path_prefix_;
        std::string installer_pkg_name_;
        std::string base_config_path_;
        std::string blacklist_path_;
        std::string whitelist_path_;
        std::string use_whitelist_path_;
        bool black_white_list_enabled_ = false;
        bool dynamic_modules_enabled_ = false;
        bool deopt_boot_image_enabled_ = false;
        bool no_module_log_enabled_ = false;
        bool resources_hook_enabled_ = true;
        // snapshot at boot
        bool use_white_list_snapshot_ = false;
        std::vector<std::string> white_list_default_;
        std::vector<std::string> black_list_default_;

        ConfigManager();

        void UpdateConfigPath(const uid_t user);

        void SnapshotBlackWhiteList();

        std::string RetrieveInstallerPkgName() const;
    };

} // namespace edxp

