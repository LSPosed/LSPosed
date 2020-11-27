//
// Created by loves on 11/13/2020.
//
#pragma once

#include <string>
#include <filesystem>
#include "logging.h"
#include <sys/system_properties.h>

namespace edxp {

    static inline int32_t GetAndroidApiLevel() {
        char prop_value[PROP_VALUE_MAX];
        __system_property_get("ro.build.version.sdk", prop_value);
        return atoi(prop_value);
    }

    inline const std::string operator ""_str(const char *str, std::size_t size) {
        return {str, size};
    }

    inline bool path_exists(const std::filesystem::path &path) {
        try {
            return std::filesystem::exists(path);
        } catch (const std::filesystem::filesystem_error &e) {
            LOGE("%s", e.what());
            return false;
        }
    }
}
