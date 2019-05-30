
#pragma once

#include <base/object.h>
#include "collector/gc_type.h"
#include "gc_cause.h"
#include "../thread.h"

namespace art {

    namespace gc {

        class Heap : public edxp::HookedObject {

        private:
            inline static Heap *instance_;

            CREATE_FUNC_SYMBOL_ENTRY(collector::GcType, WaitForGcToComplete,
                                     void *thiz, GcCause cause, void *threadSelf) {
                if (LIKELY(WaitForGcToCompleteSym))
                    return WaitForGcToCompleteSym(thiz, cause, threadSelf);
                return art::gc::collector::GcType::kGcTypeNone;
            }

            CREATE_HOOK_STUB_ENTRIES(void, PreZygoteFork, void *thiz) {
                if (instance_)
                    instance_->Reset(thiz);
                else
                    instance_ = new Heap(thiz);
                PreZygoteForkBackup(thiz);
            }

        public:
            Heap(void *thiz) : HookedObject(thiz) {}

            static Heap *Current() {
                return instance_;
            }

            static void Setup(void *handle, HookFunType hook_func) {
                HOOK_FUNC(PreZygoteFork, "_ZN3art2gc4Heap13PreZygoteForkEv");
                RETRIEVE_FUNC_SYMBOL(WaitForGcToComplete,
                                     "_ZN3art2gc4Heap19WaitForGcToCompleteENS0_7GcCauseEPNS_6ThreadE");
            }

            ALWAYS_INLINE collector::GcType
            WaitForGcToComplete(GcCause cause, void *threadSelf) const {
                if (LIKELY(thiz_))
                    return WaitForGcToComplete(thiz_, cause, threadSelf);
                return art::gc::collector::GcType::kGcTypeNone;
            }

        };
    } // namespace gc
} // namespace art
