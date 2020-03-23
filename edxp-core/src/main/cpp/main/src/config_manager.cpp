//
// Created by solo on 2019/5/31.
//

#include <cstdio>
#include <dirent.h>
#include <unistd.h>
#include <jni.h>
#include <cstdlib>
#include <array>
#include <thread>
#include <vector>
#include <string>

#include <android_build.h>
#include <logging.h>
#include <linux/limits.h>
#include <JNIHelper.h>
#include "art/runtime/native/native_util.h"
#include "config_manager.h"

using namespace std;
using namespace art;

namespace edxp {

    std::string ConfigManager::RetrieveInstallerPkgName() const {
        std::string data_test_path = data_path_prefix_ + kPrimaryInstallerPkgName;
        if (access(data_test_path.c_str(), F_OK) == 0) {
            LOGI("using installer %s", kPrimaryInstallerPkgName);
            return kPrimaryInstallerPkgName;
        }
        data_test_path = data_path_prefix_ + kLegacyInstallerPkgName;
        if (access(data_test_path.c_str(), F_OK) == 0) {
            LOGI("using installer %s", kLegacyInstallerPkgName);
            return kLegacyInstallerPkgName;
        }
        LOGE("no supported installer app found, using default: %s", kPrimaryInstallerPkgName);
        return kPrimaryInstallerPkgName;
    }

    void ConfigManager::SnapshotBlackWhiteList() {
        DIR *dir;
        struct dirent *dent;
        dir = opendir(whitelist_path_.c_str());
        if (dir != nullptr) {
            while ((dent = readdir(dir)) != nullptr) {
                if (dent->d_type == DT_REG) {
                    const char *fileName = dent->d_name;
                    LOGI("  whitelist: %s", fileName);
                    white_list_default_.emplace_back(fileName);
                }
            }
            closedir(dir);
        }
        dir = opendir(blacklist_path_.c_str());
        if (dir != nullptr) {
            while ((dent = readdir(dir)) != nullptr) {
                if (dent->d_type == DT_REG) {
                    const char *fileName = dent->d_name;
                    LOGI("  blacklist: %s", fileName);
                    black_list_default_.emplace_back(fileName);
                }
            }
            closedir(dir);
        }
    }

    void ConfigManager::UpdateConfigPath(const uid_t user) {
        if (last_user_ != user) {
            LOGI("updating config data paths from %u to %u...", last_user_, user);
            last_user_ = user;
        }

        const char *format = use_prot_storage_ ? "/data/user_de/%u/" : "/data/user/%u/";
        char buff[PATH_MAX];
        snprintf(buff, sizeof(buff), format, last_user_);
        data_path_prefix_ = buff;

        installer_pkg_name_ = RetrieveInstallerPkgName();
        base_config_path_ = GetConfigPath("");
        blacklist_path_ = GetConfigPath("blacklist/");
        whitelist_path_ = GetConfigPath("whitelist/");
        use_whitelist_path_ = GetConfigPath("usewhitelist");

        dynamic_modules_enabled_ = access(GetConfigPath("dynamicmodules").c_str(), F_OK) == 0;
        black_white_list_enabled_ = access(GetConfigPath("blackwhitelist").c_str(), F_OK) == 0;
        deopt_boot_image_enabled_ = access(GetConfigPath("deoptbootimage").c_str(), F_OK) == 0;
        resources_hook_enabled_ = access(GetConfigPath("disable_resources").c_str(), F_OK) != 0;
        no_module_log_enabled_ = access(GetConfigPath("disable_modules_log").c_str(), F_OK) == 0;

        // use_white_list snapshot
        use_white_list_snapshot_ = access(use_whitelist_path_.c_str(), F_OK) == 0;
        LOGI("data path prefix: %s", data_path_prefix_.c_str());
        LOGI("  application list mode: %s", BoolToString(black_white_list_enabled_));
        LOGI("    using whitelist: %s", BoolToString(use_white_list_snapshot_));
        LOGI("  dynamic modules mode: %s", BoolToString(dynamic_modules_enabled_));
        LOGI("  resources hook: %s", BoolToString(resources_hook_enabled_));
        LOGI("  deopt boot image: %s", BoolToString(deopt_boot_image_enabled_));
        LOGI("  no module log: %s", BoolToString(no_module_log_enabled_));
        if (black_white_list_enabled_) {
            SnapshotBlackWhiteList();
        }
    }

    bool ConfigManager::IsAppNeedHook(const std::string &app_data_dir) {
        // zygote always starts with `uid == 0` and then fork into different user.
        // so we have to check if we are the correct user or not.
        uid_t user = 0;
        char package_name[PATH_MAX];
        if (sscanf(app_data_dir.c_str(), "/data/%*[^/]/%u/%s", &user, package_name) != 2) {
            if (sscanf(app_data_dir.c_str(), "/data/%*[^/]/%s", package_name) != 1) {
                package_name[0] = '\0';
                LOGE("can't parse %s", app_data_dir.c_str());
                return false; // default to no hooking for safety
            }
        }

        if (last_user_ != user) {
            UpdateConfigPath(user);
        }

        if (!black_white_list_enabled_) {
            return true;
        }
        bool can_access_app_data = access(base_config_path_.c_str(), F_OK) == 0;
        bool use_white_list;
        if (can_access_app_data) {
            use_white_list = access(use_whitelist_path_.c_str(), F_OK) == 0;
        } else {
            LOGE("can't access config path, using snapshot use_white_list: %s",
                 app_data_dir.c_str());
            use_white_list = use_white_list_snapshot_;
        }
        if (strcmp(package_name, kPrimaryInstallerPkgName) == 0
            || strcmp(package_name, kLegacyInstallerPkgName) == 0) {
            // always hook installer apps
            return true;
        }
        if (use_white_list) {
            if (!can_access_app_data) {
                LOGE("can't access config path, using snapshot white list: %s",
                     app_data_dir.c_str());
                return !(find(white_list_default_.begin(), white_list_default_.end(),
                              package_name) ==
                         white_list_default_.end());
            }
            std::string target_path = whitelist_path_ + package_name;
            bool res = access(target_path.c_str(), F_OK) == 0;
            LOGD("using whitelist, %s -> %d", app_data_dir.c_str(), res);
            return res;
        } else {
            if (!can_access_app_data) {
                LOGE("can't access config path, using snapshot black list: %s",
                     app_data_dir.c_str());
                return find(black_list_default_.begin(), black_list_default_.end(), package_name) ==
                       black_list_default_.end();
            }
            std::string target_path = blacklist_path_ + package_name;
            bool res = access(target_path.c_str(), F_OK) != 0;
            LOGD("using blacklist, %s -> %d", app_data_dir.c_str(), res);
            return res;
        }
    }

    ALWAYS_INLINE bool ConfigManager::IsBlackWhiteListEnabled() const {
        return black_white_list_enabled_;
    }

    ALWAYS_INLINE bool ConfigManager::IsDynamicModulesEnabled() const {
        return dynamic_modules_enabled_;
    }

    ALWAYS_INLINE bool ConfigManager::IsNoModuleLogEnabled() const {
        return no_module_log_enabled_;
    }

    ALWAYS_INLINE bool ConfigManager::IsResourcesHookEnabled() const {
        return resources_hook_enabled_;
    }

    ALWAYS_INLINE bool ConfigManager::IsDeoptBootImageEnabled() const {
        return deopt_boot_image_enabled_;
    }

    ALWAYS_INLINE std::string ConfigManager::GetInstallerPackageName() const {
        return installer_pkg_name_;
    }

    ALWAYS_INLINE std::string ConfigManager::GetDataPathPrefix() const {
        return data_path_prefix_;
    }

    ALWAYS_INLINE std::string ConfigManager::GetConfigPath(const std::string &suffix) const {
        return data_path_prefix_ + installer_pkg_name_ + "/conf/" + suffix;
    };

    ConfigManager::ConfigManager() {
        use_prot_storage_ = GetAndroidApiLevel() >= ANDROID_N;
        last_user_ = 0;
        UpdateConfigPath(last_user_);
    }

}