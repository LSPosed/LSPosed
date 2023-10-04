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
 * Copyright (C) 2022 LSPosed Contributors
 */

#pragma once

#include "config_bridge.h"
#include "service.h"

namespace lspd {
    class ConfigImpl : public ConfigBridge {
    public:
        inline static void Init() {
            instance_ = std::make_unique<ConfigImpl>();
        }

        virtual obfuscation_map_t &obfuscation_map() override { return obfuscation_map_; }

        virtual void
        obfuscation_map(obfuscation_map_t m) override { obfuscation_map_ = std::move(m); }

    private:
        inline static std::map<std::string, std::string> obfuscation_map_;
    };
}
