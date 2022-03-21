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
// Created by Nullptr on 2022/3/16.
//

#pragma once

#include "config.h"

namespace lspd {
    inline static constexpr auto kEntryClassName = "org.lsposed.lspd.core.Main"_tstr;
    inline static constexpr auto kBridgeServiceClassName = "org.lsposed.lspd.service.BridgeService"_tstr;

    extern const int apiVersion;
    extern const char* const moduleName;
}
