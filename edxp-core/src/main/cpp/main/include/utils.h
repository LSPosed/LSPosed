//
// Created by loves on 11/13/2020.
//
#pragma once

#include <string>
#include <filesystem>
#include "logging.h"

namespace edxp {
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
