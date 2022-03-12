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
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma once

#include <jni_helper.h>
#include <base/object.h>
#include "jni/yahfa.h"

namespace art {
    namespace jit {
        namespace jit_code_cache {
            CREATE_MEM_FUNC_SYMBOL_ENTRY(void, MoveObsoleteMethod, void *thiz,
                                         void *old_method, void *new_method) {
                if (MoveObsoleteMethodSym)
                    [[likely]] MoveObsoleteMethodSym(thiz, old_method, new_method);
            }

            CREATE_MEM_HOOK_STUB_ENTRIES(
                    "_ZN3art3jit12JitCodeCache19GarbageCollectCacheEPNS_6ThreadE",
                    void, GarbageCollectCache, (void * thiz, void * self), {
                        LOGD("Before jit cache gc, moving hooked methods");
                        for (auto[target, backup] : lspd::getJitMovements()) {
                            MoveObsoleteMethod(thiz, target, backup);
                        }
                        backup(thiz, self);
                    });

            inline void Setup(const SandHook::ElfImg &handle) {
                RETRIEVE_MEM_FUNC_SYMBOL(MoveObsoleteMethod,
                                         "_ZN3art3jit12JitCodeCache18MoveObsoleteMethodEPNS_9ArtMethodES3_");
                lspd::HookSyms(handle, GarbageCollectCache);
            }
        }
    }
}
