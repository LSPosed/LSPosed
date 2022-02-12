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
#include "jni/pending_hooks.h"
#include "jni/yahfa.h"
#include "utils.h"
#include "HookMain.h"

namespace art {

    class ClassLinker : public lspd::HookedObject {

    private:
        inline static ClassLinker *instance_;

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, SetEntryPointsToInterpreter, void *thiz,
                                     void *art_method) {
            if (SetEntryPointsToInterpreterSym) [[likely]]
                SetEntryPointsToInterpreterSym(thiz, art_method);
        }

        [[gnu::always_inline]]
        static void MaybeDelayHook(void *clazz_ptr) {
            art::mirror::Class mirror_class(clazz_ptr);
            auto class_def = mirror_class.GetClassDef();
            bool should_intercept = class_def && lspd::IsClassPending(class_def);
            if (should_intercept) [[unlikely]] {
                LOGD("Pending hook for %p (%s)", clazz_ptr,
                     art::mirror::Class(clazz_ptr).GetDescriptor().c_str());
                lspd::Context::GetInstance()->CallOnPostFixupStaticTrampolines(clazz_ptr);
                lspd::DonePendingHook(class_def);
            }
        }

        CREATE_MEM_HOOK_STUB_ENTRIES(
                "_ZN3art11ClassLinker22FixupStaticTrampolinesENS_6ObjPtrINS_6mirror5ClassEEE",
                void, FixupStaticTrampolines, (void * thiz, void * clazz_ptr), {
                    backup(thiz, clazz_ptr);
                    MaybeDelayHook(clazz_ptr);
                });

        CREATE_MEM_HOOK_STUB_ENTRIES(
                "_ZN3art11ClassLinker22FixupStaticTrampolinesEPNS_6ThreadENS_6ObjPtrINS_6mirror5ClassEEE",
                void, FixupStaticTrampolinesWithThread,
                (void * thiz, void * self, void * clazz_ptr), {
                    backup(thiz, self, clazz_ptr);
                    MaybeDelayHook(clazz_ptr);
                });

        CREATE_MEM_HOOK_STUB_ENTRIES(
                "_ZN3art11ClassLinker20MarkClassInitializedEPNS_6ThreadENS_6HandleINS_6mirror5ClassEEE",
                void*, MarkClassInitialized, (void * thiz, void * self, uint32_t * clazz_ptr), {
                    void *result = backup(thiz, self, clazz_ptr);
                    auto ptr = reinterpret_cast<void *>(*clazz_ptr);
                    MaybeDelayHook(ptr);
                    return result;
                });

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, MakeInitializedClassesVisiblyInitialized, void *thiz,
                                     void *self, bool wait) {
            if (MakeInitializedClassesVisiblyInitializedSym) [[likely]]
                MakeInitializedClassesVisiblyInitializedSym(thiz, self, wait);
        }


        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art11ClassLinker30ShouldUseInterpreterEntrypointEPNS_9ArtMethodEPKv",
                bool, ShouldUseInterpreterEntrypoint, (void * art_method,
                        const void *quick_code), {
                    if (quick_code != nullptr &&
                        (lspd::isHooked(art_method) ||
                         lspd::IsMethodPending(art_method))) [[unlikely]] {
                        return false;
                    }
                    return backup(art_method, quick_code);
                });

        CREATE_FUNC_SYMBOL_ENTRY(void, art_quick_to_interpreter_bridge, void*) {}
        CREATE_FUNC_SYMBOL_ENTRY(void, art_quick_generic_jni_trampoline, void*) {}

    public:
        ClassLinker(void *thiz) : HookedObject(thiz) {}

        inline static ClassLinker *Current() {
            return instance_;
        }

        // @ApiSensitive(Level.MIDDLE)
        inline static void Setup(const SandHook::ElfImg &handle) {
            int api_level = lspd::GetAndroidApiLevel();

            instance_ = new ClassLinker(nullptr); // make it nullptr

            RETRIEVE_MEM_FUNC_SYMBOL(SetEntryPointsToInterpreter,
                                     "_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");

            if (api_level < __ANDROID_API_T__) {
                lspd::HookSyms(handle, ShouldUseInterpreterEntrypoint);
            }

            if (api_level >= __ANDROID_API_R__) {
                // In android R, FixupStaticTrampolines won't be called unless it's marking it as
                // visiblyInitialized.
                // So we miss some calls between initialized and visiblyInitialized.
                // Therefore we hook the new introduced MarkClassInitialized instead
                // This only happens on non-x86 devices
                lspd::HookSyms(handle, MarkClassInitialized);
                lspd::HookSyms(handle, FixupStaticTrampolinesWithThread, FixupStaticTrampolines);
            } else {
                lspd::HookSyms(handle, FixupStaticTrampolines);
            }

            // MakeInitializedClassesVisiblyInitialized will cause deadlock
            // IsQuickToInterpreterBridge is inlined
            // So we use GetSavedEntryPointOfPreCompiledMethod instead
//            if (api_level >= __ANDROID_API_R__) {
//                RETRIEVE_FUNC_SYMBOL(MakeInitializedClassesVisiblyInitialized,
//                                     "_ZN3art11ClassLinker40MakeInitializedClassesVisiblyInitializedEPNS_6ThreadEb");
//            }

            RETRIEVE_FUNC_SYMBOL(art_quick_to_interpreter_bridge, "art_quick_to_interpreter_bridge");
            RETRIEVE_FUNC_SYMBOL(art_quick_generic_jni_trampoline, "art_quick_generic_jni_trampoline");

            LOGD("art_quick_to_interpreter_bridge = %p", art_quick_to_interpreter_bridgeSym);
            LOGD("art_quick_generic_jni_trampoline = %p", art_quick_generic_jni_trampolineSym);
        }

        [[gnu::always_inline]]
        void MakeInitializedClassesVisiblyInitialized(void *self, bool wait) const {
            LOGD("MakeInitializedClassesVisiblyInitialized start, thiz=%p, self=%p", thiz_, self);
            if (thiz_) [[likely]]
                MakeInitializedClassesVisiblyInitialized(thiz_, self, wait);
            else LOGW("Classlinker is nullptr");
        }

        [[gnu::always_inline]]
        void SetEntryPointsToInterpreter(void *art_method) const {
            if (art_quick_to_interpreter_bridgeSym && art_quick_generic_jni_trampolineSym) [[likely]] {
                if (yahfa::getAccessFlags(art_method) & yahfa::kAccNative) [[unlikely]] {
                    yahfa::setEntryPoint(art_method,
                                         reinterpret_cast<void *>(art_quick_generic_jni_trampolineSym));
                } else {
                    yahfa::setEntryPoint(art_method,
                                         reinterpret_cast<void *>(art_quick_to_interpreter_bridgeSym));
                }
            }
            SetEntryPointsToInterpreter(thiz_, art_method);
        }

    };
}
