
#pragma once

#include <base/object.h>

namespace art {

    class Thread : public edxp::HookedObject {

        CREATE_FUNC_SYMBOL_ENTRY(void *, DecodeJObject, void *thiz,
                                 jobject obj) {
            if (DecodeJObjectSym)
                return DecodeJObjectSym(thiz, obj);
            else
                return nullptr;
        }

    public:
        Thread(void *thiz) : HookedObject(thiz) {}

        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_FUNC_SYMBOL(DecodeJObject,
                                 "_ZNK3art6Thread13DecodeJObjectEP8_jobject");
        }

        void *DecodeJObject(jobject obj) {
            if (thiz_ && DecodeJObjectSym) {
                return DecodeJObject(thiz_, obj);
            }
            return nullptr;
        }
    };
}
