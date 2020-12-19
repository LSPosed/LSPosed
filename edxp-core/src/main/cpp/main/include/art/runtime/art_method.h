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
            // LOGD("GetOatQuickMethodHeader called, thiz=%p", thiz);
            // This is a partial copy from AOSP. We only touch them if they are hooked.
            if (LIKELY(edxp::isHooked(thiz))) {
                uintptr_t original_ep = reinterpret_cast<uintptr_t>(getOriginalEntryPointFromTargetMethod(thiz));
                if(original_ep) {
                    char* code_length_loc = reinterpret_cast<char *>(original_ep) + oat_header_code_length_offset;
                    uint32_t code_length = *reinterpret_cast<uint32_t *>(code_length_loc) & ~0x80000000;
                    LOGD("GetOatQuickMethodHeader: isHooked=true, original_ep=0x%x, code_length=0x%x, pc=0x%x", original_ep, code_length, pc);
                    if (original_ep <= pc && pc <= original_ep + code_length)
                        return reinterpret_cast<void *>(original_ep - oat_header_length);
                    // If PC is not in range, we mark it as not found.
                    LOGD("GetOatQuickMethodHeader: PC not found in current method.");
                    return nullptr;
                } else {
                    LOGD("GetOatQuickMethodHeader: isHooked but not backup, fallback to system");
                }
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
