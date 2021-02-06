//
// Created by loves on 2/6/2021.
//

#include "logger.h"
#include "nativehelper/jni_macros.h"
#include "native_util.h"
#include "JNIHelper.h"
#include "../config_manager.h"
#include <fstream>
#include <fcntl.h>

namespace lspd {
    LSP_DEF_NATIVE_METHOD(void, Logger, nativeLog, jstring jstr) {
        static int fd = open(ConfigManager::GetModulesLogPath().c_str(), O_APPEND | O_WRONLY);
        if (fd < 0) {
            LOGD("Logger fail: %s", strerror(errno));
            return;
        }
        JUTFString str(env, jstr);
        int res = write(fd, str.get(), std::strlen(str.get()));
        if (res < 0) {
            LOGD("Logger fail: %s", strerror(errno));
        }
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Logger, nativeLog, "(Ljava/lang/String;)V")
    };

    void RegisterLogger(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(Logger);
    }
}
