//
// Created by liuruikai756 on 05/07/2017.
//

#ifndef YAHFA_TAMPOLINE_H
#define YAHFA_TAMPOLINE_H

extern int SDKVersion;

extern unsigned int hookCap; // capacity for trampolines
extern unsigned int hookCount; // current count of used trampolines

extern unsigned char trampoline[];

int doInitHookCap(unsigned int cap);
void setupTrampoline(uint8_t offset);
void *genTrampoline(void *toMethod, void *entrypoint);

#define DEFAULT_CAP 1 //size of each trampoline area would be no more than 4k Bytes(one page)

#endif //YAHFA_TAMPOLINE_H
