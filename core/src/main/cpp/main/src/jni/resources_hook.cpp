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
#include <dex_builder.h>
#include <art/runtime/thread.h>
#include <art/runtime/mirror/class.h>
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

    LSP_DEF_NATIVE_METHOD(jobject, ResourcesHook, buildDummyClassLoader, jobject parent, jobject resource_super_class, jobject typed_array_super_class) {
        using namespace startop::dex;
        static auto in_memory_classloader = (jclass)env->NewGlobalRef(env->FindClass( "dalvik/system/InMemoryDexClassLoader"));
        static jmethodID initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                                   "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        DexBuilder dex_file;

        std::string storage;
        auto current_thread = art::Thread::Current();
        ClassBuilder xresource_builder{
                dex_file.MakeClass("xposed.dummy.XResourcesSuperClass")};
        xresource_builder.setSuperClass(TypeDescriptor::FromDescriptor(art::mirror::Class(current_thread.DecodeJObject(resource_super_class)).GetDescriptor(&storage)));

        ClassBuilder xtypearray_builder{
                dex_file.MakeClass("xposed.dummy.XTypedArraySuperClass")};
        xtypearray_builder.setSuperClass(TypeDescriptor::FromDescriptor(art::mirror::Class(current_thread.DecodeJObject(typed_array_super_class)).GetDescriptor(&storage)));

        slicer::MemView image{dex_file.CreateImage()};

        auto dex_buffer = env->NewDirectByteBuffer(const_cast<void*>(image.ptr()), image.size());
        return JNI_NewObject(env, in_memory_classloader, initMid,
                                      dex_buffer, parent);
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ResourcesHook, initXResourcesNative, "()Z"),
            LSP_NATIVE_METHOD(ResourcesHook, removeFinalFlagNative, "(Ljava/lang/Class;)Z"),
            LSP_NATIVE_METHOD(ResourcesHook, buildDummyClassLoader, "(Ljava/lang/ClassLoader;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/ClassLoader;"),
    };

    void RegisterResourcesHook(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ResourcesHook);
    }

}