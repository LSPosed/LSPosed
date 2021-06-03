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

#include <base/object.h>

namespace art {

    class Thread : public lspd::HookedObject {

        CREATE_MEM_FUNC_SYMBOL_ENTRY(lspd::ObjPtr, DecodeJObject, void *thiz, jobject obj) {
            if (DecodeJObjectSym)
                return DecodeJObjectSym(thiz, obj);
            else
                return {.data=nullptr};
        }

        CREATE_FUNC_SYMBOL_ENTRY(void *, CurrentFromGdb) {
            if (LIKELY(CurrentFromGdbSym))
                return CurrentFromGdbSym();
            else
                return nullptr;
        }

    public:
        Thread(void *thiz) : HookedObject(thiz) {}

        static Thread Current() {
            return Thread(CurrentFromGdb());
        }

        static void Setup(const SandHook::ElfImg &handle) {
            RETRIEVE_MEM_FUNC_SYMBOL(DecodeJObject,
                                     "_ZNK3art6Thread13DecodeJObjectEP8_jobject");
            RETRIEVE_FUNC_SYMBOL(CurrentFromGdb,
                                 "_ZN3art6Thread14CurrentFromGdbEv");
        }

        void *DecodeJObject(jobject obj) {
            if (LIKELY(thiz_ && DecodeJObjectSym)) {
                return DecodeJObject(thiz_, obj).data;
            }
            return nullptr;
        }
    };
}
