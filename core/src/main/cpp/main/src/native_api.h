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
// Created by kotori on 2/4/21.
//

#ifndef LSPOSED_NATIVE_API_H
#define LSPOSED_NATIVE_API_H

#include <cstdint>
#include <string>
#include <base/object.h>

// typedef int (*HookFunType)(void *, void *, void **);  // For portability
typedef void (*LsposedNativeOnModuleLoaded) (const char* name, void* handle);
typedef void (*NativeInit)(void * init_func);
struct LsposedNativeAPIEntriesV1 {
    uint32_t version;
    lspd::HookFunType inlineHookFunc;
};

namespace lspd {
    void InstallNativeAPI();
    void RegisterNativeLib(const std::string& library_name);
}

#endif //LSPOSED_NATIVE_API_H
