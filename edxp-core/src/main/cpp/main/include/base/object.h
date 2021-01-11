#pragma once

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wgnu-string-literal-operator-template"

#include <art/base/macros.h>
#include <dlfcn.h>
#include <sys/mman.h>
#include "config.h"

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

#define CONCATENATE(a, b) a##b

#define CREATE_HOOK_STUB_ENTRIES(SYM, RET, FUNC, PARAMS, DEF)                               \
  inline static struct : public edxp::Hooker<RET PARAMS, decltype(CONCATENATE(SYM,_tstr))>{ \
    inline static RET replace PARAMS DEF                                                    \
  } FUNC

#define CREATE_MEM_HOOK_STUB_ENTRIES(SYM, RET, FUNC, PARAMS, DEF)                              \
  inline static struct : public edxp::MemHooker<RET PARAMS, decltype(CONCATENATE(SYM,_tstr))>{  \
    inline static RET replace PARAMS DEF                                                       \
  } FUNC

#define RETRIEVE_FUNC_SYMBOL(name, ...) \
        name##Sym = reinterpret_cast<name##Type>( \
                edxp::Dlsym(handle, __VA_ARGS__))

#define RETRIEVE_MEM_FUNC_SYMBOL(name, ...) \
        name##Sym = reinterpret_cast<name##Type::FunType>( \
                edxp::Dlsym(handle, __VA_ARGS__))

#define RETRIEVE_FIELD_SYMBOL(name, ...) \
        void *name = edxp::Dlsym(handle, __VA_ARGS__)

#define CREATE_FUNC_SYMBOL_ENTRY(ret, func, ...) \
        typedef ret (*func##Type)(__VA_ARGS__); \
        inline static ret (*func##Sym)(__VA_ARGS__); \
        inline static ret func(__VA_ARGS__)

#define CREATE_MEM_FUNC_SYMBOL_ENTRY(ret, func, thiz, ...) \
        using func##Type = edxp::MemberFunction<ret(__VA_ARGS__)>; \
        inline static func##Type func##Sym; \
        inline static ret func(thiz, ## __VA_ARGS__)

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

    struct ObjPtr {
        void *data;
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

    template<class, template<class, class...> class>
    struct is_instance : public std::false_type {
    };

    template<class...Ts, template<class, class...> class U>
    struct is_instance<U<Ts...>, U> : public std::true_type {
    };

    template<typename Class, typename Return, typename T, typename... Args>
    inline static auto memfun_cast(Return (*func)(T *, Args...)) {
        static_assert(std::is_same_v<T, void> || std::is_same_v<Class, T>,
                      "Not viable cast");
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

    template<typename T, typename Return, typename... Args,
            typename = std::enable_if_t<!std::is_same_v<T, void>>>
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

    template<char... chars> using tstring = std::integer_sequence<char, chars...>;

    template<typename T, T... chars>
    constexpr tstring<chars...> operator ""_tstr() {
        return {};
    }

    template<typename, typename>
    struct Hooker;

    template<typename Ret, typename... Args, char... cs>
    struct Hooker<Ret(Args...), tstring<cs...>> {
        inline static Ret (*backup)(Args...) = nullptr;

        inline static constexpr const char sym[sizeof...(cs) + 1] = {cs..., '\0'};
    };

    template<typename, typename>
    struct MemHooker;
    template<typename Ret, typename This, typename... Args, char... cs>
    struct MemHooker<Ret(This, Args...), tstring<cs...>> {
        inline static MemberFunction<Ret(Args...)> backup;
        inline static constexpr const char sym[sizeof...(cs) + 1] = {cs..., '\0'};
    };

    template<typename T>
    inline static bool HookSym(void *handle, HookFunType hook_fun, T &arg) {
        auto original = Dlsym(handle, arg.sym);
        if (original) {
            if constexpr(is_instance<decltype(arg.backup), MemberFunction>::value) {
                void *backup;
                HookFunction(hook_fun, original, reinterpret_cast<void *>(arg.replace), &backup);
                arg.backup = reinterpret_cast<typename decltype(arg.backup)::FunType>(backup);
            } else {
                HookFunction(hook_fun, original, reinterpret_cast<void *>(arg.replace),
                             reinterpret_cast<void **>(&arg.backup));
            }
            return true;
        } else {
            return false;
        }
    }

    template<typename T, typename...Args>
    inline static bool HookSyms(void *handle, HookFunType hook_fun, T &first, Args &...rest) {
        if (!(HookSym(handle, hook_fun, first) || ... || HookSym(handle, hook_fun, rest))) {
            LOGW("Hook Fails: %s", first.sym);
            return false;
        }
        return true;
    }

} // namespace edxp

using edxp::operator ""_tstr;

#pragma clang diagnostic pop
