
#pragma once

#include "jni.h"
#include "base/object.h"

namespace art {

    class JNIEnvExt : edxp::HookedObject {

    private:
        CREATE_FUNC_SYMBOL_ENTRY(jobject, NewLocalRef, void *env, void *mirror_ptr) {
            return NewLocalRefSym(env, mirror_ptr);
        }

        CREATE_FUNC_SYMBOL_ENTRY(void, DeleteLocalRef, void *env, jobject obj) {
            DeleteLocalRefSym(env, obj);
        }

    public:
        JNIEnvExt(void *thiz) : HookedObject(thiz) {}

        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_FUNC_SYMBOL(NewLocalRef, "_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE");
            RETRIEVE_FUNC_SYMBOL(DeleteLocalRef, "_ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject");
        }

        jobject NewLocalRefer(void *mirror_ptr) {
            return NewLocalRef(thiz_, mirror_ptr);
        }

        void DeleteLocalRef(jobject obj) {
            DeleteLocalRef(thiz_, obj);
        }
    };


}