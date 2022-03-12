#pragma once

#include <stddef.h>  // for size_t
#include <unistd.h>  // for TEMP_FAILURE_RETRY
#include <utility>
// The arraysize(arr) macro returns the # of elements in an array arr.
// The expression is a compile-time constant, and therefore can be
// used in defining new arrays, for example.  If you use arraysize on
// a pointer by mistake, you will get a compile-time error.
template<typename T, size_t N>
[[gnu::always_inline]] constexpr inline size_t arraysize(T(&)[N]) {
  return N;
}
