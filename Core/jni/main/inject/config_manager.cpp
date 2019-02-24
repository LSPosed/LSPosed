//
// Created by Solo on 2019/1/27.
//

#include <cstdio>
#include <unistd.h>
#include <jni.h>
#include <cstdlib>
#include <array>
#include <thread>
#include <vector>
#include <string>
#include <include/android_build.h>
#include <include/logging.h>
#include "config_manager.h"

#define PRIMARY_INSTALLER_PKG_NAME "com.solohsu.android.edxp.manager"
#define SECONDARY_INSTALLER_PKG_NAME "org.meowcat.edxposed.manager"
#define LEGACY_INSTALLER_PKG_NAME "de.robv.android.xposed.installer"

static bool use_prot_storage = GetAndroidApiLevel() >= ANDROID_N;
static const char *data_path_prefix = use_prot_storage ? "/data/user_de/0/" : "/data/user/0/";
static const char *config_path_tpl = "%s/%s/conf/%s";
static char data_test_path[PATH_MAX];
static char blacklist_path[PATH_MAX];
static char whitelist_path[PATH_MAX];
static char use_whitelist_path[PATH_MAX];
static char black_white_list_path[PATH_MAX];
static char dynamic_modules_path[PATH_MAX];

static const char *installer_package_name;
static bool black_white_list_enabled = false;
static bool dynamic_modules_enabled = false;
static bool inited = false;

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

static void init_once() {
    if (!inited) {
        installer_package_name = get_installer_package_name();
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
        dynamic_modules_enabled = access(dynamic_modules_path, F_OK) == 0;
        black_white_list_enabled = access(black_white_list_path, F_OK) == 0;
        LOGI("black/white list mode: %d", black_white_list_enabled);
        LOGI("dynamic modules mode: %d", dynamic_modules_enabled);
        inited = true;
    }
}

static char package_name[256];

bool is_app_need_hook(JNIEnv *env, jstring appDataDir) {
    init_once();
    if (!black_white_list_enabled) {
        return true;
    }
    bool use_white_list = access(use_whitelist_path, F_OK) == 0;
    if (!appDataDir) {
        LOGE("appDataDir is null");
        return !use_white_list;
    }
    const char *app_data_dir = env->GetStringUTFChars(appDataDir, nullptr);
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
        char path[PATH_MAX];
        snprintf(path, PATH_MAX, "%s%s", whitelist_path, package_name);
        bool res = access(path, F_OK) == 0;
        LOGD("using whitelist, %s -> %d", app_data_dir, res);
        env->ReleaseStringUTFChars(appDataDir, app_data_dir);
        return res;
    } else {
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

jstring get_installer_pkg_name(JNIEnv *env) {
    init_once();
    return env->NewStringUTF(installer_package_name);
}
