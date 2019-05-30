
#pragma once

#include <JNIHelper.h>
#include <base/object.h>

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

    public:
        ClassLinker(void *thiz) : HookedObject(thiz) {}

        static ClassLinker *Current() {
            return instance_;
        }

        static void Setup(void *handle, HookFunType hook_func) {
            HOOK_FUNC(Constructor, "_ZN3art11ClassLinkerC2EPNS_11InternTableE");
            RETRIEVE_FUNC_SYMBOL(SetEntryPointsToInterpreter,
                                 "_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");
        }

        ALWAYS_INLINE void SetEntryPointsToInterpreter(void *art_method) const {
            if (LIKELY(thiz_))
                SetEntryPointsToInterpreter(thiz_, art_method);
        }

    };
}
