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
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma once

#include <jni_helper.h>
#include <base/object.h>
#include <art/runtime/mirror/class.h>
#include "utils.h"
#include "jni/pending_hooks.h"

namespace art {
    namespace profiling_info {
        inline static size_t OFFSET_art_method = -1;

        CREATE_MEM_HOOK_STUB_ENTRIES(
                "_ZN3art13ProfilingInfo13AddInvokeInfoEjPNS_6mirror5ClassE",
                void, AddInvokeInfo, (void * thiz, uint32_t dex_pc, void * clazz_ptr), {
                    void *method = *reinterpret_cast<void **>(
                            reinterpret_cast<uintptr_t>(thiz) + OFFSET_art_method);
                    if (lspd::isHooked(method)) [[unlikely]] return;
                    backup(thiz, dex_pc, clazz_ptr);
                });

        static void Setup(const SandHook::ElfImg &handle) {
            int api_level = lspd::GetAndroidApiLevel();
            switch (api_level) {
                case __ANDROID_API_Q__:
                    OFFSET_art_method = 0;
                    break;
                case __ANDROID_API_O_MR1__:
                    [[fallthrough]];
                case __ANDROID_API_P__:
                    [[fallthrough]];
                case __ANDROID_API_R__:
                    [[fallthrough]];
                case __ANDROID_API_S__:
                    if constexpr(lspd::is64) {
                        OFFSET_art_method = 8;
                    } else {
                        OFFSET_art_method = 4;
                    }
                    break;
            }
            if (OFFSET_art_method != size_t(-1))
                lspd::HookSyms(handle, AddInvokeInfo);
        }
    }
}
