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

//
// Created by Kotori0 on 2022/4/14.
//

#ifndef LSPOSED_CONFIGBRIDGE_H
#define LSPOSED_CONFIGBRIDGE_H
#include <map>

using obfuscation_map_t = std::map<std::string, std::string>;
class ConfigBridge {
public:
    inline static ConfigBridge *GetInstance() {
        return instance_.get();
    }

    inline static std::unique_ptr<ConfigBridge> ReleaseInstance() {
        return std::move(instance_);
    }

    virtual obfuscation_map_t& obfuscation_map() = 0;
    virtual void obfuscation_map(obfuscation_map_t) = 0;

    virtual ~ConfigBridge() = default;
protected:
    inline static std::unique_ptr<ConfigBridge> instance_ = nullptr;
};

#endif //LSPOSED_CONFIGBRIDGE_H
