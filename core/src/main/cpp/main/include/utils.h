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

    static inline int32_t GetAndroidApiLevel() {
        char prop_value[PROP_VALUE_MAX];
        __system_property_get("ro.build.version.sdk", prop_value);
        return atoi(prop_value);
    }


    template<char... chars>
    struct tstring : public std::integer_sequence<char, chars...> {
        inline constexpr static const char *c_str() {
            return str_;
        }

        inline constexpr operator std::string_view() const {
            return c_str();
        }

    private:
        constexpr static char str_[]{chars..., '\0'};
    };

    template<typename T, T... chars>
    inline constexpr tstring<chars...> operator ""_tstr() {
        return {};
    }


    template<char... as, char... bs>
    inline constexpr tstring<as..., bs...>
    operator+(const tstring<as...> &, const tstring<bs...> &) {
        return {};
    }

    template<char... as>
    inline constexpr auto operator+(const std::string &a, const tstring<as...> &) {
        char b[]{as..., '\0'};
        return a + b;
    }

    template<char... as>
    inline constexpr auto operator+(const tstring<as...> &, const std::string &b) {
        char a[]{as..., '\0'};
        return a + b;
    }
}

#pragma clang diagnostic pop
