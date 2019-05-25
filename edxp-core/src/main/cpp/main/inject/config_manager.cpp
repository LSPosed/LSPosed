//
// Created by Solo on 2019/1/27.
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

#include <include/android_build.h>
#include <include/logging.h>
#include <linux/limits.h>
#include "config_manager.h"

using namespace std;

#define PRIMARY_INSTALLER_PKG_NAME "com.solohsu.android.edxp.manager"
#define SECONDARY_INSTALLER_PKG_NAME "org.meowcat.edxposed.manager"
#define LEGACY_INSTALLER_PKG_NAME "de.robv.android.xposed.installer"

static bool use_prot_storage = GetAndroidApiLevel() >= ANDROID_N;
static const char *data_path_prefix = use_prot_storage ? "/data/user_de/0/" : "/data/user/0/";
static const char *config_path_tpl = "%s/%s/conf/%s";
static char data_test_path[PATH_MAX];
static char base_config_path[PATH_MAX];
static char blacklist_path[PATH_MAX];
static char whitelist_path[PATH_MAX];
static char use_whitelist_path[PATH_MAX];
static char black_white_list_path[PATH_MAX];
static char dynamic_modules_path[PATH_MAX];
static char deopt_boot_image_path[PATH_MAX];
static char resources_hook_disable_path[PATH_MAX];

static const char *installer_package_name;
static bool black_white_list_enabled = false;
static bool dynamic_modules_enabled = false;
static bool deopt_boot_image_enabled = false;
static bool resources_hook_enabled = true;
static bool inited = false;
// snapshot at boot
static bool use_white_list_snapshot = false;
static vector<string> white_list_default;
static vector<string> black_list_default;

static const char *get_installer_package_name() {
    snprintf(data_test_path, PATH_MAX, config_path_tpl, data_path_prefix,
             PRIMARY_INSTALLER_PKG_NAME, "/");
    if (access(data_test_path, F_OK) == 0) {
        LOGI("using installer "
                     PRIMARY_INSTALLER_PKG_NAME);
        return PRIMARY_INSTALLER_PKG_NAME;
    }
    snprintf(data_test_path, PATH_MAX, config_path_tpl, data_path_prefix,
             SECONDARY_INSTALLER_PKG_NAME, "/");
    if (access(data_test_path, F_OK) == 0) {
        LOGI("using installer "
                     SECONDARY_INSTALLER_PKG_NAME);
        return SECONDARY_INSTALLER_PKG_NAME;
    }
    snprintf(data_test_path, PATH_MAX, config_path_tpl, data_path_prefix,
             LEGACY_INSTALLER_PKG_NAME, "/");
    if (access(data_test_path, F_OK) == 0) {
        LOGI("using installer "
                     LEGACY_INSTALLER_PKG_NAME);
        return LEGACY_INSTALLER_PKG_NAME;
    }
    LOGE("no supported installer app found, using primary as default");
    return PRIMARY_INSTALLER_PKG_NAME;
}

static void snapshotBlackWhiteList() {
    DIR *dir;
    struct dirent *dent;
    dir = opendir(whitelist_path);
    if (dir != nullptr) {
        while ((dent = readdir(dir)) != nullptr) {
            if (dent->d_type == DT_REG) {
                const char *fileName = dent->d_name;
                LOGI("whitelist: %s", fileName);
                white_list_default.emplace_back(fileName);
            }
        }
        closedir(dir);
    }
    dir = opendir(blacklist_path);
    if (dir != nullptr) {
        while ((dent = readdir(dir)) != nullptr) {
            if (dent->d_type == DT_REG) {
                const char *fileName = dent->d_name;
                LOGI("blacklist: %s", fileName);
                black_list_default.emplace_back(fileName);
            }
        }
        closedir(dir);
    }
}

static void init_once() {
    if (!inited) {
        installer_package_name = get_installer_package_name();
        snprintf(base_config_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "");
        snprintf(blacklist_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "blacklist/");
        snprintf(whitelist_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "whitelist/");
        snprintf(use_whitelist_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "usewhitelist");
        snprintf(black_white_list_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "blackwhitelist");
        snprintf(dynamic_modules_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "dynamicmodules");
        snprintf(deopt_boot_image_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "deoptbootimage");
        snprintf(resources_hook_disable_path, PATH_MAX, config_path_tpl, data_path_prefix,
                 installer_package_name, "disable_resources");
        dynamic_modules_enabled = access(dynamic_modules_path, F_OK) == 0;
        black_white_list_enabled = access(black_white_list_path, F_OK) == 0;
        // use_white_list snapshot
        use_white_list_snapshot = access(use_whitelist_path, F_OK) == 0;
        deopt_boot_image_enabled = access(deopt_boot_image_path, F_OK) == 0;
        resources_hook_enabled = access(resources_hook_disable_path, F_OK) != 0;
        LOGI("black/white list mode: %d, using whitelist: %d", black_white_list_enabled,
             use_white_list_snapshot);
        LOGI("dynamic modules mode: %d", dynamic_modules_enabled);
        LOGI("resources hook: %d", resources_hook_enabled);
        if (black_white_list_enabled) {
            snapshotBlackWhiteList();
        }
        inited = true;
    }
}

static char package_name[256];

bool is_app_need_hook(JNIEnv *env, jclass, jstring appDataDir) {
    init_once();
    if (!black_white_list_enabled) {
        return true;
    }

    const char *app_data_dir = env->GetStringUTFChars(appDataDir, nullptr);
    bool can_access_app_data = access(base_config_path, F_OK) == 0;
    bool use_white_list;
    if (can_access_app_data) {
        use_white_list = access(use_whitelist_path, F_OK) == 0;
    } else {
        LOGE("can't access config path, using snapshot use_white_list: %s", app_data_dir);
        use_white_list = use_white_list_snapshot;
    }
    int user = 0;
    if (sscanf(app_data_dir, "/data/%*[^/]/%d/%s", &user, package_name) != 2) {
        if (sscanf(app_data_dir, "/data/%*[^/]/%s", package_name) != 1) {
            package_name[0] = '\0';
            LOGE("can't parse %s", app_data_dir);
            env->ReleaseStringUTFChars(appDataDir, app_data_dir);
            return !use_white_list;
        }
    }
    if (strcmp(package_name, "com.solohsu.android.edxp.manager") == 0
        || strcmp(package_name, "org.meowcat.edxposed.manager") == 0
        || strcmp(package_name, "de.robv.android.xposed.installer") == 0) {
        // always hook installer apps
        env->ReleaseStringUTFChars(appDataDir, app_data_dir);
        return true;
    }
    if (use_white_list) {
        if (!can_access_app_data) {
            LOGE("can't access config path, using snapshot white list: %s", app_data_dir);
            return find(white_list_default.begin(), white_list_default.end(), package_name) !=
                   white_list_default.end();
        }
        char path[PATH_MAX];
        snprintf(path, PATH_MAX, "%s%s", whitelist_path, package_name);
        bool res = access(path, F_OK) == 0;
        LOGD("using whitelist, %s -> %d", app_data_dir, res);
        env->ReleaseStringUTFChars(appDataDir, app_data_dir);
        return res;
    } else {
        if (!can_access_app_data) {
            LOGE("can't access config path, using snapshot black list: %s", app_data_dir);
            return !(find(black_list_default.begin(), black_list_default.end(), package_name) !=
                     black_list_default.end());
        }
        char path[PATH_MAX];
        snprintf(path, PATH_MAX, "%s%s", blacklist_path, package_name);
        bool res = access(path, F_OK) != 0;
        LOGD("using blacklist, %s -> %d", app_data_dir, res);
        env->ReleaseStringUTFChars(appDataDir, app_data_dir);
        return res;
    }
}

bool is_black_white_list_enabled() {
    init_once();
    return black_white_list_enabled;
}

bool is_dynamic_modules_enabled() {
    init_once();
    return dynamic_modules_enabled;
}

bool is_resources_hook_enabled() {
    init_once();
    return resources_hook_enabled;
}

bool is_deopt_boot_image_enabled() {
    init_once();
    return deopt_boot_image_enabled;
}

jstring get_installer_pkg_name(JNIEnv *env, jclass) {
    init_once();
    return env->NewStringUTF(installer_package_name);
}
