
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
            RETRIEVE_FUNC_SYMBOL(DecodeJObject,
                                 "_ZNK3art6Thread13DecodeJObjectEP8_jobject");
            RETRIEVE_FUNC_SYMBOL(CurrentFromGdb,
                                 "_ZN3art6Thread14CurrentFromGdbEv");
        }

        void *DecodeJObject(jobject obj) {
            if (LIKELY(thiz_ && DecodeJObjectSym)) {
                return DecodeJObject(thiz_, obj);
            }
            return nullptr;
        }
    };
}
