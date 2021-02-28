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
 * Copyright (C) 2021 LSPosed Contributors
 */

#include <sandhook.h>
#include "sandhook.h"
#include "native_util.h"
#include "nativehelper/jni_macros.h"

namespace lspd {
    LSP_DEF_NATIVE_METHOD(bool, SandHook, init, jclass classSandHook, jclass classNeverCall) {
        return JNI_Load_Ex(env, classSandHook, classNeverCall);
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(SandHook, init,
                              "(Ljava/lang/Class;Ljava/lang/Class;)Z"),
    };

    void RegisterSandHook(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(SandHook);
    }
}