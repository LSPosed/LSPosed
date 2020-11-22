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

#include <logging.h>
#include <climits>
#include <fstream>
#include <sstream>
#include "art/runtime/native/native_util.h"
#include "config_manager.h"
#include "utils.h"

namespace edxp {
    namespace fs = std::filesystem;

    std::string ConfigManager::RetrieveInstallerPkgName() const {
        std::string data_test_path = data_path_prefix_ / kPrimaryInstallerPkgName;
        if (path_exists(data_test_path, true)) {
            LOGI("using installer %s", kPrimaryInstallerPkgName.c_str());
            return kPrimaryInstallerPkgName;
        }
        data_test_path = data_path_prefix_ / kLegacyInstallerPkgName;
        if (path_exists(data_test_path, true)) {
            LOGI("using installer %s", kLegacyInstallerPkgName.c_str());
            return kLegacyInstallerPkgName;
        }
        LOGE("no supported installer app found, using default: %s",
             kPrimaryInstallerPkgName.c_str());
        return kPrimaryInstallerPkgName;
    }

    std::unordered_set<std::string> ConfigManager::GetAppList(const fs::path &dir) {
        std::unordered_set<std::string> set;
        for (auto &item: fs::directory_iterator(dir)) {
            if (item.is_regular_file()) {
                set.emplace(item.path().filename());
            }
        }
        return set;
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
    bool ConfigManager::IsAppNeedHook(const std::string &package_name) const {
        if (!black_list_enable_ && !white_list_enable_) {
            return true;
        }

        if (package_name == installer_pkg_name_) {
            return true;
        }

        if (white_list_enable_) {
            auto res = white_list_.count(package_name);
            LOGD("using whitelist, %s -> %d", package_name.c_str(), res);
            return res;
        } else {
            auto res = black_list_.count(package_name);
            LOGD("using blacklist, %s -> %d", package_name.c_str(), res);
            return res;
        }
    }

    ConfigManager::ConfigManager(uid_t user) :
            user_(user),
            data_path_prefix_(fs::path(use_prot_storage_ ? "/data/user_de" : "/data/user") /
                              std::to_string(user_)),
            installer_pkg_name_(RetrieveInstallerPkgName()),
            black_list_enable_(path_exists(GetConfigPath("blacklist"))),
            white_list_enable_(path_exists(GetConfigPath("whiltelist"))),
            deopt_boot_image_enabled_(path_exists(GetConfigPath("deoptbootimage"))),
            no_module_log_enabled_(path_exists(GetConfigPath("disable_modules_log"))),
            resources_hook_enabled_(path_exists(GetConfigPath("enable_resources"))),
            hidden_api_bypass_enabled_(!path_exists(GetConfigPath("disable_hidden_api_bypass"))),
            white_list_(white_list_enable_ ? GetAppList(GetConfigPath("whitelist/"))
                                           : std::unordered_set<std::string>{}),
            black_list_(black_list_enable_ ? GetAppList(GetConfigPath("blacklist/"))
                                           : std::unordered_set<std::string>{}),
            modules_list_(GetModuleList()),
            last_write_time_(GetLastWriteTime()){
        // use_white_list snapshot
        LOGI("data path prefix: %s", data_path_prefix_.c_str());
        LOGI("  using blacklist: %s", BoolToString(black_list_enable_));
        LOGI("  using whitelist: %s", BoolToString(white_list_enable_));
        LOGI("  resources hook: %s", BoolToString(resources_hook_enabled_));
        LOGI("  deopt boot image: %s", BoolToString(deopt_boot_image_enabled_));
        LOGI("  no module log: %s", BoolToString(no_module_log_enabled_));
        LOGI("  hidden api bypass: %s", BoolToString(hidden_api_bypass_enabled_));
    }

    auto ConfigManager::GetModuleList() -> std::remove_const_t<decltype(modules_list_)> {
        std::remove_const_t<decltype(modules_list_)> modules_list;
        auto global_modules_list = GetConfigPath("modules.list");
        if (!path_exists(global_modules_list)) {
            LOGE("Cannot access path %s", global_modules_list.c_str());
            return modules_list;
        }
        std::ifstream ifs(global_modules_list);
        if (!ifs.good()) {
            LOGE("Cannot access path %s", global_modules_list.c_str());
            return modules_list;
        }
        std::string module;
        while (std::getline(ifs, module)) {
            const auto &module_pkg_name = GetPackageNameFromBaseApkPath(module);
            modules_list.emplace_back(std::move(module), std::unordered_set<std::string>{});
            const auto &module_scope_conf = GetConfigPath(module_pkg_name + ".conf");
            if (!path_exists(module_scope_conf, true)) {
                LOGD("module scope is not set for %s", module_pkg_name.c_str());
                continue;
            }
            std::ifstream ifs_c(module_scope_conf);
            if (!ifs_c.good()) {
                LOGE("Cannot access path %s", module_scope_conf.c_str());
                continue;
            }
            auto &scope = modules_list.back().second;
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
        return modules_list;
    }

    std::vector<std::string> ConfigManager::GetAppModuleList(const std::string &pkg_name) const {
        std::vector<std::string> app_modules_list;
        for (const auto&[module, scope]: modules_list_) {
            if (scope.empty() || scope.count(pkg_name)) app_modules_list.push_back(module);
        }
        return app_modules_list;
    }

    std::filesystem::file_time_type ConfigManager::GetLastWriteTime() const {
        auto dynamic_path = GetConfigPath("dynamic");
        if (!path_exists(dynamic_path, true))
            return {};
        return fs::last_write_time(dynamic_path);
    }

}