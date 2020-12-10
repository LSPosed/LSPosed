//
// Created by swift on 2019/2/3.
//

#include "../includes/offset.h"

namespace SandHook {

    template<typename T>
    int Offset::findOffset(void *start, size_t len, size_t step, T value) {

        if (nullptr == start) {
            return -1;
        }

        for (int i = 0; i <= len; i += step) {
            T current_value = *reinterpret_cast<T *>((size_t) start + i);
            if (value == current_value) {
                return i;
            }
        }
        return -1;
    }

    template<typename T>
    int Offset::findOffsetWithCB1(void *start, size_t len, size_t step, bool func(int, T)) {

        if (nullptr == start) {
            return -1;
        }

        for (int i = 0; i <= len; i += step) {
            T current_value = *reinterpret_cast<T *>((size_t) start + i);
            if (func(i, current_value)) {
                return i;
            }
        }
        return -1;
    }

    template<typename T>
    int Offset::findOffsetWithCB2(void *start1, void *start2, size_t len, size_t step, bool func(T, T)) {

        if (nullptr == start1 || nullptr == start2) {
            return -1;
        }

        for (int i = 0; i <= len; i += step) {
            T v1 = *reinterpret_cast<T *>((size_t) start1 + i);
            T v2 = *reinterpret_cast<T *>((size_t) start2 + i);
            if (func(v1, v2)) {
                return i;
            }
        }

        return -1;
    }

}
