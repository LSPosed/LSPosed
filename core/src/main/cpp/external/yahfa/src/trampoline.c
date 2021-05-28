//
// Created by liuruikai756 on 05/07/2017.
//
#include <sys/mman.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <malloc.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/syscall.h>

#include "common.h"
#include "trampoline.h"

// trampoline:
// 1. set eax/r0/x0 to the hook ArtMethod addr
// 2. jump into its entry point
#if defined(__i386__)
// b8 78 56 34 12 ; mov eax, 0x12345678 (addr of the hook method)
// ff 70 20 ; push DWORD PTR [eax + 0x20]
// c3 ; ret
unsigned char trampoline[] = {
        0xb8, 0x78, 0x56, 0x34, 0x12,
        0xff, 0x70, 0x20,
        0xc3
};

#elif defined(__x86_64__)
// 48 bf 78 56 34 12 78 56 34 12 ; movabs rdi, 0x1234567812345678
// ff 77 20 ; push QWORD PTR [rdi + 0x20]
// c3 ; ret
unsigned char trampoline[] = {
    0x48, 0xbf, 0x78, 0x56, 0x34, 0x12, 0x78, 0x56, 0x34, 0x12,
    0xff, 0x77, 0x20,
    0xc3
};

#elif defined(__arm__)
// 00 00 9F E5 ; ldr r0, [pc, #0]
// 20 F0 90 E5 ; ldr pc, [r0, 0x20]
// 78 56 34 12 ; 0x12345678 (addr of the hook method)
unsigned char trampoline[] = {
        0x00, 0x00, 0x9f, 0xe5,
        0x20, 0xf0, 0x90, 0xe5,
        0x78, 0x56, 0x34, 0x12
};

#elif defined(__aarch64__)
// 60 00 00 58 ; ldr x0, 12
// 10 00 40 F8 ; ldr x16, [x0, #0x00]
// 00 02 1f d6 ; br x16
// 78 56 34 12
// 89 67 45 23 ; 0x2345678912345678 (addr of the hook method)
unsigned char trampoline[] = {
        0x60, 0x00, 0x00, 0x58,
        0x10, 0x00, 0x40, 0xf8,
        0x00, 0x02, 0x1f, 0xd6,
        0x78, 0x56, 0x34, 0x12,
        0x89, 0x67, 0x45, 0x23
};
#endif
static unsigned int trampolineSize = roundUpToPtrSize(sizeof(trampoline));

static inline void FlushCache(void *addr, size_t size) {
    __builtin___clear_cache((char *) addr, (char *) ((uintptr_t) addr + size));
}

void *genTrampoline(void *hookMethod) {
    unsigned char *targetAddr = mmap(NULL, trampolineSize, PROT_READ | PROT_WRITE | PROT_EXEC,
                              MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
    if (targetAddr == MAP_FAILED) {
        LOGE("mmap failed, errno = %s", strerror(errno));
        return NULL;
    }
    memcpy(targetAddr, trampoline,
           sizeof(trampoline)); // do not use trampolineSize since it's a rounded size

    // replace with the actual ArtMethod addr
#if defined(__i386__)
    memcpy(targetAddr+1, &hookMethod, pointer_size);

#elif defined(__x86_64__)
    memcpy((char*)targetAddr + 2, &hookMethod, pointer_size);

#elif defined(__arm__)
    memcpy(targetAddr+8, &hookMethod, pointer_size);

#elif defined(__aarch64__)
    memcpy(targetAddr + 12, &hookMethod, pointer_size);

#else
#error Unsupported architecture
#endif
    FlushCache(targetAddr, sizeof(trampoline));

    return targetAddr;
}

void setupTrampoline() {
#if defined(__i386__)
    trampoline[7] = (unsigned char)OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
#elif defined(__x86_64__)
    trampoline[12] = (unsigned char)OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
#elif defined(__arm__)
    trampoline[4] = (unsigned char)OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod;
#elif defined(__aarch64__)
    trampoline[5] |=
            ((unsigned char) OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod) << 4;
    trampoline[6] |=
            ((unsigned char) OFFSET_entry_point_from_quick_compiled_code_in_ArtMethod) >> 4;
#else
#error Unsupported architecture
#endif
}
