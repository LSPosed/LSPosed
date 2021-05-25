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

#include <dlfcn.h>
#include <string>
#include <vector>
#include <art/runtime/runtime.h>
#include <dl_util.h>
#include <art/runtime/jni_env_ext.h>
#include <dobby.h>
#include "symbol_cache.h"
#include "logging.h"
#include "native_api.h"
#include "native_hook.h"
#include "art/runtime/mirror/class.h"
#include "art/runtime/art_method.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/thread.h"
#include "art/runtime/hidden_api.h"
#include "art/runtime/instrumentation.h"
#include "art/runtime/thread_list.h"
#include "art/runtime/gc/scoped_gc_critical_section.h"

namespace lspd {
    static std::atomic_bool installed = false;

    void InstallInlineHooks() {
        if (installed.exchange(true)) {
            LOGD("Inline hooks have been installed, skip");
            return;
        }
        LOGD("Start to install inline hooks");
        art::Runtime::Setup(handle_libart);
        art::hidden_api::DisableHiddenApi(handle_libart);
        art::art_method::Setup(handle_libart);
        art::Thread::Setup(handle_libart);
        art::ClassLinker::Setup(handle_libart);
        art::mirror::Class::Setup(handle_libart);
        art::JNIEnvExt::Setup(handle_libart);
        art::instrumentation::DisableUpdateHookedMethodsCode(handle_libart);
        art::thread_list::ScopedSuspendAll::Setup(handle_libart);
        art::gc::ScopedGCCriticalSection::Setup(handle_libart);
        LOGD("Inline hooks installed");
    }
}

