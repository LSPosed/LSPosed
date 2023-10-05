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
#include "elf_util.h"
#include <dobby.h>
#include "macros.h"
#include "config.h"
#include <vector>
#include <logging.h>

namespace lspd {
    std::unique_ptr<SymbolCache> symbol_cache = std::make_unique<SymbolCache>();

    std::unique_ptr<const SandHook::ElfImg> &GetArt(bool release) {
        static std::unique_ptr<const SandHook::ElfImg> kArtImg = nullptr;
        if (release) {
            kArtImg.reset();
        } else if (!kArtImg) {
            kArtImg = std::make_unique<SandHook::ElfImg>(kLibArtName);
        }
        return kArtImg;
    }


    void InitSymbolCache(SymbolCache *other) {
        LOGD("InitSymbolCache");
        if (other && other->initialized.test(std::memory_order_acquire)) {
            LOGD("Already initialized");
            *symbol_cache = *other;
            symbol_cache->initialized.test_and_set(std::memory_order_relaxed);
            return;
        }
        symbol_cache->do_dlopen = SandHook::ElfImg("/linker").getSymbAddress(
                "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        symbol_cache->initialized.test_and_set(std::memory_order_relaxed);
        if (other) {
            *other = *symbol_cache;
            other->initialized.test_and_set(std::memory_order_acq_rel);
        }
    }
}  // namespace lspd
