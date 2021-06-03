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
// Created by 双草酸酯 on 12/19/20.
//

#ifndef LSPOSED_ART_METHOD_H
#define LSPOSED_ART_METHOD_H

#include "jni/pending_hooks.h"
#include <HookMain.h>

namespace art {
    namespace art_method {
        CREATE_MEM_FUNC_SYMBOL_ENTRY(std::string, PrettyMethod, void *thiz, bool with_signature) {
            if (UNLIKELY(thiz == nullptr))
                return "null";
            if (LIKELY(PrettyMethodSym))
                return PrettyMethodSym(thiz, with_signature);
            else return "null sym";
        }

        inline static std::string PrettyMethod(void *thiz) {
            return PrettyMethod(thiz, true);
        }

        static void Setup(const SandHook::ElfImg &handle) {
            RETRIEVE_MEM_FUNC_SYMBOL(PrettyMethod, "_ZN3art9ArtMethod12PrettyMethodEb");
        }
    }
}

#endif //LSPOSED_ART_METHOD_H
