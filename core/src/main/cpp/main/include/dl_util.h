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

#include <dlfcn.h>
#include "logging.h"

namespace lspd {

    inline static void *DlOpen(const char *file) {
        void *handle = dlopen(file, RTLD_LAZY | RTLD_GLOBAL);
        if (!handle) {
            LOGE("dlopen(%s) failed: %s", file, dlerror());
        }
        return handle;
    }

    template<typename T>
    inline static T DlSym(void *handle, const char *sym_name) {
        if (!handle) {
            LOGE("dlsym(%s) failed: handle is null", sym_name);
        }
        T symbol = reinterpret_cast<T>(dlsym(handle, sym_name));
        if (!symbol) {
            LOGE("dlsym(%s) failed: %s", sym_name, dlerror());
        }
        return symbol;
    }

    class ScopedDlHandle {

    public:
        ScopedDlHandle(const char *file) {
            handle_ = DlOpen(file);
        }

        ~ScopedDlHandle() {
            if (handle_) {
                dlclose(handle_);
            }
        }

        void *Get() const {
            return handle_;
        }

        template<typename T>
        T DlSym(const char *sym_name) const {
            return lspd::DlSym<T>(handle_, sym_name);
        }

        bool IsValid() const {
            return handle_ != nullptr;
        }

    private:
        void *handle_;
    };

}
