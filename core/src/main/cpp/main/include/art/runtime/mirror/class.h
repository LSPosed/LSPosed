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

#include <context.h>
#include "base/object.h"

namespace art {
    namespace mirror {

        using namespace std;
        using namespace lspd;

        class Class : public HookedObject {

        private:

            CREATE_MEM_FUNC_SYMBOL_ENTRY(const char *, GetDescriptor, void *thiz,
                                     std::string *storage) {
                if (GetDescriptorSym)
                    return GetDescriptorSym(thiz, storage);
                else
                    return "";
            }

            CREATE_MEM_HOOK_STUB_ENTRIES("_ZN3art6mirror5Class15IsInSamePackageENS_6ObjPtrIS1_EE",
                    bool, IsInSamePackage, (void *thiz, void* that), {
                std::string storage1;
                std::string storage2;
                const char *thisDesc = GetDescriptor(thiz, &storage1);
                const char *thatDesc = GetDescriptor(that, &storage2);
                // Note: these identifiers should be consistent with those in Java layer
                if (strstr(thisDesc, "LspHooker_") != nullptr
                    || strstr(thatDesc, "LspHooker_") != nullptr
                    || strstr(thisDesc, "org/lsposed/") != nullptr
                    || strstr(thatDesc, "org/lsposed/") != nullptr) {
                    return true;
                }
                // for MIUI resources hooking
                if (strstr(thisDesc, "android/content/res/MiuiTypedArray") != nullptr
                    || strstr(thatDesc, "android/content/res/MiuiTypedArray") != nullptr
                    || strstr(thisDesc, "android/content/res/XResources$XTypedArray") != nullptr
                    || strstr(thatDesc, "android/content/res/XResources$XTypedArray") != nullptr) {
                    return true;
                }
                return backup(thiz, that);
            });

            CREATE_MEM_FUNC_SYMBOL_ENTRY(void*, GetClassDef, void* thiz) {
                if (LIKELY(GetClassDefSym))
                    return GetClassDefSym(thiz);
                return nullptr;
            }

        public:
            Class(void *thiz) : HookedObject(thiz) {}

            // @ApiSensitive(Level.MIDDLE)
            static void Setup(void *handle) {
                RETRIEVE_MEM_FUNC_SYMBOL(GetDescriptor, "_ZN3art6mirror5Class13GetDescriptorEPNSt3__112"
                                                    "basic_stringIcNS2_11char_traitsIcEENS2_9allocatorIcEEEE");

                RETRIEVE_MEM_FUNC_SYMBOL(GetClassDef, "_ZN3art6mirror5Class11GetClassDefEv");

                lspd::HookSyms(handle, IsInSamePackage);
            }

            const char *GetDescriptor(std::string *storage) {
                if (thiz_ && GetDescriptorSym) {
                    return GetDescriptor(thiz_, storage);
                }
                return "";
            }

            std::string GetDescriptor() {
                std::string storage;
                return GetDescriptor(&storage);
            }

            void *GetClassDef() {
                if(thiz_ && GetClassDefSym)
                    return GetClassDef(thiz_);
                return nullptr;
            }
        };

    } // namespace mirror
} // namespace art
