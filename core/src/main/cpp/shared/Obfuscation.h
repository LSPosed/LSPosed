//
// Created by Kotori0 on 2021/12/2.
//

#ifndef LSPOSED_OBFUSCATION_H
#define LSPOSED_OBFUSCATION_H
#include "slicer/writer.h"

using ustring = std::basic_string<unsigned char>;

class Obfuscation {
public:
    static ustring obfuscateDex(void* dex, size_t size);
};


#endif //LSPOSED_OBFUSCATION_H