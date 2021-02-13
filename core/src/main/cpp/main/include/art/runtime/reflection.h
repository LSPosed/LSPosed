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

#ifndef LSPOSED_REFLECTION_H
#define LSPOSED_REFLECTION_H

#include "base/object.h"

namespace art {

    CREATE_HOOK_STUB_ENTRIES(
            "_ZN3art12VerifyAccessENS_6ObjPtrINS_6mirror6ObjectEEENS0_INS1_5ClassEEEjS5_",
            bool, VerifyAccess,
            (void * obj, void * declaring_class, uint32_t access_flags, void * calling_class), {
                auto calling_desc = art::mirror::Class(calling_class).GetDescriptor();
                if (UNLIKELY(calling_desc.find("de/robv/android/xposed/LspHooker") !=
                             std::string::npos)) {
                    return true;
                }
                return backup(obj, declaring_class, access_flags, calling_class);
            });

    static void PermissiveAccessByReflection(void *handle) {
        lspd::HookSym(handle, VerifyAccess);
    }
}
#endif //LSPOSED_REFLECTION_H
