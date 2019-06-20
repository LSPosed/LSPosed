
#pragma once

#include <JNIHelper.h>
#include <base/object.h>
#include <art/runtime/mirror/class.h>
#include <android-base/strings.h>
#include "runtime.h"
#include "jni_env_ext.h"
#include "edxp_context.h"
#include "jni/edxp_pending_hooks.h"

namespace art {

    class ClassLinker : public edxp::HookedObject {

    private:
        inline static ClassLinker *instance_;

        CREATE_FUNC_SYMBOL_ENTRY(void, SetEntryPointsToInterpreter, void *thiz, void *art_method) {
            if (LIKELY(SetEntryPointsToInterpreterSym))
                SetEntryPointsToInterpreterSym(thiz, art_method);
        }

        CREATE_HOOK_STUB_ENTRIES(void *, Constructor, void *thiz, void *intern_table) {
            if (LIKELY(instance_))
                instance_->Reset(thiz);
            else
                instance_ = new ClassLinker(thiz);
            return ConstructorBackup(thiz, intern_table);
        }

        CREATE_HOOK_STUB_ENTRIES(void, FixupStaticTrampolines, void *thiz, void *clazz_ptr) {
            art::mirror::Class clazz(clazz_ptr);
            std::string storage;
            const char *desc = clazz.GetDescriptor(&storage);
            bool should_intercept = edxp::IsClassPending(desc);
            if (UNLIKELY(should_intercept)) {
                edxp::Context::GetInstance()->CallOnPreFixupStaticTrampolines(clazz_ptr);
            }
            FixupStaticTrampolinesBackup(thiz, clazz_ptr);
            if (UNLIKELY(should_intercept)) {
                edxp::Context::GetInstance()->CallOnPostFixupStaticTrampolines(clazz_ptr);
            }
        }

    public:
        ClassLinker(void *thiz) : HookedObject(thiz) {}

        static ClassLinker *Current() {
            return instance_;
        }

        static void Setup(void *handle, HookFunType hook_func) {
            HOOK_FUNC(Constructor, "_ZN3art11ClassLinkerC2EPNS_11InternTableE",
                      "_ZN3art11ClassLinkerC2EPNS_11InternTableEb"); // 10.0
            RETRIEVE_FUNC_SYMBOL(SetEntryPointsToInterpreter,
                                 "_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");

            HOOK_FUNC(FixupStaticTrampolines,
                      "_ZN3art11ClassLinker22FixupStaticTrampolinesENS_6ObjPtrINS_6mirror5ClassEEE");
        }

        ALWAYS_INLINE void SetEntryPointsToInterpreter(void *art_method) const {
            if (LIKELY(thiz_))
                SetEntryPointsToInterpreter(thiz_, art_method);
        }

    };
}
