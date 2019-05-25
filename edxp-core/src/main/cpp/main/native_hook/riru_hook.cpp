//
// Created by solo on 2019/3/16.
//

#include <cstring>
#include <string>
#include <riru.h>
#include <xhook.h>
#include <sys/system_properties.h>
#include <include/logging.h>
#include <include/android_build.h>
#include "riru_hook.h"

int api_level = 0;

#define PROP_KEY_COMPILER_FILTER "dalvik.vm.dex2oat-filter"
#define PROP_KEY_COMPILER_FLAGS "dalvik.vm.dex2oat-flags"
#define PROP_KEY_USEJITPROFILES "dalvik.vm.usejitprofiles"
#define PROP_KEY_PM_BG_DEXOPT "pm.dexopt.bg-dexopt"
#define PROP_VALUE_COMPILER_FILTER "quicken"
#define PROP_VALUE_COMPILER_FLAGS "--inline-max-code-units=0"
#define PROP_VALUE_PM_BG_DEXOPT "speed"

#define XHOOK_REGISTER(NAME) \
    if (xhook_register(".*", #NAME, (void*) new_##NAME, (void **) &old_##NAME) == 0) { \
        if (riru_get_version() >= 8) { \
            void *f = riru_get_func(#NAME); \
            if (f != nullptr) { \
                memcpy(&old_##NAME, &f, sizeof(void *)); \
            } \
            riru_set_func(#NAME, (void *) new_##NAME); \
        } \
    } else { \
        LOGE("failed to register riru hook " #NAME "."); \
    }

#define NEW_FUNC_DEF(ret, func, ...) \
    static ret (*old_##func)(__VA_ARGS__); \
    static ret new_##func(__VA_ARGS__)

NEW_FUNC_DEF(int, __system_property_get, const char *key, char *value) {
    int res = old___system_property_get(key, value);
    if (key) {
        if (strcmp(PROP_KEY_COMPILER_FILTER, key) == 0) {
            strcpy(value, PROP_VALUE_COMPILER_FILTER);
            LOGI("system_property_get: %s -> %s", key, value);
        } else if (strcmp(PROP_KEY_COMPILER_FLAGS, key) == 0) {
            strcpy(value, PROP_VALUE_COMPILER_FLAGS);
            LOGI("system_property_get: %s -> %s", key, value);
        }
        if (api_level == ANDROID_O_MR1) {
            // https://android.googlesource.com/platform/art/+/f5516d38736fb97bfd0435ad03bbab17ddabbe4e
            // Android 8.1 add a fatal check for debugging (removed in Android 9.0),
            // which will be triggered by EdXposed in cases where target method is hooked
            // (native flag set) after it has been called several times(getCounter() return positive number)
            if (strcmp(PROP_KEY_USEJITPROFILES, key) == 0) {
                strcpy(value, "false");
            } else if (strcmp(PROP_KEY_PM_BG_DEXOPT, key) == 0) {
                // use speed as bg-dexopt filter since that speed-profile won't work after
                // jit profiles is disabled
                strcpy(value, PROP_VALUE_PM_BG_DEXOPT);
            }
            LOGD("system_property_get: %s -> %s", key, value);
        }
    }
    return res;
}

NEW_FUNC_DEF(std::string,
             _ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_,
             const std::string &key, const std::string &default_value) {
    std::string res = old__ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_(
            key, default_value);
    if (strcmp(PROP_KEY_COMPILER_FILTER, key.c_str()) == 0) {
        res = PROP_VALUE_COMPILER_FILTER;
        LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    } else if (strcmp(PROP_KEY_COMPILER_FLAGS, key.c_str()) == 0) {
        res = PROP_VALUE_COMPILER_FLAGS;
        LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    }
    if (api_level == ANDROID_O_MR1) {
        // see __system_property_get hook above for explanations
        if (strcmp(PROP_KEY_USEJITPROFILES, key.c_str()) == 0) {
            res = "false";
        } else if (strcmp(PROP_KEY_PM_BG_DEXOPT, key.c_str()) == 0) {
            res = PROP_VALUE_PM_BG_DEXOPT;
        }
        LOGD("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
    }
    return res;
}

void install_riru_hooks() {

    LOGI("install riru hook");

    api_level = GetAndroidApiLevel();

    XHOOK_REGISTER(__system_property_get);

    if (GetAndroidApiLevel() >= ANDROID_P) {
        XHOOK_REGISTER(
                _ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_);
    }

    if (xhook_refresh(0) == 0) {
        xhook_clear();
        LOGI("riru hooks installed");
    } else {
        LOGE("failed to install riru hooks");
    }
}