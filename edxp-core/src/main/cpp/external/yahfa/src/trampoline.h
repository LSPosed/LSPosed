//
// Created by liuruikai756 on 05/07/2017.
//

#ifndef YAHFA_TAMPOLINE_H
#define YAHFA_TAMPOLINE_H

extern int SDKVersion;

extern unsigned char trampoline[];

void* doInitHookCap(size_t cap);
void setupTrampoline(uint8_t offset);
void *genTrampoline(void *toMethod, void *entrypoint);

#endif //YAHFA_TAMPOLINE_H
