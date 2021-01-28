//
// Created by 双草酸酯 on 12/19/20.
//

#ifndef EDXPOSED_ART_METHOD_H
#define EDXPOSED_ART_METHOD_H

#include "jni/edxp_pending_hooks.h"
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

        static void Setup(void *handle, HookFunType hook_func) {
            LOGD("art_method hook setup, handle=%p", handle);
            RETRIEVE_MEM_FUNC_SYMBOL(PrettyMethod, "_ZN3art9ArtMethod12PrettyMethodEb");
        }
    }
}

#endif //EDXPOSED_ART_METHOD_H
