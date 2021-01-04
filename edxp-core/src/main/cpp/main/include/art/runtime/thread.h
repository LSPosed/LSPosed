
#pragma once

#include <base/object.h>

namespace art {

    class Thread : public edxp::HookedObject {

#ifdef __i386__
        typedef void (*DecodeJObjectType)(void **, void *thiz, jobject obj);
        inline static void (*DecodeJObjectSym)(void **, void *thiz, jobject obj);
        static void *DecodeJObject(void *thiz, jobject obj) {
            if (LIKELY(DecodeJObjectSym)) {
                // Special call conversion
                void *ret = nullptr;
                DecodeJObjectSym(&ret, thiz, obj);
                // Stack unbalanced since we faked return value as 1st param
                __asm__("sub $0x4, %esp");
                return ret;
            } else
                return nullptr;
        }
#else
        CREATE_FUNC_SYMBOL_ENTRY(void *, DecodeJObject, void *thiz, jobject obj) {
            if (DecodeJObjectSym)
                return DecodeJObjectSym(thiz, obj);
            else
                return nullptr;
        }
#endif

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
