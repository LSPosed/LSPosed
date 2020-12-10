//
// Created by SwiftGan on 2019/4/12.
//

#ifndef SANDHOOK_SANDHOOK_H
#define SANDHOOK_SANDHOOK_H

#include <jni.h>

extern "C"
JNIEXPORT bool nativeHookNoBackup(void* origin, void* hook);

extern "C"
JNIEXPORT void* findSym(const char *elf, const char *sym_name);

#endif //SANDHOOK_SANDHOOK_H
