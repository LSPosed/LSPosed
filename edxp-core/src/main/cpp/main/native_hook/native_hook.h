#ifndef HOOK_H
#define HOOK_H

#include <xhook.h>

#if defined(__LP64__)
static constexpr const char *kLibArtPath = "/system/lib64/libart.so";
static constexpr const char *kLibWhalePath = "/system/lib64/libwhale.edxp.so";
#else
static constexpr const char *kLibArtPath = "/system/lib/libart.so";
static constexpr const char *kLibWhalePath = "/system/lib/libwhale.edxp.so";
#endif

#define XHOOK_REGISTER(NAME) \
    if (xhook_register(".*", #NAME, (void*) new_##NAME, (void **) &old_##NAME) != 0) \
        LOGE("failed to register hook " #NAME "."); \

#define NEW_FUNC_DEF(ret, func, ...) \
    static ret (*old_##func)(__VA_ARGS__); \
    static ret new_##func(__VA_ARGS__)

class ScopedSuspendAll {
};

extern void (*suspendAll)(ScopedSuspendAll *, const char *, bool);

extern void (*resumeAll)(ScopedSuspendAll *);

extern int waitGc(int, void *);

void install_inline_hooks();

void deoptimize_method(JNIEnv *env, jclass clazz, jobject method);

#endif // HOOK_H
