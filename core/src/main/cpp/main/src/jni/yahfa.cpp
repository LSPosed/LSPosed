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

#include "yahfa.h"
#include "HookMain.h"
#include "native_util.h"
#include "pending_hooks.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/thread_list.h"
#include "art/runtime/thread.h"
#include "art/runtime/gc/scoped_gc_critical_section.h"
#include <dex_builder.h>


namespace lspd {
    using namespace startop::dex;
    LSP_DEF_NATIVE_METHOD(void, Yahfa, init, jint sdkVersion) {
        yahfa::init(env, clazz, sdkVersion);
    }

    LSP_DEF_NATIVE_METHOD(jobject, Yahfa, findMethodNative, jclass targetClass,
                          jstring methodName, jstring methodSig) {
        return yahfa::findMethodNative(env, clazz, targetClass, methodName,
                                       methodSig);
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, backupAndHookNative, jobject target,
                          jobject hook, jobject backup) {
        art::gc::ScopedGCCriticalSection section(art::Thread::Current().Get(),
                                                 art::gc::kGcCauseDebugger,
                                                 art::gc::kCollectorTypeDebugger);
        art::thread_list::ScopedSuspendAll suspend("Yahfa Hook", false);
        return yahfa::backupAndHookNative(env, clazz, target, hook, backup);
    }

    LSP_DEF_NATIVE_METHOD(void, Yahfa, recordHooked, jobject member) {
        lspd::recordHooked(yahfa::getArtMethod(env, member));
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, isHooked, jobject member) {
        return lspd::isHooked(yahfa::getArtMethod(env, member));
    }

    LSP_DEF_NATIVE_METHOD(jclass, Yahfa, buildHooker, jobject app_class_loader, jchar return_class,
                          jcharArray classes, jstring method_name) {
        static auto in_memory_classloader = JNI_NewGlobalRef(env, JNI_FindClass(env,
                                                                                "dalvik/system/InMemoryDexClassLoader"));
        static jmethodID initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                                   "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        DexBuilder dex_file;

        auto parameter_length = env->GetArrayLength(classes);
        auto parameter_types = std::vector<TypeDescriptor>();
        parameter_types.reserve(parameter_length);
        std::string storage;
        auto return_type =
                return_class == 'L' ? TypeDescriptor::Object : TypeDescriptor::FromDescriptor(
                        (char) return_class);
        auto params = env->GetCharArrayElements(classes, nullptr);
        for (int i = 0; i < parameter_length; ++i) {
            parameter_types.push_back(
                    params[i] == 'L' ? TypeDescriptor::Object : TypeDescriptor::FromDescriptor(
                            (char) params[i]));
        }

        ClassBuilder cbuilder{dex_file.MakeClass("LspHooker_")};
        cbuilder.set_source_file("LSP");

        auto hooker_type =
                TypeDescriptor::FromClassname("de.robv.android.xposed.LspHooker");

        auto *hooker_field = cbuilder.CreateField("hooker", hooker_type)
                .access_flags(dex::kAccStatic)
                .Encode();

        auto setupBuilder{cbuilder.CreateMethod(
                "setup", Prototype{TypeDescriptor::Void, hooker_type})};
        setupBuilder
                .AddInstruction(Instruction::SetStaticObjectField(
                        hooker_field->decl->orig_index, Value::Parameter(0)))
                .BuildReturn()
                .Encode();

        auto hookBuilder{cbuilder.CreateMethod(
                JUTFString(env, method_name), Prototype{return_type, parameter_types})};
        // allocate tmp frist because of wide
        auto tmp{hookBuilder.AllocRegister()};
        hookBuilder.BuildConst(tmp, parameter_types.size());
        auto hook_params_array{hookBuilder.AllocRegister()};
        hookBuilder.BuildNewArray(hook_params_array, TypeDescriptor::Object, tmp);
        for (size_t i = 0u, j = 0u; i < parameter_types.size(); ++i, ++j) {
            hookBuilder.BuildBoxIfPrimitive(Value::Parameter(j), parameter_types[i],
                                            Value::Parameter(j));
            hookBuilder.BuildConst(tmp, i);
            hookBuilder.BuildAput(Instruction::Op::kAputObject, hook_params_array,
                                  Value::Parameter(j), tmp);
            if (parameter_types[i].is_wide()) ++j;
        }
        auto handle_hook_method{dex_file.GetOrDeclareMethod(
                hooker_type, "handleHookedMethod",
                Prototype{TypeDescriptor::Object, TypeDescriptor::Object.ToArray()})};
        hookBuilder.AddInstruction(
                Instruction::GetStaticObjectField(hooker_field->decl->orig_index, tmp));
        hookBuilder.AddInstruction(Instruction::InvokeVirtualObject(
                handle_hook_method.id, tmp, tmp, hook_params_array));
        if (return_type == TypeDescriptor::Void) {
            hookBuilder.BuildReturn();
        } else if (return_type.is_primitive()) {
            auto box_type{return_type.ToBoxType()};
            const ir::Type *type_def = dex_file.GetOrAddType(box_type);
            hookBuilder.AddInstruction(
                    Instruction::Cast(tmp, Value::Type(type_def->orig_index)));
            hookBuilder.BuildUnBoxIfPrimitive(tmp, box_type, tmp);
            hookBuilder.BuildReturn(tmp, false, return_type.is_wide());
        } else {
            const ir::Type *type_def = dex_file.GetOrAddType(return_type);
            hookBuilder.AddInstruction(
                    Instruction::Cast(tmp, Value::Type(type_def->orig_index)));
            hookBuilder.BuildReturn(tmp, true);
        }
        [[maybe_unused]] auto *hook_method = hookBuilder.Encode();

        auto backup_builder{
                cbuilder.CreateMethod("backup", Prototype{return_type, parameter_types})};
        if (return_type == TypeDescriptor::Void) {
            backup_builder.BuildReturn();
        } else if (return_type.is_wide()) {
            LiveRegister zero = backup_builder.AllocRegister();
            LiveRegister zero_wide = backup_builder.AllocRegister();
            backup_builder.BuildConstWide(zero, 0);
            backup_builder.BuildReturn(zero, /*is_object=*/false, true);
        } else {
            LiveRegister zero = backup_builder.AllocRegister();
            LiveRegister zero_wide = backup_builder.AllocRegister();
            backup_builder.BuildConst(zero, 0);
            backup_builder.BuildReturn(zero, /*is_object=*/!return_type.is_primitive(), false);
        }
        [[maybe_unused]] auto *back_method = backup_builder.Encode();

        slicer::MemView image{dex_file.CreateImage()};

        auto dex_buffer = env->NewDirectByteBuffer(const_cast<void *>(image.ptr()), image.size());
        auto my_cl = JNI_NewObject(env, in_memory_classloader, initMid,
                                   dex_buffer, app_class_loader);
        env->DeleteLocalRef(dex_buffer);

        static jmethodID mid = JNI_GetMethodID(env, in_memory_classloader, "loadClass",
                                               "(Ljava/lang/String;)Ljava/lang/Class;");
        if (!mid) {
            mid = JNI_GetMethodID(env, in_memory_classloader, "findClass",
                                  "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        auto target = JNI_CallObjectMethod(env, my_cl, mid, env->NewStringUTF("LspHooker_"));
//        LOGD("Created %zd", image.size());
        if (target) {
            return (jclass) target.release();
        }
        return nullptr;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Yahfa, init, "(I)V"),
            LSP_NATIVE_METHOD(Yahfa, findMethodNative,
                              "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/reflect/Executable;"),
            LSP_NATIVE_METHOD(Yahfa, backupAndHookNative,
                              "(Ljava/lang/reflect/Executable;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Z"),
            LSP_NATIVE_METHOD(Yahfa, recordHooked, "(Ljava/lang/reflect/Executable;)V"),
            LSP_NATIVE_METHOD(Yahfa, isHooked, "(Ljava/lang/reflect/Executable;)Z"),
            LSP_NATIVE_METHOD(Yahfa, buildHooker,
                              "(Ljava/lang/ClassLoader;C[CLjava/lang/String;)Ljava/lang/Class;"),
    };

    void RegisterYahfa(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(Yahfa);
    }

}
