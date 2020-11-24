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
#include <climits>
#include <fstream>
#include <sstream>
#include "art/runtime/native/native_util.h"
#include "config_manager.h"

namespace edxp {
    namespace fs = std::filesystem;

    std::string ConfigManager::RetrieveInstallerPkgName() const {
        std::string data_test_path = data_path_prefix_ / kPrimaryInstallerPkgName;
        if (path_exists(data_test_path)) {
            LOGI("using installer %s", kPrimaryInstallerPkgName.c_str());
            return kPrimaryInstallerPkgName;
        }
        data_test_path = data_path_prefix_ / kLegacyInstallerPkgName;
        if (path_exists(data_test_path)) {
            LOGI("using installer %s", kLegacyInstallerPkgName.c_str());
            return kLegacyInstallerPkgName;
        }
        LOGE("no supported installer app found, using default: %s",
             kPrimaryInstallerPkgName.c_str());
        return kPrimaryInstallerPkgName;
    }

    void ConfigManager::SnapshotBlackWhiteList() {
        white_list_default_.clear();
        black_list_default_.clear();
        try {
            for (auto &item: fs::directory_iterator(whitelist_path_)) {
                if (item.is_regular_file()) {
                    const auto &file_name = item.path().filename();
                    LOGI("  whitelist: %s", file_name.c_str());
                    white_list_default_.emplace(file_name);
                }
            }
            for (auto &item: fs::directory_iterator(blacklist_path_)) {
                if (item.is_regular_file()) {
                    const auto &file_name = item.path().filename();
                    LOGI("  blacklist: %s", file_name.c_str());
                    black_list_default_.emplace(file_name);
                }
            }
        } catch (const fs::filesystem_error &e) {
            LOGE("%s", e.what());
        }
    }

    void ConfigManager::UpdateConfigPath(const uid_t user) {
        if (LIKELY(last_user_ == user && instance_)) return;

        LOGI("updating config data paths from %u to %u...", last_user_, user);
        last_user_ = user;

        data_path_prefix_ = use_prot_storage_ ? "/data/user_de" : "/data/user";
        data_path_prefix_ /= std::to_string(last_user_);

        installer_pkg_name_ = RetrieveInstallerPkgName();
        base_config_path_ = GetConfigPath("");
        blacklist_path_ = GetConfigPath("blacklist/");
        whitelist_path_ = GetConfigPath("whitelist/");
        use_whitelist_path_ = GetConfigPath("usewhitelist");

        dynamic_modules_enabled_ = path_exists(GetConfigPath("dynamicmodules"));
        black_white_list_enabled_ = path_exists(GetConfigPath("blackwhitelist"));
        deopt_boot_image_enabled_ = path_exists(GetConfigPath("deoptbootimage"));
        resources_hook_enabled_ = path_exists(GetConfigPath("enable_resources"));
        no_module_log_enabled_ = path_exists(GetConfigPath("disable_modules_log"));
        hidden_api_bypass_enabled_ =
                !path_exists(GetConfigPath("disable_hidden_api_bypass"));
        modules_list_.clear();
        app_modules_list_.clear();

        UpdateModuleList();

        // use_white_list snapshot
        use_white_list_snapshot_ = path_exists(use_whitelist_path_);
        LOGI("data path prefix: %s", data_path_prefix_.c_str());
        LOGI("  application list mode: %s", BoolToString(black_white_list_enabled_));
        LOGI("    using whitelist: %s", BoolToString(use_white_list_snapshot_));
        LOGI("  dynamic modules mode: %s", BoolToString(dynamic_modules_enabled_));
        LOGI("  resources hook: %s", BoolToString(resources_hook_enabled_));
        LOGI("  deopt boot image: %s", BoolToString(deopt_boot_image_enabled_));
        LOGI("  no module log: %s", BoolToString(no_module_log_enabled_));
        LOGI("  hidden api bypass: %s", BoolToString(hidden_api_bypass_enabled_));
        if (black_white_list_enabled_) {
            SnapshotBlackWhiteList();
        }
    }

    std::string ConfigManager::GetPackageNameFromBaseApkPath(const fs::path &path) {
        std::vector<std::string> paths(path.begin(), path.end());
        auto base_apk = paths.back(); // base.apk
        if (base_apk != "base.apk") return {};
        paths.pop_back();
        auto pkg_name_with_obfuscation = paths.back();
        if (auto pos = pkg_name_with_obfuscation.find('-'); pos != std::string::npos) {
            return pkg_name_with_obfuscation.substr(0, pos);
        }
        return {};
    }

    // TODO ignore unrelated processes
    bool ConfigManager::IsAppNeedHook(const uid_t user, const std::string &package_name) {
        // zygote always starts with `uid == 0` and then fork into different user.
        // so we have to check if we are the correct user or not.
        UpdateConfigPath(user);

        if (!black_white_list_enabled_) {
            return true;
        }
        bool can_access_app_data = path_exists(base_config_path_);
        bool use_white_list;
        if (can_access_app_data) {
            use_white_list = path_exists(use_whitelist_path_);
        } else {
            LOGE("can't access config path, using snapshot use_white_list: %s",
                 package_name.c_str());
            use_white_list = use_white_list_snapshot_;
        }
        if (package_name == kPrimaryInstallerPkgName
            || package_name == kLegacyInstallerPkgName) {
            // always hook installer apps
            return true;
        }
        if (use_white_list) {
            if (!can_access_app_data) {
                LOGE("can't access config path, using snapshot white list: %s",
                     package_name.c_str());
                return white_list_default_.count(package_name);
            }
            std::string target_path = whitelist_path_ / package_name;
            bool res = path_exists(target_path);
            LOGD("using whitelist, %s -> %d", package_name.c_str(), res);
            return res;
        } else {
            if (!can_access_app_data) {
                LOGE("can't access config path, using snapshot black list: %s",
                     package_name.c_str());
                return black_list_default_.count(package_name);
            }
            std::string target_path = blacklist_path_ / package_name;
            bool res = !path_exists(target_path);
            LOGD("using blacklist, %s -> %d", package_name.c_str(), res);
            return res;
        }
    }

    ConfigManager::ConfigManager() {
        use_prot_storage_ = GetAndroidApiLevel() >= __ANDROID_API_N__;
        last_user_ = 0;
        UpdateConfigPath(last_user_);
    }

    bool ConfigManager::UpdateModuleList() {
        if (LIKELY(!modules_list_.empty()) && !IsDynamicModulesEnabled())
            return true;
        modules_list_.clear();
        auto global_modules_list = GetConfigPath("modules.list");
        if (!path_exists(global_modules_list)) {
            LOGE("Cannot access path %s", global_modules_list.c_str());
            return false;
        }

        if (auto last_write_time = fs::last_write_time(global_modules_list);
                LIKELY(last_write_time < last_write_time_)) {
            return true;
        } else {
            last_write_time_ = last_write_time;
        }

        std::ifstream ifs(global_modules_list);
        if (!ifs.good()) {
            LOGE("Cannot access path %s", global_modules_list.c_str());
            return false;
        }
        std::string module;
        while (std::getline(ifs, module)) {
            const auto &module_pkg_name = GetPackageNameFromBaseApkPath(module);
            modules_list_.emplace_back(std::move(module), std::unordered_set<std::string>{});
            const auto &module_scope_conf = GetConfigPath(module_pkg_name + ".conf");
            if (!path_exists(module_scope_conf)) {
                LOGD("module scope is not set for %s", module_pkg_name.c_str());
                continue;
            }
            std::ifstream ifs_c(module_scope_conf);
            if (!ifs_c.good()) {
                LOGE("Cannot access path %s", module_scope_conf.c_str());
                continue;
            }
            auto &scope = modules_list_.back().second;
            std::string app_pkg_name;
            while (std::getline(ifs_c, app_pkg_name)) {
                if (!app_pkg_name.empty())
                    scope.emplace(std::move(app_pkg_name));
            }
            scope.insert(module_pkg_name); // Always add module itself
            LOGD("scope of %s is:\n%s", module_pkg_name.c_str(), ([&scope]() {
                std::ostringstream join;
                std::copy(scope.begin(), scope.end(),
                          std::ostream_iterator<std::string>(join, "\n"));
                return join.str();
            })().c_str());
        }
        return true;
    }

    bool ConfigManager::UpdateAppModuleList(const uid_t user, const std::string &pkg_name) {
        UpdateConfigPath(user);
        app_modules_list_.clear();
        for (const auto&[module, scope]: modules_list_) {
            if (scope.empty() || scope.count(pkg_name)) app_modules_list_.push_back(module);
        }
        return !app_modules_list_.empty() || pkg_name == installer_pkg_name_;
    }

}