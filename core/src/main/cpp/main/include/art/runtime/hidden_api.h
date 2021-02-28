/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

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
        static void DisableHiddenApi(void *handle) {
            const int api_level = lspd::GetAndroidApiLevel();
            if (api_level < __ANDROID_API_P__) {
                return;
            }
            if (api_level == __ANDROID_API_P__) {
                lspd::HookSyms(handle, GetMethodActionImpl);
                lspd::HookSyms(handle, GetFieldActionImpl);
            } else {
                lspd::HookSyms(handle, ShouldDenyAccessToMethodImpl);
                lspd::HookSyms(handle, ShouldDenyAccessToFieldImpl);
            }
        };

    }

}
