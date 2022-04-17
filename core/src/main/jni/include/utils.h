/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma once

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wgnu-string-literal-operator-template"

#include <string>
#include <filesystem>
#include <sys/system_properties.h>
#include <unistd.h>
#include <sys/stat.h>
#include "logging.h"

namespace lspd {
    using namespace std::literals::string_literals;

    inline int32_t GetAndroidApiLevel() {
        static int32_t api_level = []() {
            char prop_value[PROP_VALUE_MAX];
            __system_property_get("ro.build.version.sdk", prop_value);
            int base = atoi(prop_value);
            __system_property_get("ro.build.version.preview_sdk", prop_value);
            return base + atoi(prop_value);
        }();
        return api_level;
    }

    inline std::string JavaNameToSignature(std::string s) {
        std::replace(s.begin(), s.end(), '.', '/');
        return "L" + s;
    }
}

#pragma clang diagnostic pop
