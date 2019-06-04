
#pragma once

#include <dlfcn.h>
#include "logging.h"

namespace edxp {

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
            return edxp::DlSym<T>(handle_, sym_name);
        }

        bool IsValid() const {
            return handle_ != nullptr;
        }

    private:
        void *handle_;
    };

}
