//
// Created by loves on 1/28/2021.
//

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
