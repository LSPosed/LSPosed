//
// Created by loves on 1/27/2021.
//

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

        static void DisableUpdateHookedMethodsCode(void *handle) {
            lspd::HookSym(handle, UpdateMethodsCode);
        }
    }
}
#endif //LSPOSED_INSTRUMENTATION_H
