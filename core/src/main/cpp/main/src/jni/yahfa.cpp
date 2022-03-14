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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#include "yahfa.h"
#include "HookMain.h"
#include "native_util.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/thread_list.h"
#include "art/runtime/thread.h"
#include "art/runtime/gc/scoped_gc_critical_section.h"
#include <dex_builder.h>
#include <shared_mutex>
#include <unordered_set>


namespace lspd {
    namespace {
        std::unordered_map<const void *, void*> hooked_methods_;
        std::shared_mutex hooked_methods_lock_;

        std::vector<std::pair<void *, void *>> jit_movements_;
        std::shared_mutex jit_movements_lock_;
    }

    void* isHooked(void *art_method) {
        std::shared_lock lk(hooked_methods_lock_);
        if (auto found = hooked_methods_.find(art_method); found != hooked_methods_.end()) {
            return found->second;
        }
        return nullptr;
    }

    void recordHooked(void *art_method, void *backup) {
        std::unique_lock lk(hooked_methods_lock_);
        hooked_methods_.emplace(art_method, backup);
    }

    void recordJitMovement(void *target, void *backup) {
        std::unique_lock lk(jit_movements_lock_);
        jit_movements_.emplace_back(target, backup);
    }

    std::vector<std::pair<void *, void *>> getJitMovements() {
        std::unique_lock lk(jit_movements_lock_);
        return std::move(jit_movements_);
    }

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
                          jobject hook, jobject backup, jboolean is_proxy) {
        art::gc::ScopedGCCriticalSection section(art::Thread::Current().Get(),
                                                 art::gc::kGcCauseDebugger,
                                                 art::gc::kCollectorTypeDebugger);
        art::thread_list::ScopedSuspendAll suspend("Yahfa Hook", false);
        if (yahfa::backupAndHookNative(env, clazz, target, hook, backup)) {
            auto *target_method = yahfa::getArtMethod(env, target);
            auto *backup_method = yahfa::getArtMethod(env, backup);
            recordHooked(target_method, backup_method);
            if (!is_proxy) [[likely]] recordJitMovement(target_method, backup_method);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, isHooked, jobject member) {
        return lspd::isHooked(yahfa::getArtMethod(env, member)) != nullptr;
    }

    LSP_DEF_NATIVE_METHOD(jclass, Yahfa, buildHooker, jobject app_class_loader, jchar return_class,
                          jcharArray classes, jstring method_name, jstring hooker_name) {
        static auto *kInMemoryClassloader = JNI_NewGlobalRef(env, JNI_FindClass(env,
                                                                                "dalvik/system/InMemoryDexClassLoader"));
        static jmethodID kInitMid = JNI_GetMethodID(env, kInMemoryClassloader, "<init>",
                                                    "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        DexBuilder dex_file;

        auto parameter_length = env->GetArrayLength(classes);
        auto parameter_types = std::vector<TypeDescriptor>();
        parameter_types.reserve(parameter_length);
        std::string storage;
        auto return_type =
                return_class == 'L' ? TypeDescriptor::Object : TypeDescriptor::FromDescriptor(
                        (char) return_class);
        auto *params = env->GetCharArrayElements(classes, nullptr);
        for (int i = 0; i < parameter_length; ++i) {
            parameter_types.push_back(
                    params[i] == 'L' ? TypeDescriptor::Object : TypeDescriptor::FromDescriptor(
                            (char) params[i]));
        }

        ClassBuilder cbuilder{dex_file.MakeClass("LspHooker_")};
        cbuilder.set_source_file("LSP");

        auto hooker_type =
                TypeDescriptor::FromClassname(JUTFString(env, hooker_name).get());

        auto *hooker_field = cbuilder.CreateField("hooker", hooker_type)
                .access_flags(dex::kAccStatic)
                .Encode();

        auto hook_builder{cbuilder.CreateMethod(
                JUTFString(env, method_name), Prototype{return_type, parameter_types})};
        // allocate tmp frist because of wide
        auto tmp{hook_builder.AllocRegister()};
        hook_builder.BuildConst(tmp, parameter_types.size());
        auto hook_params_array{hook_builder.AllocRegister()};
        hook_builder.BuildNewArray(hook_params_array, TypeDescriptor::Object, tmp);
        for (size_t i = 0U, j = 0U; i < parameter_types.size(); ++i, ++j) {
            hook_builder.BuildBoxIfPrimitive(Value::Parameter(j), parameter_types[i],
                                             Value::Parameter(j));
            hook_builder.BuildConst(tmp, i);
            hook_builder.BuildAput(Instruction::Op::kAputObject, hook_params_array,
                                   Value::Parameter(j), tmp);
            if (parameter_types[i].is_wide()) ++j;
        }
        auto handle_hook_method{dex_file.GetOrDeclareMethod(
                hooker_type, "handleHookedMethod",
                Prototype{TypeDescriptor::Object, TypeDescriptor::Object.ToArray()})};
        hook_builder.AddInstruction(
                Instruction::GetStaticObjectField(hooker_field->decl->orig_index, tmp));
        hook_builder.AddInstruction(Instruction::InvokeVirtualObject(
                handle_hook_method.id, tmp, tmp, hook_params_array));
        if (return_type == TypeDescriptor::Void) {
            hook_builder.BuildReturn();
        } else if (return_type.is_primitive()) {
            auto box_type{return_type.ToBoxType()};
            const ir::Type *type_def = dex_file.GetOrAddType(box_type);
            hook_builder.AddInstruction(
                    Instruction::Cast(tmp, Value::Type(type_def->orig_index)));
            hook_builder.BuildUnBoxIfPrimitive(tmp, box_type, tmp);
            hook_builder.BuildReturn(tmp, false, return_type.is_wide());
        } else {
            const ir::Type *type_def = dex_file.GetOrAddType(return_type);
            hook_builder.AddInstruction(
                    Instruction::Cast(tmp, Value::Type(type_def->orig_index)));
            hook_builder.BuildReturn(tmp, true);
        }
        [[maybe_unused]] auto *hook_method = hook_builder.Encode();

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

        auto *dex_buffer = env->NewDirectByteBuffer(const_cast<void *>(image.ptr()), image.size());
        auto my_cl = JNI_NewObject(env, kInMemoryClassloader, kInitMid,
                                   dex_buffer, app_class_loader);
        env->DeleteLocalRef(dex_buffer);

        static jmethodID kMid = JNI_GetMethodID(env, kInMemoryClassloader, "loadClass",
                                                "(Ljava/lang/String;)Ljava/lang/Class;");
        if (!kMid) {
            kMid = JNI_GetMethodID(env, kInMemoryClassloader, "findClass",
                                   "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        if (my_cl) {
            auto target = JNI_CallObjectMethod(env, my_cl, kMid,
                                               JNI_NewStringUTF(env, "LspHooker_"));
            if (target) return (jclass) target.release();
        }
        return nullptr;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Yahfa, init, "(I)V"),
            LSP_NATIVE_METHOD(Yahfa, findMethodNative,
                              "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/reflect/Executable;"),
            LSP_NATIVE_METHOD(Yahfa, backupAndHookNative,
                              "(Ljava/lang/reflect/Executable;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Z)Z"),
            LSP_NATIVE_METHOD(Yahfa, isHooked, "(Ljava/lang/reflect/Executable;)Z"),
            LSP_NATIVE_METHOD(Yahfa, buildHooker,
                              "(Ljava/lang/ClassLoader;C[CLjava/lang/String;Ljava/lang/String;)Ljava/lang/Class;"),
    };

    void RegisterYahfa(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(Yahfa);
    }

}  // namespace lspd
