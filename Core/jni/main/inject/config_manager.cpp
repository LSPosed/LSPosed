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
#include <include/logging.h>
#include <sys/system_properties.h>
#include "config_manager.h"

#define INSTALLER_PACKAGE_NAME "org.meowcat.edxposed.manager"

static char package_name[256];
static bool global_mode = false;
static bool dynamic_modules = false;
static bool inited = false;
static char sdk[PROP_VALUE_MAX + 1];
static bool use_protected_storage =
        __system_property_get("ro.build.version.sdk", sdk) > 0 && atoi(sdk) >= 24;
static const char *data_dir = use_protected_storage ?
                              "/data/user_de/0/" INSTALLER_PACKAGE_NAME "/" :
                              "/data/user/0/" INSTALLER_PACKAGE_NAME "/";

const char *get_black_list_path() {
    char *result = new char[256];
    return strcat(strcpy(result, data_dir), "conf/blacklist/");
}

const char *get_white_list_path() {
    char *result = new char[256];
    return strcat(strcpy(result, data_dir), "conf/whitelist/");
}

const char *get_use_white_list_file() {
    char *result = new char[256];
    return strcat(strcpy(result, data_dir), "conf/usewhitelist");
}

const char *get_force_global_file() {
    char *result = new char[256];
    return strcat(strcpy(result, data_dir), "conf/forceglobal");
}

const char *get_dynamic_modules_file() {
    char *result = new char[256];
    return strcat(strcpy(result, data_dir), "conf/dynamicmodules");
}

void init_once() {
    if (!inited) {
        global_mode = access(get_force_global_file(), F_OK) == 0;
        dynamic_modules = access(get_dynamic_modules_file(), F_OK) == 0;
        inited = true;
    }
}

// default is true
int is_app_need_hook(JNIEnv *env, jstring appDataDir) {
    if (is_global_mode()) {
        return 1;
    }
    if (!appDataDir) {
        LOGW("appDataDir is null");
        return 1;
    }
    const char *app_data_dir = env->GetStringUTFChars(appDataDir, nullptr);
    int user = 0;
    if (sscanf(app_data_dir, "/data/%*[^/]/%d/%s", &user, package_name) != 2) {
        if (sscanf(app_data_dir, "/data/%*[^/]/%s", package_name) != 1) {
            package_name[0] = '\0';
            LOGW("can't parse %s", app_data_dir);
            return 1;
        }
    }
    env->ReleaseStringUTFChars(appDataDir, app_data_dir);
    const char *white_list_path = get_white_list_path();
    const char *black_list_path = get_black_list_path();
    bool use_white_list = access(get_use_white_list_file(), F_OK) == 0;
    bool white_list_exists = access(white_list_path, F_OK) == 0;
    bool black_list_exists = access(black_list_path, F_OK) == 0;
    if (use_white_list && white_list_exists) {
        char path[PATH_MAX];
        LOGE("package_name: %s", package_name);
        snprintf(path, PATH_MAX, "%s%s", white_list_path, package_name);
        int res = access(path, F_OK) == 0;
        LOGD("use whitelist, res=%d", res);
        return res;
    } else if (!use_white_list && black_list_exists) {
        char path[PATH_MAX];
        snprintf(path, PATH_MAX, "%s%s", black_list_path, package_name);
        int res = access(path, F_OK) != 0;
        LOGD("use blacklist, res=%d", res);
        return res;
    } else {
        LOGD("use nonlist, res=%d", 1);
        return 1;
    }
}

bool is_global_mode() {
    init_once();
    return global_mode;
}

bool is_dynamic_modules() {
    init_once();
    return dynamic_modules;
}
