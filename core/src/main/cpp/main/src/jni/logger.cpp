/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#include "logger.h"
#include "native_util.h"
#include "jni_helper.h"
#include <fcntl.h>

namespace lspd {
    LSP_DEF_NATIVE_METHOD(void, ModuleLogger, nativeLog, int fd, jstring jstr) {
        if (UNLIKELY(fd < 0)) {
            LOGE("fd is -1");
            return;
        }
        JUTFString str(env, jstr);
        int res = write(fd, str.get(), std::strlen(str.get()));
        if (res < 0) {
            LOGD("Logger fail: %s", strerror(errno));
        }
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ModuleLogger, nativeLog, "(ILjava/lang/String;)V")
    };

    void RegisterLogger(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ModuleLogger);
    }
}
