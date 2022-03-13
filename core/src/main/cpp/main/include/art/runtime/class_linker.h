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

#include <jni_helper.h>
#include <base/object.h>
#include <art/runtime/mirror/class.h>
#include "runtime.h"
#include "config.h"
#include "jni_env_ext.h"
#include "context.h"
#include "jni/yahfa.h"
#include "utils.h"
#include "HookMain.h"

namespace art {

    class ClassLinker : public lspd::HookedObject {
    private:
        [[gnu::always_inline]]
        static auto MaybeDelayHook(void *clazz_ptr) {
            std::vector<std::tuple<void*, void*>> out;
            art::mirror::Class mirror_class(clazz_ptr);
            auto class_def = mirror_class.GetClassDef();
            if (!class_def) return out;
            auto set = lspd::isUninitializedHooked(class_def);
            if (!set.empty()) [[unlikely]] {
                LOGD("Pending hook for %p (%s)", clazz_ptr,
                     art::mirror::Class(clazz_ptr).GetDescriptor().c_str());
                for (auto art_method : set) {
                    out.emplace_back(art_method, yahfa::getEntryPoint(art_method));
                }
            }
            return out;
        }

        [[gnu::always_inline]]
        static void FixTrampoline(const std::vector<std::tuple<void*, void*>>& methods) {
            for (const auto &[art_method, old_trampoline] : methods) {
                auto *new_trampoline = yahfa::getEntryPoint(art_method);
                auto *backup = lspd::isHooked(art_method);
                if (backup && new_trampoline != old_trampoline) {
                    yahfa::setEntryPoint(backup, new_trampoline);
                    yahfa::setEntryPoint(art_method, old_trampoline);
                }
            }
        }

        CREATE_MEM_HOOK_STUB_ENTRIES(
        "_ZN3art11ClassLinker22FixupStaticTrampolinesENS_6ObjPtrINS_6mirror5ClassEEE",
        void, FixupStaticTrampolines, (void * thiz, void * clazz_ptr), {
            auto b = MaybeDelayHook(clazz_ptr);
            backup(thiz, clazz_ptr);
            FixTrampoline(b);
        });

        CREATE_MEM_HOOK_STUB_ENTRIES(
        "_ZN3art11ClassLinker22FixupStaticTrampolinesEPNS_6ThreadENS_6ObjPtrINS_6mirror5ClassEEE",
        void, FixupStaticTrampolinesWithThread,
        (void * thiz, void * self, void * clazz_ptr), {
            auto b = MaybeDelayHook(clazz_ptr);
            backup(thiz, self, clazz_ptr);
            FixTrampoline(b);
        });

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, MakeInitializedClassesVisiblyInitialized, void *thiz,
                                     void *self, bool wait) {
            if (MakeInitializedClassesVisiblyInitializedSym) [[likely]]
                        MakeInitializedClassesVisiblyInitializedSym(thiz, self, wait);
        }

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, SetEntryPointsToInterpreter, void *thiz,
                                     void *art_method) {
            if (SetEntryPointsToInterpreterSym) [[likely]]
                SetEntryPointsToInterpreterSym(thiz, art_method);
        }

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art11ClassLinker30ShouldUseInterpreterEntrypointEPNS_9ArtMethodEPKv",
                bool, ShouldUseInterpreterEntrypoint, (void * art_method,
                        const void *quick_code), {
                    if (quick_code != nullptr && lspd::isHooked(art_method)) [[unlikely]] {
                        return false;
                    }
                    return backup(art_method, quick_code);
                });

        CREATE_FUNC_SYMBOL_ENTRY(void, art_quick_to_interpreter_bridge, void*) {}
        CREATE_FUNC_SYMBOL_ENTRY(void, art_quick_generic_jni_trampoline, void*) {}

        CREATE_HOOK_STUB_ENTRIES("_ZN3art11interpreter29ShouldStayInSwitchInterpreterEPNS_9ArtMethodE",
                                 bool, ShouldStayInSwitchInterpreter ,(void* art_method), {
            if (lspd::isHooked(art_method)) [[unlikely]] {
                return false;
            }
            return backup(art_method);
        });

    public:
        // @ApiSensitive(Level.MIDDLE)
        inline static void Setup(const SandHook::ElfImg &handle) {
            RETRIEVE_MEM_FUNC_SYMBOL(SetEntryPointsToInterpreter,
                                     "_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");

            lspd::HookSyms(handle, ShouldUseInterpreterEntrypoint, ShouldStayInSwitchInterpreter);

            lspd::HookSyms(handle, FixupStaticTrampolinesWithThread, FixupStaticTrampolines);

            RETRIEVE_FUNC_SYMBOL(art_quick_to_interpreter_bridge, "art_quick_to_interpreter_bridge");
            RETRIEVE_FUNC_SYMBOL(art_quick_generic_jni_trampoline, "art_quick_generic_jni_trampoline");

            LOGD("art_quick_to_interpreter_bridge = %p", art_quick_to_interpreter_bridgeSym);
            LOGD("art_quick_generic_jni_trampoline = %p", art_quick_generic_jni_trampolineSym);
        }

        [[gnu::always_inline]]
        static void SetEntryPointsToInterpreter(void *art_method) {
            if (art_quick_to_interpreter_bridgeSym && art_quick_generic_jni_trampolineSym) [[likely]] {
                if (yahfa::getAccessFlags(art_method) & yahfa::kAccNative) [[unlikely]] {
                    yahfa::setEntryPoint(art_method,
                                         reinterpret_cast<void *>(art_quick_generic_jni_trampolineSym));
                } else {
                    yahfa::setEntryPoint(art_method,
                                         reinterpret_cast<void *>(art_quick_to_interpreter_bridgeSym));
                }
            }
            SetEntryPointsToInterpreter(nullptr, art_method);
        }

    };
}
