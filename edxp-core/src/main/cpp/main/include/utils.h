//
// Created by loves on 11/13/2020.
//
#pragma once

#include <string>
#include <filesystem>
#include <sys/system_properties.h>
#include <unistd.h>
#include <sys/stat.h>
#include "logging.h"

namespace edxp {
    using namespace std::literals::string_literals;

    static inline int32_t GetAndroidApiLevel() {
        char prop_value[PROP_VALUE_MAX];
        __system_property_get("ro.build.version.sdk", prop_value);
        return atoi(prop_value);
    }

    static inline std::string GetAndroidBrand() {
        char prop_value[PROP_VALUE_MAX];
        __system_property_get("ro.product.brand", prop_value);
        return prop_value;
    }

    template<bool quite = false>
    inline bool path_exists(const std::filesystem::path &path) {
        try {
            return std::filesystem::exists(path);
        } catch (const std::filesystem::filesystem_error &e) {
            if constexpr(!quite) {
                LOGE("%s", e.what());
            }
            return false;
        }
    }

    inline void
    path_chown(const std::filesystem::path &path, uid_t uid, gid_t gid, bool recursively = false) {
        if (chown(path.c_str(), uid, gid) != 0) {
            throw std::filesystem::filesystem_error(strerror(errno), path,
                                                    {errno, std::system_category()});
        }
        if (recursively) {
            for (const auto &item : std::filesystem::recursive_directory_iterator(path)) {
                if (chown(item.path().c_str(), uid, gid) != 0) {
                    throw std::filesystem::filesystem_error(strerror(errno), item.path(),
                                                            {errno, std::system_category()});
                }
            }
        }
    }

    inline std::tuple<uid_t, gid_t> path_own(const std::filesystem::path &path) {
        struct stat sb;
        stat(path.c_str(), &sb);
        return {sb.st_uid, sb.st_gid};
    }
}
