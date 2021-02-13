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
#include <resource_hook.h>
#include "native_util.h"
#include "nativehelper/jni_macros.h"
#include "resources_hook.h"

namespace lspd {

    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, initXResourcesNative) {
        return XposedBridge_initXResourcesNative(env, clazz);
    }

    // @ApiSensitive(Level.MIDDLE)
    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, removeFinalFlagNative, jclass target_class) {
        if (target_class) {
            jclass class_clazz = JNI_FindClass(env, "java/lang/Class");
            jfieldID java_lang_Class_accessFlags = JNI_GetFieldID(
                    env, class_clazz, "accessFlags", "I");
            jint access_flags = env->GetIntField(target_class, java_lang_Class_accessFlags);
            env->SetIntField(target_class, java_lang_Class_accessFlags, access_flags & ~kAccFinal);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ResourcesHook, initXResourcesNative, "()Z"),
            LSP_NATIVE_METHOD(ResourcesHook, removeFinalFlagNative, "(Ljava/lang/Class;)Z"),
    };

    void RegisterEdxpResourcesHook(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ResourcesHook);
    }

}