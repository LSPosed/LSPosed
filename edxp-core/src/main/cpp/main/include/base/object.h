
#pragma once

#include <art/base/macros.h>
#include <dlfcn.h>
#include <sys/mman.h>

#define _uintval(p)               reinterpret_cast<uintptr_t>(p)
#define _ptr(p)                   reinterpret_cast<void *>(p)
#define _align_up(x, n)           (((x) + ((n) - 1)) & ~((n) - 1))
#define _align_down(x, n)         ((x) & -(n))
#define _page_size                4096
#define _page_align(n)            _align_up(static_cast<uintptr_t>(n), _page_size)
#define _ptr_align(x)             _ptr(_align_down(reinterpret_cast<uintptr_t>(x), _page_size))
#define _make_rwx(p, n)           ::mprotect(_ptr_align(p), \
                                              _page_align(_uintval(p) + n) != _page_align(_uintval(p)) ? _page_align(n) + _page_size : _page_align(n), \
                                              PROT_READ | PROT_WRITE | PROT_EXEC)

typedef void (*HookFunType)(void *, void *, void **);

#define HOOK_FUNC(func, ...) \
        edxp::HookSyms(handle, hook_func, \
            reinterpret_cast<void *>(func##Replace), \
            reinterpret_cast<void **>(&func##Backup), \
            __VA_ARGS__)

#define CREATE_HOOK_STUB_ENTRIES(ret, func, ...) \
        inline static ret (*func##Backup)(__VA_ARGS__); \
        static ret func##Replace(__VA_ARGS__)

#define CREATE_ORIGINAL_ENTRY(ret, func, ...) \
        static ret func(__VA_ARGS__)

#define RETRIEVE_FUNC_SYMBOL(name, ...) \
        name##Sym = reinterpret_cast<name##Type>( \
                edxp::Dlsym(handle, __VA_ARGS__))

#define RETRIEVE_FIELD_SYMBOL(name, ...) \
        void *name = edxp::Dlsym(handle, __VA_ARGS__)

#define CREATE_FUNC_SYMBOL_ENTRY(ret, func, ...) \
        typedef ret (*func##Type)(__VA_ARGS__); \
        inline static ret (*func##Sym)(__VA_ARGS__); \
        ALWAYS_INLINE static ret func(__VA_ARGS__)

namespace edxp {

    class ShadowObject {

    public:
        ShadowObject(void *thiz) : thiz_(thiz) {
        }

        ALWAYS_INLINE inline void *Get() {
            return thiz_;
        }

        ALWAYS_INLINE inline void Reset(void *thiz) {
            thiz_ = thiz;
        }

        ALWAYS_INLINE inline operator bool() const {
            return thiz_ != nullptr;
        }

    protected:
        void *thiz_;
    };

    class HookedObject : public ShadowObject {

    public:

        HookedObject(void *thiz) : ShadowObject(thiz) {}

        static void SetupSymbols(void *handle) {

        }

        static void SetupHooks(void *handle, HookFunType hook_fun) {

        }
    };

    ALWAYS_INLINE static void *Dlsym(void *handle, const char *name) {
        return dlsym(handle, name);
    }

    template<class T, class ... Args>
    static void *Dlsym(void *handle, T first, Args... last) {
        auto ret = Dlsym(handle, first);
        if (ret) {
            return ret;
        }
        return Dlsym(handle, last...);
    }

    ALWAYS_INLINE inline static void HookFunction(HookFunType hook_fun, void *original,
                                                  void *replace, void **backup) {
        _make_rwx(original, _page_size);
        hook_fun(original, replace, backup);
    }

    inline static void *HookSym(void *handle, HookFunType hook_fun, const char *sym,
                                void *replace, void **backup) {
        auto original = Dlsym(handle, sym);
        if (original) {
            HookFunction(hook_fun, original, replace, backup);
        } else {
            LOGW("%s not found", sym);
        }
        return original;
    }

    template<class T, class ... Args>
    inline static void *HookSyms(void *handle, HookFunType hook_fun,
                                 void *replace, void **backup, T first, Args... last) {
        auto original = Dlsym(handle, first, last...);
        if (original) {
            HookFunction(hook_fun, original, replace, backup);
            return original;
        } else {
            LOGW("%s not found", first);
            return nullptr;
        }
    }

} // namespace edxp
