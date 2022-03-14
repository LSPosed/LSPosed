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

#include <jni.h>
#include <native_util.h>
#include <art/runtime/class_linker.h>
#include <vector>
#include <HookMain.h>
#include <unordered_set>
#include "art_class_linker.h"

namespace lspd {
    LSP_DEF_NATIVE_METHOD(void, ClassLinker, setEntryPointsToInterpreter, jobject method) {
        void *reflected_method = yahfa::getArtMethod(env, method);
        LOGD("deoptimizing method: %p", reflected_method);
        art::ClassLinker::Current()->SetEntryPointsToInterpreter(reflected_method);
        LOGD("method deoptimized: %p", reflected_method);
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ClassLinker, setEntryPointsToInterpreter,
                              "(Ljava/lang/reflect/Executable;)V")
    };

    void RegisterArtClassLinker(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ClassLinker);
    }

}  // namespace lspd
