//
// Created by swift on 2019/2/3.
//

#ifndef SANDHOOK_OFFSET_H
#define SANDHOOK_OFFSET_H

#include <unistd.h>

namespace SandHook {

    class Offset {
    public:

        template<typename T>
        static int findOffset(void *start, size_t len, size_t step, T value);

        template<typename T>
        static int findOffsetWithCB1(void *start, size_t len, size_t step, bool func(int, T));

        template<typename T>
        static int findOffsetWithCB2(void *start1, void *start2, size_t len, size_t step, bool func(T, T));

    };

}

#endif //SANDHOOK_OFFSET_H
