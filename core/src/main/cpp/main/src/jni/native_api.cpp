//
// Created by 双草酸酯 on 2/7/21.
//
#include "native_api.h"
#include "nativehelper/jni_macros.h"
#include "native_util.h"
#include "JNIHelper.h"
#include "../native_api.h"

namespace lspd {
    LSP_DEF_NATIVE_METHOD(void, NativeAPI, recordNativeEntrypoint, jstring jstr) {
        JUTFString str(env, jstr);
        RegisterNativeLib(str);
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(NativeAPI, recordNativeEntrypoint, "(Ljava/lang/String;)V")
    };

    void RegisterNativeAPI(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(NativeAPI);
    }
}
