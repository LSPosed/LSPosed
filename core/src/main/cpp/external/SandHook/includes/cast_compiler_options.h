//
// Created by 甘尧 on 2019/1/12.
//

#ifndef SANDHOOK_CAST_COMPILER_OPTIONS_H
#define SANDHOOK_CAST_COMPILER_OPTIONS_H

#include "cast.h"
#include "art_compiler_options.h"

namespace SandHook {

    class CastCompilerOptions {
    public:
        static void init(JNIEnv *jniEnv);
        static IMember<art::CompilerOptions, size_t>* inlineMaxCodeUnits;
    };


}

#endif //SANDHOOK_CAST_COMPILER_OPTIONS_H


