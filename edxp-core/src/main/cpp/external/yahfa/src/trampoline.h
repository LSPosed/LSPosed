//
// Created by liuruikai756 on 05/07/2017.
//

#ifndef YAHFA_TAMPOLINE_H
#define YAHFA_TAMPOLINE_H

extern int SDKVersion;

extern _Thread_local unsigned int hookCap; // capacity for trampolines
extern _Thread_local unsigned int hookCount; // current count of used trampolines

extern unsigned char trampoline[];

int doInitHookCap();
void setupTrampoline(uint8_t offset);
void *genTrampoline(void *toMethod, void *entrypoint);

#endif //YAHFA_TAMPOLINE_H
