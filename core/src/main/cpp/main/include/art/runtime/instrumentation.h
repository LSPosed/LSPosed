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

#ifndef LSPOSED_INSTRUMENTATION_H
#define LSPOSED_INSTRUMENTATION_H

#include "base/object.h"

namespace art {
    namespace instrumentation {

        CREATE_MEM_HOOK_STUB_ENTRIES(
                "_ZN3art15instrumentation15Instrumentation21UpdateMethodsCodeImplEPNS_9ArtMethodEPKv",
                void, UpdateMethodsCode, (void * thiz, void * art_method, const void *quick_code), {
                    if (UNLIKELY(lspd::isHooked(art_method))) {
                        LOGD("Skip update method code for hooked method %s",
                             art_method::PrettyMethod(art_method).c_str());
                        return;
                    } else {
                        backup(thiz, art_method, quick_code);
                    }
                });

        static void DisableUpdateHookedMethodsCode(const SandHook::ElfImg &handle) {
            lspd::HookSym(handle, UpdateMethodsCode);
        }
    }
}
#endif //LSPOSED_INSTRUMENTATION_H
