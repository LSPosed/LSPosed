//
// Created by 甘尧 on 2019/2/23.
//

#ifndef SANDHOOK_ART_JIT_H
#define SANDHOOK_ART_JIT_H

namespace art {
    namespace jit {

        //7.0 - 9.0
        class JitCompiler {
        public:
            virtual ~JitCompiler();
            std::unique_ptr<art::CompilerOptions> compilerOptions;
        };

        class Jit {
        public:
            //void* getCompilerOptions();
        };



    };
}

#endif //SANDHOOK_ART_JIT_H
