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
#include "utils.h"
#include "symbol_cache.h"
#include "logging.h"
#include "native_api.h"
#include "native_hook.h"
#include "riru_hook.h"
#include "art/runtime/mirror/class.h"
#include "art/runtime/art_method.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/thread.h"
#include "art/runtime/hidden_api.h"
#include "art/runtime/instrumentation.h"
#include "art/runtime/reflection.h"
#include "art/runtime/thread_list.h"
#include "art/runtime/gc/scoped_gc_critical_section.h"

namespace lspd {
    static volatile bool installed = false;
    static volatile bool art_hooks_installed = false;

    void InstallArtHooks(void *art_handle);

    void InstallInlineHooks() {
        if (installed) {
            LOGI("Inline hooks have been installed, skip");
            return;
        }
        installed = true;
        LOGI("Start to install inline hooks");
        int api_level = GetAndroidApiLevel();
        if (UNLIKELY(api_level < __ANDROID_API_L__)) {
            LOGE("API level not supported: %d, skip inline hooks", api_level);
            return;
        }
        LOGI("Using api level %d", api_level);
        InstallRiruHooks();
        // install ART hooks
        InstallArtHooks(handle_libart);
//        InstallNativeAPI();
    }

    void InstallArtHooks(void *art_handle) {
        if (art_hooks_installed) {
            return;
        }
        art::Runtime::Setup(art_handle);
        art::hidden_api::DisableHiddenApi(art_handle);
        art::art_method::Setup(art_handle);
        art::Thread::Setup(art_handle);
        art::ClassLinker::Setup(art_handle);
        art::mirror::Class::Setup(art_handle);
        art::JNIEnvExt::Setup(art_handle);
        art::instrumentation::DisableUpdateHookedMethodsCode(art_handle);
        art::PermissiveAccessByReflection(art_handle);
        art::thread_list::ScopedSuspendAll::Setup(art_handle);
        art::gc::ScopedGCCriticalSection::Setup(art_handle);

        art_hooks_installed = true;
        LOGI("ART hooks installed");
    }
}

