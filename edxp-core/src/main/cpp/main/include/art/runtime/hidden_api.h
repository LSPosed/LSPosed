
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

        static void DisableHiddenApi(void *handle, HookFunType hook_func) {
            if (GetAndroidApiLevel() < ANDROID_P) {
                return;
            }
            HOOK_FUNC(GetMethodActionImpl,
                      "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_9ArtMethodEEENS0_"
                      "6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
            HOOK_FUNC(GetFieldActionImpl,
                      "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_8ArtFieldEEENS0_"
                      "6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
        };

    }

}
