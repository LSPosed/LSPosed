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

#include "yahfa.h"
#include "HookMain.h"
#include "nativehelper/jni_macros.h"
#include "native_util.h"
#include "pending_hooks.h"
#include "art/runtime/class_linker.h"
#include "art/runtime/thread_list.h"
#include "art/runtime/thread.h"
#include "art/runtime/gc/scoped_gc_critical_section.h"


namespace lspd {

    LSP_DEF_NATIVE_METHOD(void, Yahfa, init, jint sdkVersion) {
        Java_lab_galaxy_yahfa_HookMain_init(env, clazz, sdkVersion);
    }

    LSP_DEF_NATIVE_METHOD(jobject, Yahfa, findMethodNative, jclass targetClass,
                                          jstring methodName, jstring methodSig) {
        return Java_lab_galaxy_yahfa_HookMain_findMethodNative(env, clazz, targetClass, methodName,
                                                               methodSig);
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, backupAndHookNative, jobject target,
                                              jobject hook, jobject backup) {
        art::gc::ScopedGCCriticalSection section(art::Thread::Current().Get(), art::gc::kGcCauseDebugger, art::gc::kCollectorTypeDebugger);
        art::thread_list::ScopedSuspendAll suspend("Yahfa Hook", false);
        return Java_lab_galaxy_yahfa_HookMain_backupAndHookNative(env, clazz, target, hook, backup);
    }

    LSP_DEF_NATIVE_METHOD(void, Yahfa, recordHooked, jobject member) {
        lspd::recordHooked(getArtMethodYahfa(env, member));
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, isHooked, jobject member) {
        return lspd::isHooked(getArtMethodYahfa(env, member));
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Yahfa, init, "(I)V"),
            LSP_NATIVE_METHOD(Yahfa, findMethodNative,
                          "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/reflect/Member;"),
            LSP_NATIVE_METHOD(Yahfa, backupAndHookNative,
                          "(Ljava/lang/Object;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Z"),
            LSP_NATIVE_METHOD(Yahfa, recordHooked, "(Ljava/lang/reflect/Member;)V"),
            LSP_NATIVE_METHOD(Yahfa, isHooked, "(Ljava/lang/reflect/Member;)Z")
    };

    void RegisterYahfa(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(Yahfa);
    }

}