
#pragma once

#include <base/object.h>

namespace art {

    class Thread : public edxp::HookedObject {
        struct ObjPtr { void *data; ObjPtr(ObjPtr const &) = delete; } ;
        CREATE_FUNC_SYMBOL_ENTRY(ObjPtr, DecodeJObject, void *thiz, jobject obj) {
            if (DecodeJObjectSym)
                return DecodeJObjectSym(thiz, obj);
            else
                return ObjPtr{nullptr};
        }

    public:
        Thread(void *thiz) : HookedObject(thiz) {}

        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_FUNC_SYMBOL(DecodeJObject,
                                 "_ZNK3art6Thread13DecodeJObjectEP8_jobject");
        }

        void *DecodeJObject(jobject obj) {
            if (thiz_ && DecodeJObjectSym) {
                return DecodeJObject(thiz_, obj).data;
            }
            return nullptr;
        }
    };
}
