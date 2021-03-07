#pragma once

#include <stddef.h>  // for size_t
#include <unistd.h>  // for TEMP_FAILURE_RETRY
#include <utility>
// A macro to disallow the copy constructor and operator= functions
// This must be placed in the private: declarations for a class.
//
// For disallowing only assign or copy, delete the relevant operator or
// constructor, for example:
// void operator=(const TypeName&) = delete;
// Note, that most uses of DISALLOW_ASSIGN and DISALLOW_COPY are broken
// semantically, one should either use disallow both or neither. Try to
// avoid these in new code.
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;      \
  void operator=(const TypeName&) = delete
// A macro to disallow all the implicit constructors, namely the
// default constructor, copy constructor and operator= functions.
//
// This should be used in the private: declarations for a class
// that wants to prevent anyone from instantiating it. This is
// especially useful for classes containing only static methods.
#define DISALLOW_IMPLICIT_CONSTRUCTORS(TypeName) \
  TypeName() = delete;                           \
  DISALLOW_COPY_AND_ASSIGN(TypeName)
// The arraysize(arr) macro returns the # of elements in an array arr.
// The expression is a compile-time constant, and therefore can be
// used in defining new arrays, for example.  If you use arraysize on
// a pointer by mistake, you will get a compile-time error.
//
// One caveat is that arraysize() doesn't accept any array of an
// anonymous type or a type defined inside a function.  In these rare
// cases, you have to use the unsafe ARRAYSIZE_UNSAFE() macro below.  This is
// due to a limitation in C++'s template system.  The limitation might
// eventually be removed, but it hasn't happened yet.
// This template function declaration is used in defining arraysize.
// Note that the function doesn't need an implementation, as we only
// use its type.
template <typename T, size_t N>
char(&ArraySizeHelper(T(&array)[N]))[N];  // NOLINT(readability/casting)
#define arraysize(array) (sizeof(ArraySizeHelper(array)))
#define SIZEOF_MEMBER(t, f) sizeof(std::declval<t>().f)
// Changing this definition will cause you a lot of pain.  A majority of
// vendor code defines LIKELY and UNLIKELY this way, and includes
// this header through an indirect path.
#define LIKELY( exp )       (__builtin_expect( (exp) != 0, true  ))
#define UNLIKELY( exp )     (__builtin_expect( (exp) != 0, false ))
#define WARN_UNUSED __attribute__((warn_unused_result))
// A deprecated function to call to create a false use of the parameter, for
// example:
//   int foo(int x) { UNUSED(x); return 10; }
// to avoid compiler warnings. Going forward we prefer ATTRIBUTE_UNUSED.
template <typename... T>
void UNUSED(const T&...) {
}
// An attribute to place on a parameter to a function, for example:
//   int foo(int x ATTRIBUTE_UNUSED) { return 10; }
// to avoid compiler warnings.
#define ATTRIBUTE_UNUSED __attribute__((__unused__))
// The FALLTHROUGH_INTENDED macro can be used to annotate implicit fall-through
// between switch labels:
//  switch (x) {
//    case 40:
//    case 41:
//      if (truth_is_out_there) {
//        ++x;
//        FALLTHROUGH_INTENDED;  // Use instead of/along with annotations in
//                               // comments.
//      } else {
//        return x;
//      }
//    case 42:
//      ...
//
// As shown in the example above, the FALLTHROUGH_INTENDED macro should be
// followed by a semicolon. It is designed to mimic control-flow statements
// like 'break;', so it can be placed in most places where 'break;' can, but
// only if there are no statements on the execution path between it and the
// next switch label.
//
// When compiled with clang, the FALLTHROUGH_INTENDED macro is expanded to
// [[clang::fallthrough]] attribute, which is analysed when performing switch
// labels fall-through diagnostic ('-Wimplicit-fallthrough'). See clang
// documentation on language extensions for details:
// http://clang.llvm.org/docs/LanguageExtensions.html#clang__fallthrough
//
// When used with unsupported compilers, the FALLTHROUGH_INTENDED macro has no
// effect on diagnostics.
//
// In either case this macro has no effect on runtime behavior and performance
// of code.
#ifndef FALLTHROUGH_INTENDED
#define FALLTHROUGH_INTENDED [[clang::fallthrough]]  // NOLINT
#endif
// Current ABI string
#if defined(__arm__)
#define ABI_STRING "arm"
#elif defined(__aarch64__)
#define ABI_STRING "arm64"
#elif defined(__i386__)
#define ABI_STRING "x86"
#elif defined(__x86_64__)
#define ABI_STRING "x86_64"
#elif defined(__mips__) && !defined(__LP64__)
#define ABI_STRING "mips"
#elif defined(__mips__) && defined(__LP64__)
#define ABI_STRING "mips64"
#endif
// A macro to disallow new and delete operators for a class. It goes in the private: declarations.
// NOTE: Providing placement new (and matching delete) for constructing container elements.
#define DISALLOW_ALLOCATION() \
  public: \
    NO_RETURN ALWAYS_INLINE void operator delete(void*, size_t) { UNREACHABLE(); } \
    ALWAYS_INLINE void* operator new(size_t, void* ptr) noexcept { return ptr; } \
    ALWAYS_INLINE void operator delete(void*, void*) noexcept { } \
  private: \
    void* operator new(size_t) = delete  // NOLINT
#define ALIGNED(x) __attribute__ ((__aligned__(x)))
#define PACKED(x) __attribute__ ((__aligned__(x), __packed__))
// Stringify the argument.
#define QUOTE(x) #x
#define STRINGIFY(x) QUOTE(x)
// Append tokens after evaluating.
#define APPEND_TOKENS_AFTER_EVAL_2(a, b) a ## b
#define APPEND_TOKENS_AFTER_EVAL(a, b) APPEND_TOKENS_AFTER_EVAL_2(a, b)
#ifndef NDEBUG
#define ALWAYS_INLINE
#else
#define ALWAYS_INLINE  __attribute__ ((always_inline))
#endif
// clang doesn't like attributes on lambda functions. It would be nice to say:
//   #define ALWAYS_INLINE_LAMBDA ALWAYS_INLINE
#define ALWAYS_INLINE_LAMBDA
#define NO_INLINE __attribute__ ((noinline))
#if defined (__APPLE__)
#define HOT_ATTR
#define COLD_ATTR
#else
#define HOT_ATTR __attribute__ ((hot))
#define COLD_ATTR __attribute__ ((cold))
#endif
#define PURE __attribute__ ((__pure__))
// Define that a position within code is unreachable, for example:
//   int foo () { LOG(FATAL) << "Don't call me"; UNREACHABLE(); }
// without the UNREACHABLE a return statement would be necessary.
#define UNREACHABLE  __builtin_unreachable
// Add the C++11 noreturn attribute.
#define NO_RETURN [[ noreturn ]]  // NOLINT[whitespace/braces] [5]
// Annotalysis thread-safety analysis support. Things that are not in base.
#define LOCKABLE CAPABILITY("mutex")
#define SHARED_LOCKABLE SHARED_CAPABILITY("mutex")

// DISALLOW_COPY_AND_ASSIGN disallows the copy and operator= functions. It goes in the private:
// declarations in a class.
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;  \
  void operator=(const TypeName&) = delete
