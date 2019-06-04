
#pragma once

#include "jni.h"
#include "base/object.h"

namespace art {

    class JNIEnvExt : edxp::HookedObject {

    private:
        CREATE_FUNC_SYMBOL_ENTRY(jobject, NewLocalRef, void *env, void *mirror_ptr) {
            return NewLocalRefSym(env, mirror_ptr);
        }

    public:
        JNIEnvExt(void *thiz) : HookedObject(thiz) {}

        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_FUNC_SYMBOL(NewLocalRef, "_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE");
        }

        jobject NewLocalRefer(void *mirror_ptr) {
            return NewLocalRef(thiz_, mirror_ptr);
        }
    };


}