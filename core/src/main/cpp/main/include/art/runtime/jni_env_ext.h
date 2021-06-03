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

#pragma once

#include "jni.h"
#include "base/object.h"

namespace art {

    class JNIEnvExt : lspd::HookedObject {

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
        static void Setup(const SandHook::ElfImg &handle) {
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
