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

#ifndef LSPOSED_SYMBOL_CACHE_H
#define LSPOSED_SYMBOL_CACHE_H

#include <memory>
namespace SandHook {
    class ElfImg;
}

namespace lspd {
    extern bool sym_initialized;
    extern std::unique_ptr<SandHook::ElfImg> art_img;
    extern void *sym_do_dlopen;
    extern void *sym_openInMemoryDexFilesNative;
    extern void *sym_createCookieWithArray;
    extern void *sym_createCookieWithDirectBuffer;
    extern void *sym_openDexFileNative;
    extern void *sym_setTrusted;
    extern void *sym_set_table_override;

    void InitSymbolCache();
}

#endif //LSPOSED_SYMBOL_CACHE_H
