
#pragma once

#include "base/object.h"

namespace art {

    namespace hidden_api {

        enum Action {
            kAllow,
            kAllowButWarn,
            kAllowButWarnAndToast,
            kDeny
        };

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_9ArtMethodEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE",
                Action, GetMethodActionImpl, (), {
                    return Action::kAllow;
                });

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_8ArtFieldEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE",
                Action, GetFieldActionImpl, (), {
                    return Action::kAllow;
                });

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_9ArtMethodEEEbPT_NS0_7ApiListENS0_12AccessMethodE",
                bool, ShouldDenyAccessToMethodImpl, (), {
                    return false;
                });

        CREATE_HOOK_STUB_ENTRIES(
                "_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_8ArtFieldEEEbPT_NS0_7ApiListENS0_12AccessMethodE",
                bool, ShouldDenyAccessToFieldImpl, (), {
                    return false;
                });

        // @ApiSensitive(Level.HIGH)
        static void DisableHiddenApi(void *handle, HookFunType hook_func) {
            const int api_level = lspd::GetAndroidApiLevel();
            if (api_level < __ANDROID_API_P__) {
                return;
            }
            if (api_level == __ANDROID_API_P__) {
                lspd::HookSyms(handle, hook_func, GetMethodActionImpl);
                lspd::HookSyms(handle, hook_func, GetFieldActionImpl);
            } else {
                lspd::HookSyms(handle, hook_func, ShouldDenyAccessToMethodImpl);
                lspd::HookSyms(handle, hook_func, ShouldDenyAccessToFieldImpl);
            }
        };

    }

}
