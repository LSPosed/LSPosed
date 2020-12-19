//
// Created by 双草酸酯 on 12/19/20.
//

#ifndef EDXPOSED_ART_METHOD_H
#define EDXPOSED_ART_METHOD_H

#include "jni/edxp_pending_hooks.h"
#include <HookMain.h>

namespace art {

    class ArtMethod : public edxp::HookedObject {

    private:
        inline static size_t oat_header_length;
        inline static int32_t oat_header_code_length_offset;
        CREATE_HOOK_STUB_ENTRIES(void *, GetOatQuickMethodHeader, void *thiz, uintptr_t pc) {
            LOGD("GetOatQuickMethodHeader called");
            // This is a partial copy from AOSP. We only touch them if they are hooked.
            if (LIKELY(edxp::isHooked(thiz))) {
                LOGD("GetOatQuickMethodHeader: isHooked=true, thiz=%p", thiz);
                char* thiz_ = static_cast<char *>(thiz);
                char* code_length_loc = thiz_ + oat_header_code_length_offset;
                uint32_t code_length = *reinterpret_cast<uint32_t *>(code_length_loc);
                uintptr_t original_ep = reinterpret_cast<uintptr_t>(
                        getOriginalEntryPointFromTargetMethod(thiz));
                if (original_ep <= pc <= original_ep + code_length) return thiz_ - oat_header_length;
                // If PC is not in range, we mark it as not found.
                LOGD("GetOatQuickMethodHeader: PC not found in current method.");
                return nullptr;
            }
            return GetOatQuickMethodHeaderBackup(thiz, pc);
        }

    public:
        // @ApiSensitive(Level.MIDDLE)
        static void Setup(void *handle, HookFunType hook_func) {
            LOGD("Classlinker hook setup, handle=%p", handle);
            int api_level = edxp::GetAndroidApiLevel();
            switch (api_level) {
                case __ANDROID_API_O__:
                case __ANDROID_API_O_MR1__:
                case __ANDROID_API_P__:
                    oat_header_length = 24;
                    oat_header_code_length_offset = -4;
                    break;
                default:
                    LOGW("No valid offset in SDK %d for oatHeaderLen, using offset from Android R", api_level);
                case __ANDROID_API_Q__:
                case __ANDROID_API_R__:
                    oat_header_length = 8;
                    oat_header_code_length_offset = -4;
                    break;
            }
            HOOK_FUNC(GetOatQuickMethodHeader, "_ZN3art9ArtMethod23GetOatQuickMethodHeaderEj");
        }
    };
}
#endif //EDXPOSED_ART_METHOD_H
