//
// Created by 双草酸酯 on 12/19/20.
//

#ifndef EDXPOSED_ART_METHOD_H
#define EDXPOSED_ART_METHOD_H

#include "jni/edxp_pending_hooks.h"
#include <HookMain.h>

namespace art {
    namespace art_method {

        inline static size_t oat_header_length;
        inline static int32_t oat_header_code_length_offset;

        CREATE_MEM_FUNC_SYMBOL_ENTRY(std::string, PrettyMethod, void *thiz, bool with_signature) {
            if (UNLIKELY(thiz == nullptr))
                return "null";
            if (LIKELY(PrettyMethodSym))
                return PrettyMethodSym(thiz, with_signature);
            else return "null sym";
        }

        inline static std::string PrettyMethod(void *thiz) {
            return PrettyMethod(thiz, true);
        }

        CREATE_MEM_HOOK_STUB_ENTRIES(void *, GetOatQuickMethodHeader, void *thiz, uintptr_t pc) {
            // This is a partial copy from AOSP. We only touch them if they are hooked.
            if (UNLIKELY(edxp::isHooked(thiz))) {
                uintptr_t original_ep = reinterpret_cast<uintptr_t>(getOriginalEntryPointFromTargetMethod(
                        thiz)) & ~0x1;
                if (original_ep) {
                    char *code_length_loc =
                            reinterpret_cast<char *>(original_ep) + oat_header_code_length_offset;
                    uint32_t code_length =
                            *reinterpret_cast<uint32_t *>(code_length_loc) & ~0x80000000u;
                    LOGD("art_method::GetOatQuickMethodHeader: ArtMethod=%p (%s), isHooked=true, original_ep=0x%zux, code_length=0x%x, pc=0x%zux",
                         thiz, PrettyMethod(thiz).c_str(), original_ep, code_length, pc);
                    if (original_ep <= pc && pc <= original_ep + code_length)
                        return reinterpret_cast<void *>(original_ep - oat_header_length);
                    // If PC is not in range, we mark it as not found.
                    LOGD("art_method::GetOatQuickMethodHeader: PC not found in current method.");
                    return nullptr;
                } else {
                    LOGD("art_method::GetOatQuickMethodHeader: ArtMethod=%p (%s) isHooked but not backup, fallback to system",
                            thiz, PrettyMethod(thiz).c_str());
                }
            }
            return GetOatQuickMethodHeaderBackup(thiz, pc);
        }

        static void Setup(void *handle, HookFunType hook_func) {
            LOGD("art_method hook setup, handle=%p", handle);
            int api_level = edxp::GetAndroidApiLevel();
            switch (api_level) {
                case __ANDROID_API_O__:
                    [[fallthrough]];
                case __ANDROID_API_O_MR1__:
                    [[fallthrough]];
                case __ANDROID_API_P__:
                    oat_header_length = 24;
                    oat_header_code_length_offset = -4;
                    break;
                default:
                    LOGW("No valid offset in SDK %d for oat_header_length, using offset from Android R",
                         api_level);
                    [[fallthrough]];
                case __ANDROID_API_Q__:
                    [[fallthrough]];
                case __ANDROID_API_R__:
                    oat_header_length = 8;
                    oat_header_code_length_offset = -4;
                    break;
            }
            if constexpr (edxp::is64) {
                HOOK_MEM_FUNC(GetOatQuickMethodHeader, "_ZN3art9ArtMethod23GetOatQuickMethodHeaderEm");
            } else {
                HOOK_MEM_FUNC(GetOatQuickMethodHeader, "_ZN3art9ArtMethod23GetOatQuickMethodHeaderEj");
            }

            RETRIEVE_MEM_FUNC_SYMBOL(PrettyMethod, "_ZN3art9ArtMethod12PrettyMethodEb");
        }
    }
}

#endif //EDXPOSED_ART_METHOD_H
