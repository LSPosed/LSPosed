
#pragma once

#include <base/object.h>

namespace art {

    class Thread : public edxp::HookedObject {

        CREATE_MEM_FUNC_SYMBOL_ENTRY(edxp::ObjPtr, DecodeJObject, void *thiz, jobject obj) {
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

        static void Setup(void *handle, [[maybe_unused]] HookFunType hook_func) {
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
