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

#include <base/object.h>

namespace art {

    class Runtime : public lspd::HookedObject {
    private:
        inline static Runtime *instance_;
        CREATE_MEM_FUNC_SYMBOL_ENTRY(void, SetJavaDebuggable, void *thiz, bool value) {
            if (LIKELY(SetJavaDebuggableSym)) {
                SetJavaDebuggableSym(thiz, value);
            }
        }

    public:
        Runtime(void *thiz) : HookedObject(thiz) {}

        static Runtime *Current() {
            return instance_;
        }

        void SetJavaDebuggable(bool value) {
            SetJavaDebuggable(thiz_, value);
        }

        // @ApiSensitive(Level.LOW)
        static void Setup(const SandHook::ElfImg &handle) {
            RETRIEVE_FIELD_SYMBOL(instance, "_ZN3art7Runtime9instance_E");
            RETRIEVE_MEM_FUNC_SYMBOL(SetJavaDebuggable, "_ZN3art7Runtime17SetJavaDebuggableEb");
            void *thiz = *reinterpret_cast<void **>(instance);
            LOGD("_ZN3art7Runtime9instance_E = %p", thiz);
            instance_ = new Runtime(thiz);
        }
    };

}
