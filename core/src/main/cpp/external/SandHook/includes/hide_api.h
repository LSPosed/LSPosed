//
// Created by swift on 2019/1/21.
//

#ifndef SANDHOOK_HIDE_API_H
#define SANDHOOK_HIDE_API_H

#include <jni.h>
#include "dlfcn_nougat.h"
#include "dlfcn.h"
#include <memory>
#include "../includes/art_compiler_options.h"
#include "../includes/art_jit.h"
#include "../includes/art_method.h"

#if defined(__aarch64__)
# define __get_tls() ({ void** __val; __asm__("mrs %0, tpidr_el0" : "=r"(__val)); __val; })
#elif defined(__arm__)
# define __get_tls() ({ void** __val; __asm__("mrc p15, 0, %0, c13, c0, 3" : "=r"(__val)); __val; })
#endif

#define TLS_SLOT_ART_THREAD 7

using namespace art::mirror;

extern "C" {

    void initHideApi(JNIEnv *env);
    bool compileMethod(void *artMethod, void *thread);

    void suspendVM();
    void resumeVM();

    bool canGetObject();
    jobject getJavaObject(JNIEnv* env, void* thread, void* address);
    void *getCurrentThread();

    art::jit::JitCompiler* getGlobalJitCompiler();

    art::CompilerOptions* getCompilerOptions(art::jit::JitCompiler* compiler);

    art::CompilerOptions* getGlobalCompilerOptions();

    bool disableJitInline(art::CompilerOptions* compilerOptions);

    void* getInterpreterBridge(bool isNative);

    bool replaceUpdateCompilerOptionsQ();

    bool forceProcessProfiles();

    bool hookClassInit(void(*callback)(void*));

    JNIEnv *attachAndGetEvn();

    ArtMethod* getArtMethod(JNIEnv *env, jobject method);
}

#endif //SANDHOOK_HIDE_API_H
