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
// Created by kotori on 2/4/21.
//

#include "native_api.h"
#include "symbol_cache.h"
#include <dobby.h>
#include <vector>
#include <base/object.h>

/*
 * Module: define xposed_native file in /assets, each line is a .so file name
 * LSP: Hook do_dlopen, if any .so file matches the name above, try to call
 *      "native_init(void*)" function in target so with function pointer of "init" below.
 * Module: Call init function with the pointer of callback function.
 * LSP: Store the callback function pointer (multiple callback allowed) and return
 *      LsposedNativeAPIEntries struct.
 * Module: Since JNI is not yet available at that time, module can store the struct to somewhere else,
 *      and handle them in JNI_Onload or later.
 * Module: Do some MAGIC provided by LSPosed framework.
 * LSP: If any so loaded by target app, we will send a callback to the specific module callback function.
 *      But an exception is, if the target skipped dlopen and handle linker stuffs on their own, the
 *      callback will not work.
 */

namespace lspd {
    std::vector<LsposedNativeOnModuleLoaded> moduleLoadedCallbacks;
    std::vector<std::string> moduleNativeLibs;

    LsposedNativeAPIEntriesV1 init(LsposedNativeOnModuleLoaded onModuleLoaded) {
        if (onModuleLoaded != nullptr) moduleLoadedCallbacks.push_back(onModuleLoaded);

        LsposedNativeAPIEntriesV1 ret{
                .version = 1,
                .inlineHookFunc = HookFunction
        };
        return ret;
    }

    void RegisterNativeLib(const std::string& library_name) {
        LOGD("native_api: Registered %s", library_name.c_str());
        moduleNativeLibs.push_back(library_name);
    }

    bool hasEnding(std::string_view fullString, std::string_view ending) {
        if (fullString.length() >= ending.length()) {
            return (0 == fullString.compare (fullString.length() - ending.length(), ending.length(), ending));
        } else {
            return false;
        }
    }

    CREATE_HOOK_STUB_ENTRIES(
            "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv",
            void*, do_dlopen, (const char* name, int flags, const void* extinfo,
                    const void* caller_addr), {
                auto *handle = backup(name, flags, extinfo, caller_addr);
                std::string ns;
                if (name) {
                    ns = std::string(name);
                } else {
                    ns = "NULL";
                }
                LOGD("native_api: do_dlopen(%s)", name);
                if (handle == nullptr) {
                    return nullptr;
                }
                for (std::string_view module_lib: moduleNativeLibs) {
                    // the so is a module so
                    if (UNLIKELY(hasEnding(ns, module_lib))) {
                        LOGI("Loading module native library %s", module_lib.data());
                        void* native_init_sym = dlsym(handle, "native_init");
                        if (UNLIKELY(native_init_sym == nullptr)) {
                            LOGE("Failed to get symbol \"native_init\" from library %s", module_lib.data());
                            break;
                        }
                        auto native_init = reinterpret_cast<NativeInit>(native_init_sym);
                        native_init(reinterpret_cast<void*>(init));
                    }
                }

                // Callbacks
                for (LsposedNativeOnModuleLoaded callback: moduleLoadedCallbacks) {
                    callback(name, handle);
                }
                return handle;
            });

    void InstallNativeAPI() {
        symbol_do_dlopen = DobbySymbolResolver(nullptr, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        LOGD("InstallNativeAPI: %p", symbol_do_dlopen);
        HookSymNoHandle(symbol_do_dlopen, do_dlopen);
    }
}