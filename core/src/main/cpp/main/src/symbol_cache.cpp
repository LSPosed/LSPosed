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

//
// Created by kotori on 2/7/21.
//

#include "symbol_cache.h"
#include <dobby.h>
#include <art/base/macros.h>
#include <logging.h>

namespace lspd {
    void InitSymbolCache() {
        if (LIKELY(initialized)) return;
        LOGD("InitSymbolCache");
        // TODO: set image name
        symbol_do_dlopen = DobbySymbolResolver(nullptr, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        initialized = true;
    }
}