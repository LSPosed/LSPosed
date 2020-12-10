//
// Created by 甘尧 on 2019/2/23.
//

#ifndef SANDHOOK_ART_RUNTIME_H
#define SANDHOOK_ART_RUNTIME_H

#include "art_jit.h"

namespace art {
    class Runtime {

    public:
        jit::Jit* getJit();
    };
}

#endif //SANDHOOK_ART_RUNTIME_H
