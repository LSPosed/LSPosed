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

#pragma once

#include "macros.h"
#include <dlfcn.h>
#include <sys/mman.h>
#include "config.h"
#include "native_hook.h"
#include "elf_util.h"
#include <concepts>

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

#define CONCATENATE(a, b) a##b

#define CREATE_HOOK_STUB_ENTRIES(SYM, RET, FUNC, PARAMS, DEF)                               \
  inline static struct : public lspd::Hooker<RET PARAMS, decltype(CONCATENATE(SYM,_tstr))>{ \
    inline static RET replace PARAMS DEF                                                    \
  } FUNC

#define CREATE_MEM_HOOK_STUB_ENTRIES(SYM, RET, FUNC, PARAMS, DEF)                              \
  inline static struct : public lspd::MemHooker<RET PARAMS, decltype(CONCATENATE(SYM,_tstr))>{  \
    inline static RET replace PARAMS DEF                                                       \
  } FUNC

#define RETRIEVE_FUNC_SYMBOL(name, ...) \
        name##Sym = reinterpret_cast<name##Type>( \
                lspd::Dlsym(handle, __VA_ARGS__))

#define RETRIEVE_MEM_FUNC_SYMBOL(name, ...) \
        name##Sym = reinterpret_cast<name##Type::FunType>( \
                lspd::Dlsym(handle, __VA_ARGS__))

#define RETRIEVE_FIELD_SYMBOL(name, ...) \
        void *name = lspd::Dlsym(handle, __VA_ARGS__)

#define CREATE_FUNC_SYMBOL_ENTRY(ret, func, ...) \
        typedef ret (*func##Type)(__VA_ARGS__); \
        inline static ret (*func##Sym)(__VA_ARGS__); \
        inline static ret func(__VA_ARGS__)

#define CREATE_MEM_FUNC_SYMBOL_ENTRY(ret, func, thiz, ...) \
        using func##Type = lspd::MemberFunction<ret(__VA_ARGS__)>; \
        inline static func##Type func##Sym; \
        inline static ret func(thiz, ## __VA_ARGS__)

namespace lspd {

    class ShadowObject {

    public:
        ShadowObject(void *thiz) : thiz_(thiz) {
        }

        [[gnu::always_inline]]
        inline void *Get() {
            return thiz_;
        }

        [[gnu::always_inline]]
        inline void Reset(void *thiz) {
            thiz_ = thiz;
        }

        [[gnu::always_inline]]
        inline operator bool() const {
            return thiz_ != nullptr;
        }

    protected:
        void *thiz_;
    };

    class HookedObject : public ShadowObject {

    public:

        HookedObject(void *thiz) : ShadowObject(thiz) {}
    };

    struct ObjPtr {
        void *data;
    };

    [[gnu::always_inline]]
    inline void *Dlsym(void *handle, const char *name) {
        return dlsym(handle, name);
    }

    [[gnu::always_inline]]
    inline void *Dlsym(const SandHook::ElfImg &handle, const char *name) {
        return handle.getSymbAddress<void *>(name);
    }

    template<class H, class T, class ... Args>
    inline void *Dlsym(H &&handle, T first, Args... last) {
        auto ret = Dlsym(std::forward<H>(handle), first);
        if (ret) {
            return ret;
        }
        return Dlsym(std::forward<H>(handle), last...);
    }

    inline int HookFunction(void *original, void *replace, void **backup) {
        _make_rwx(original, _page_size);
        if constexpr (isDebug) {
            Dl_info info;
            if (dladdr(original, &info))
                LOGD("Hooking %s (%p) from %s (%p)",
                     info.dli_sname ? info.dli_sname : "(unknown symbol)", info.dli_saddr,
                     info.dli_fname ? info.dli_fname : "(unknown file)", info.dli_fbase);
        }
        return DobbyHook(original, replace, backup);
    }

    inline int UnhookFunction(void *original) {
        if constexpr (isDebug) {
            Dl_info info;
            if (dladdr(original, &info))
                LOGD("Unhooking %s (%p) from %s (%p)",
                     info.dli_sname ? info.dli_sname : "(unknown symbol)", info.dli_saddr,
                     info.dli_fname ? info.dli_fname : "(unknown file)", info.dli_fbase);
        }
        return DobbyDestroy(original);
    }

    template<class, template<class, class...> class>
    struct is_instance : public std::false_type {
    };

    template<class...Ts, template<class, class...> class U>
    struct is_instance<U<Ts...>, U> : public std::true_type {
    };

    template<typename Class, typename Return, typename T, typename... Args>
    requires (std::is_same_v<T, void> || std::is_same_v<Class, T>)
    inline static auto memfun_cast(Return (*func)(T *, Args...)) {
        union {
            Return (Class::*f)(Args...);

            struct {
                decltype(func) p;
                std::ptrdiff_t adj;
            } data;
        } u{.data = {func, 0}};
        static_assert(sizeof(u.f) == sizeof(u.data), "Try different T");
        return u.f;
    }

    template<std::same_as<void> T, typename Return, typename... Args>
    inline auto memfun_cast(Return (*func)(T *, Args...)) {
        return memfun_cast<T>(func);
    }

    template<typename, typename=void>
    class MemberFunction;

    template<typename This, typename Return, typename ... Args>
    class MemberFunction<Return(Args...), This> {
        using SelfType = MemberFunction<Return(This *, Args...), This>;
        using ThisType = std::conditional_t<std::is_same_v<This, void>, SelfType, This>;
        using MemFunType = Return(ThisType::*)(Args...);
    public:
        using FunType = Return (*)(This *, Args...);
    private:
        MemFunType f_ = nullptr;
    public:
        MemberFunction() = default;

        MemberFunction(FunType f) : f_(memfun_cast<ThisType>(f)) {}

        MemberFunction(MemFunType f) : f_(f) {}

        Return operator()(This *thiz, Args... args) {
            return (reinterpret_cast<ThisType *>(thiz)->*f_)(std::forward<Args>(args)...);
        }

        inline operator bool() {
            return f_ != nullptr;
        }
    };

    // deduction guide
    template<typename This, typename Return, typename...Args>
    MemberFunction(Return(*f)(This *, Args...)) -> MemberFunction<Return(Args...), This>;

    template<typename This, typename Return, typename...Args>
    MemberFunction(Return(This::*f)(Args...)) -> MemberFunction<Return(Args...), This>;

    template<typename, typename>
    struct Hooker;

    template<typename Ret, typename... Args, char... cs>
    struct Hooker<Ret(Args...), tstring<cs...>> {
        inline static Ret (*backup)(Args...) = nullptr;

        inline static constexpr const char *sym = tstring<cs...>::c_str();
    };

    template<typename, typename>
    struct MemHooker;
    template<typename Ret, typename This, typename... Args, char... cs>
    struct MemHooker<Ret(This, Args...), tstring<cs...>> {
        inline static MemberFunction<Ret(Args...)> backup;
        inline static constexpr const char *sym = tstring<cs...>::c_str();
    };

    template<typename T>
    concept HookerType = requires(T a) {
        a.backup;
        a.replace;
    };

    template<HookerType T>
    inline static bool HookSymNoHandle(void *original, T &arg) {
        if (original) {
            if constexpr(is_instance<decltype(arg.backup), MemberFunction>::value) {
                void *backup;
                HookFunction(original, reinterpret_cast<void *>(arg.replace), &backup);
                arg.backup = reinterpret_cast<typename decltype(arg.backup)::FunType>(backup);
            } else {
                HookFunction(original, reinterpret_cast<void *>(arg.replace),
                             reinterpret_cast<void **>(&arg.backup));
            }
            return true;
        } else {
            return false;
        }
    }

    template<typename H, HookerType T>
    inline static bool HookSym(H &&handle, T &arg) {
        auto original = Dlsym(std::forward<H>(handle), arg.sym);
        return HookSymNoHandle(original, arg);
    }

    template<typename H, HookerType T, HookerType...Args>
    inline static bool HookSyms(H &&handle, T &first, Args &...rest) {
        if (!(HookSym(std::forward<H>(handle), first) || ... || HookSym(std::forward<H>(handle),
                                                                        rest))) {
            LOGW("Hook Fails: %s", first.sym);
            return false;
        }
        return true;
    }

} // namespace lspd

using lspd::operator ""_tstr;
