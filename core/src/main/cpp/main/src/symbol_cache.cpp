//
// Created by kotori on 2/7/21.
//

#include "symbol_cache.h"
#include <dobby.h>
#include <art/base/macros.h>
#include <logging.h>

namespace lspd {
    void InitSymbolCache() {
        if (LIKELY(initialized)) return;
        LOGD("InitSymbolCache");
        // TODO: set image name
        symbol_do_dlopen = DobbySymbolResolver(nullptr, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        initialized = true;
    }
}