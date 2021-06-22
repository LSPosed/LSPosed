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

#ifndef LSPATCH_OAT_FILE_MANAGER_H
#define LSPATCH_OAT_FILE_MANAGER_H

#include "base/object.h"

namespace art {
    CREATE_MEM_HOOK_STUB_ENTRIES(
            "_ZN3art14OatFileManager25RunBackgroundVerificationERKNSt3__16vectorIPKNS_7DexFileENS1_9allocatorIS5_EEEEP8_jobjectPKc",
            void, RunBackgroundVerificationWithContext,
            (void * thiz, const std::vector<const void *> &dex_files,
                    jobject class_loader,
                    const char *class_loader_context), {
                if (lspd::Context::GetInstance()->GetCurrentClassLoader() == nullptr) {
                    LOGD("Disabled background verification");
                    return;
                }
                backup(thiz, dex_files, class_loader, class_loader_context);
            });

    CREATE_MEM_HOOK_STUB_ENTRIES(
            "_ZN3art14OatFileManager25RunBackgroundVerificationERKNSt3__16vectorIPKNS_7DexFileENS1_9allocatorIS5_EEEEP8_jobject",
            void, RunBackgroundVerification,
            (void * thiz, const std::vector<const void *> &dex_files,
                    jobject class_loader), {
                if (lspd::Context::GetInstance()->GetCurrentClassLoader() == nullptr) {
                    LOGD("Disabled background verification");
                    return;
                }
                backup(thiz, dex_files, class_loader);
            });


    static void DisableBackgroundVerification(const SandHook::ElfImg &handle) {
        lspd::HookSyms(handle, RunBackgroundVerificationWithContext, RunBackgroundVerification);
    }
}


#endif //LSPATCH_OAT_FILE_MANAGER_H
