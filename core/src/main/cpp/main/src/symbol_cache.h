//
// Created by kotori on 2/7/21.
//

#ifndef LSPOSED_SYMBOL_CACHE_H
#define LSPOSED_SYMBOL_CACHE_H
#include <atomic>

namespace lspd {
    static std::atomic_bool initialized;
    static void* symbol_do_dlopen = nullptr;

    void InitSymbolCache();
}

#endif //LSPOSED_SYMBOL_CACHE_H
