
#pragma once

#include "jni.h"
#include "base/object.h"

namespace art {

    class JNIEnvExt : edxp::HookedObject {

    private:
        CREATE_MEM_FUNC_SYMBOL_ENTRY(jobject, NewLocalRef, void *thiz, void *mirror_ptr) {
            return NewLocalRefSym(thiz, mirror_ptr);
        }

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, DeleteLocalRef, void *thiz, jobject obj) {
            return DeleteLocalRefSym(thiz, obj);
        }

    public:
        JNIEnvExt(void *thiz) : HookedObject(thiz) {}

        // @ApiSensitive(Level.MIDDLE)
        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_MEM_FUNC_SYMBOL(NewLocalRef, "_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE");
            RETRIEVE_MEM_FUNC_SYMBOL(DeleteLocalRef, "_ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject");
        }

        jobject NewLocalRefer(void *mirror_ptr) {
            return NewLocalRef(thiz_, mirror_ptr);
        }

        void DeleteLocalRef(jobject obj) {
            DeleteLocalRef(thiz_, obj);
        }
    };


}