//
// Created by SwiftGan on 2019/4/12.
//

#ifndef SANDHOOK_NATIVE_HOOK_H
#define SANDHOOK_NATIVE_HOOK_H

#include "sandhook.h"

namespace SandHook {

    class NativeHook {
    public:
        static bool hookDex2oat(bool disableDex2oat);
    };

}

#endif //SANDHOOK_NATIVE_HOOK_H
