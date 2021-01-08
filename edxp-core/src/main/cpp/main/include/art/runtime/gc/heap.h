
#pragma once

#include <base/object.h>
#include "collector/gc_type.h"
#include "gc_cause.h"
#include "../thread.h"
#include "../runtime.h"

namespace art {

    namespace gc {

        class Heap : public edxp::HookedObject {

        private:
            inline static Heap *instance_;

            CREATE_MEM_FUNC_SYMBOL_ENTRY(collector::GcType, WaitForGcToComplete,
                                     void *thiz, GcCause cause, void *threadSelf) {
                if (LIKELY(WaitForGcToCompleteSym))
                    return WaitForGcToCompleteSym(thiz, cause, threadSelf);
                return art::gc::collector::GcType::kGcTypeNone;
            }

        public:
            Heap(void *thiz) : HookedObject(thiz) {}

            static Heap *Current() {
                return instance_;
            }

            // @ApiSensitive(Level.MIDDLE)
            static void Setup(void *handle, HookFunType hook_func) {
                int api_level = edxp::GetAndroidApiLevel();
                size_t OFFSET_heap;  // Get offset from art::Runtime::RunRootClinits() call in IDA
                switch (api_level) {
                    case __ANDROID_API_O__:
                        [[fallthrough]];
                    case __ANDROID_API_O_MR1__:
                        if constexpr(edxp::is64) {
                            OFFSET_heap = 0x180;
                        } else {
                            OFFSET_heap = 0xF4;
                        }
                        break;
                    case __ANDROID_API_P__:
                        if constexpr(edxp::is64) {
                            OFFSET_heap = 0x1C0;
                        } else {
                            OFFSET_heap = 0x128;
                        }
                        break;
                    case __ANDROID_API_Q__:
                        if constexpr(edxp::is64) {
                            OFFSET_heap = 0x190;
                        } else {
                            OFFSET_heap = 0xF0;
                        }
                        break;
                    default:
                        LOGE("No valid offset for art::Runtime::heap_ found. Using Android R.");
                        [[fallthrough]];
                    case __ANDROID_API_R__:
                        if constexpr(edxp::is64) {
                            // TODO: preload band to a boolean or enum
                            if (edxp::GetAndroidBrand() == "meizu") {
                                OFFSET_heap = 0x190;
                            } else {
                                OFFSET_heap = 392;
                            }
                        } else {
                            // TODO: preload band to a boolean or enum
                            if (edxp::GetAndroidBrand() == "meizu") {
                                OFFSET_heap = 0xF4;
                            } else {
                                OFFSET_heap = 236;
                            }
                        }
                        break;
                }
                void *thiz = *reinterpret_cast<void **>(
                        reinterpret_cast<size_t>(Runtime::Current()->Get()) + OFFSET_heap);
                LOGD("art::runtime::Heap object: %p", thiz);
                instance_ = new Heap(thiz);
                RETRIEVE_MEM_FUNC_SYMBOL(WaitForGcToComplete,
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
