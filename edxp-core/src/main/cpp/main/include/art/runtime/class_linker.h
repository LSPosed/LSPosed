
#pragma once

#include <JNIHelper.h>
#include <base/object.h>
#include <art/runtime/mirror/class.h>
#include <android-base/strings.h>
#include "runtime.h"
#include "config.h"
#include "jni_env_ext.h"
#include "edxp_context.h"
#include "jni/edxp_pending_hooks.h"
#include "utils.h"
#include "HookMain.h"

namespace art {

    class ClassLinker : public edxp::HookedObject {

    private:
        inline static ClassLinker *instance_;

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, SetEntryPointsToInterpreter, void *thiz, void *art_method) {
            if (LIKELY(SetEntryPointsToInterpreterSym))
                SetEntryPointsToInterpreterSym(thiz, art_method);
        }

        CREATE_MEM_HOOK_STUB_ENTRIES(void, FixupStaticTrampolines, void *thiz, void *clazz_ptr) {
            FixupStaticTrampolinesBackup(thiz, clazz_ptr);
            art::mirror::Class mirror_class(clazz_ptr);
            auto class_def = mirror_class.GetClassDef();
            bool should_intercept = class_def && edxp::IsClassPending(class_def);
            if (UNLIKELY(should_intercept)) {
                LOGD("Pending hook for %p (%s)", clazz_ptr,
                     art::mirror::Class(clazz_ptr).GetDescriptor().c_str());
                edxp::Context::GetInstance()->CallOnPostFixupStaticTrampolines(clazz_ptr);
            }
        }

        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, MakeInitializedClassesVisiblyInitialized, void *thiz,
                                 void *self, bool wait) {
            if (LIKELY(MakeInitializedClassesVisiblyInitializedSym))
                MakeInitializedClassesVisiblyInitializedSym(thiz, self, wait);
        }


        CREATE_HOOK_STUB_ENTRIES(bool, ShouldUseInterpreterEntrypoint, void *art_method,
                                 const void *quick_code) {
            if (quick_code != nullptr && UNLIKELY(edxp::isHooked(art_method))) {
                return false;
            }
            return ShouldUseInterpreterEntrypointBackup(art_method, quick_code);
        }

    public:
        ClassLinker(void *thiz) : HookedObject(thiz) {}

        static ClassLinker *Current() {
            return instance_;
        }

        // @ApiSensitive(Level.MIDDLE)
        static void Setup(void *handle, HookFunType hook_func) {
            LOGD("Classlinker hook setup, handle=%p", handle);
            int api_level = edxp::GetAndroidApiLevel();
            size_t OFFSET_classlinker;  // Get offset from art::Runtime::RunRootClinits() call in IDA
            switch (api_level) {
                case __ANDROID_API_O__:
                    [[fallthrough]];
                case __ANDROID_API_O_MR1__:
                    if constexpr(edxp::is64) {
                        OFFSET_classlinker = 464;
                    } else {
                        OFFSET_classlinker = 284;
                    }
                    break;
                case __ANDROID_API_P__:
                    if constexpr(edxp::is64) {
                        OFFSET_classlinker = 528;
                    } else {
                        OFFSET_classlinker = 336;
                    }
                    break;
                case __ANDROID_API_Q__:
                    if constexpr(edxp::is64) {
                        OFFSET_classlinker = 480;
                    } else {
                        OFFSET_classlinker = 280;
                    }
                    break;
                default:
                    LOGE("No valid offset for art::Runtime::class_linker_ found. Using Android R.");
                    [[fallthrough]];
                case __ANDROID_API_R__:
                    if constexpr(edxp::is64) {
                        OFFSET_classlinker = 472;
                    } else {
                        OFFSET_classlinker = 276;
                    }
                    break;
            }

            void *thiz = *reinterpret_cast<void **>(
                    reinterpret_cast<size_t>(Runtime::Current()->Get()) + OFFSET_classlinker);
            // ClassLinker* GetClassLinker() but inlined
            LOGD("Classlinker object: %p", thiz);
            instance_ = new ClassLinker(thiz);

            RETRIEVE_MEM_FUNC_SYMBOL(SetEntryPointsToInterpreter,
                                 "_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");

            HOOK_MEM_FUNC(FixupStaticTrampolines,
                      "_ZN3art11ClassLinker22FixupStaticTrampolinesENS_6ObjPtrINS_6mirror5ClassEEE");

            HOOK_FUNC(ShouldUseInterpreterEntrypoint,
                      "_ZN3art11ClassLinker30ShouldUseInterpreterEntrypointEPNS_9ArtMethodEPKv");

            // MakeInitializedClassesVisiblyInitialized will cause deadlock
            // IsQuickToInterpreterBridge cannot be hooked by Dobby yet
            // So we use GetSavedEntryPointOfPreCompiledMethod instead
//            if (api_level >= __ANDROID_API_R__) {
//                RETRIEVE_FUNC_SYMBOL(MakeInitializedClassesVisiblyInitialized,
//                                     "_ZN3art11ClassLinker40MakeInitializedClassesVisiblyInitializedEPNS_6ThreadEb");
//            }

        }

        ALWAYS_INLINE void MakeInitializedClassesVisiblyInitialized(void *self, bool wait) const {
            LOGD("MakeInitializedClassesVisiblyInitialized start, thiz=%p, self=%p", thiz_, self);
            if (LIKELY(thiz_))
                MakeInitializedClassesVisiblyInitialized(thiz_, self, wait);
        }

        ALWAYS_INLINE void SetEntryPointsToInterpreter(void *art_method) const {
            LOGD("SetEntryPointsToInterpreter start, thiz=%p, art_method=%p", thiz_, art_method);
            if (LIKELY(thiz_))
                SetEntryPointsToInterpreter(thiz_, art_method);
        }

    };
}
