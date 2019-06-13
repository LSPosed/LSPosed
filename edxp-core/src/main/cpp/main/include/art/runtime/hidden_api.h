
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

        CREATE_HOOK_STUB_ENTRIES(Action, GetMethodActionImpl) {
            return Action::kAllow;
        }

        CREATE_HOOK_STUB_ENTRIES(Action, GetFieldActionImpl) {
            return Action::kAllow;
        }

        CREATE_HOOK_STUB_ENTRIES(bool, ShouldDenyAccessToMethodImpl) {
            return false;
        }

        CREATE_HOOK_STUB_ENTRIES(bool, ShouldDenyAccessToFieldImpl) {
            return false;
        }

        static void DisableHiddenApi(void *handle, HookFunType hook_func) {
            const int api_level = GetAndroidApiLevel();
            if (api_level < ANDROID_P) {
                return;
            }
            if (api_level == ANDROID_P) {
                HOOK_FUNC(GetMethodActionImpl,
                          "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_9ArtMethodEEENS0_"
                          "6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
                HOOK_FUNC(GetFieldActionImpl,
                          "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_8ArtFieldEEENS0_"
                          "6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
            } else {
                HOOK_FUNC(ShouldDenyAccessToMethodImpl,
                          "_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_"
                          "9ArtMethodEEEbPT_NS0_7ApiListENS0_12AccessMethodE");
                HOOK_FUNC(ShouldDenyAccessToFieldImpl,
                          "_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_"
                          "8ArtFieldEEEbPT_NS0_7ApiListENS0_12AccessMethodE");
            }
        };

    }

}
