//
// Created by 甘尧 on 2019/1/12.
//

#ifndef SANDHOOK_ARCH_H
#define SANDHOOK_ARCH_H

#define BYTE_POINT sizeof(void*)

typedef size_t Size;

//32bit
#if defined(__i386__) || defined(__arm__)
//64bit
#elif defined(__aarch64__) || defined(__x86_64__)
#else
#endif

#if defined(__arm__)
static void clearCacheArm32(char* begin, char *end)
{
    const int syscall = 0xf0002;
    __asm __volatile (
        "mov     r0, %0\n"
        "mov     r1, %1\n"
        "mov     r3, %2\n"
        "mov     r2, #0x0\n"
        "svc     0x00000000\n"
        :
        :    "r" (begin), "r" (end), "r" (syscall)
        :    "r0", "r1", "r3"
        );
}
#endif

#define ANDROID_K 19
#define ANDROID_L 21
#define ANDROID_L2 22
#define ANDROID_M 23
#define ANDROID_N 24
#define ANDROID_N2 25
#define ANDROID_O 26
#define ANDROID_O2 27
#define ANDROID_P 28
#define ANDROID_Q 29
#define ANDROID_R 30

#endif //SANDHOOK_ARCH_H