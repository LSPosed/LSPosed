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

//
// Created by kotori on 2/7/21.
//

#include "symbol_cache.h"
#include "elf_util.h"
#include <dobby.h>
#include "macros.h"
#include "config.h"
#include <vector>
#include <logging.h>

namespace lspd {
    bool sym_initialized = false;
    void *sym_do_dlopen = nullptr;
    void *sym_openInMemoryDexFilesNative = nullptr;
    void *sym_createCookieWithArray = nullptr;
    void *sym_createCookieWithDirectBuffer = nullptr;
    void *sym_openDexFileNative = nullptr;
    void *sym_setTrusted = nullptr;
    void *sym_set_table_override = nullptr;
    std::unique_ptr<SandHook::ElfImg> art_img = nullptr;

    bool findLibArt() {
        art_img = std::make_unique<SandHook::ElfImg>(kLibArtName);
        if (!art_img->isValid()) return false;
        auto api_level = GetAndroidApiLevel();
        return (sym_set_table_override = art_img->getSymbAddress<void *>(
                "_ZN3art9JNIEnvExt16SetTableOverrideEPK18JNINativeInterface")) != nullptr
               && (api_level < __ANDROID_API_P__ || (
                (sym_openDexFileNative = art_img->getSymbAddress<void *>(
                        "_ZN3artL25DexFile_openDexFileNativeEP7_JNIEnvP7_jclassP8_jstringS5_iP8_jobjectP13_jobjectArray")) &&
                (
                        (sym_openInMemoryDexFilesNative = art_img->getSymbAddress<void *>(
                                "_ZN3artL34DexFile_openInMemoryDexFilesNativeEP7_JNIEnvP7_jclassP13_jobjectArrayS5_P10_jintArrayS7_P8_jobjectS5_")) ||
                        (
                                (sym_createCookieWithArray = art_img->getSymbAddress<void *>(
                                        "_ZN3artL29DexFile_createCookieWithArrayEP7_JNIEnvP7_jclassP11_jbyteArrayii")) &&
                                (sym_createCookieWithDirectBuffer = art_img->getSymbAddress<void *>(
                                        "_ZN3artL36DexFile_createCookieWithDirectBufferEP7_JNIEnvP7_jclassP8_jobjectii"))
                        )
                ) &&
                (sym_setTrusted = art_img->getSymbAddress<void *>(
                        "_ZN3artL18DexFile_setTrustedEP7_JNIEnvP7_jclassP8_jobject"))));
    }

    void InitSymbolCache() {
        if (UNLIKELY(sym_initialized)) return;
        LOGD("InitSymbolCache");
        sym_initialized = findLibArt();
        if (UNLIKELY(!sym_initialized)) {
            art_img.reset();
            LOGE("Init symbol cache failed");
        }
    }
}
