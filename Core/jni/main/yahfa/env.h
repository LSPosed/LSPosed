//
// Created by liuruikai756 on 05/07/2017.
//

#ifndef YAHFA_ENV_H
#define YAHFA_ENV_H

#define ANDROID_L 21
#define ANDROID_L2 22
#define ANDROID_M 23
#define ANDROID_N 24
#define ANDROID_N2 25
#define ANDROID_O 26
#define ANDROID_O2 27
#define ANDROID_P 28

#define roundUpTo4(v) ((v+4-1) - ((v+4-1)&3))
#define roundUpTo8(v) ((v+8-1) - ((v+8-1)&7))

#if defined(__i386__) || defined(__arm__)
#define pointer_size 4
#define readAddr(addr) read32(addr)
#define roundUpToPtrSize(x) roundUpTo4(x)
#elif defined(__aarch64__) || defined(__x86_64__)
#define pointer_size 8
#define readAddr(addr) read64(addr)
#define roundUpToPtrSize(x) roundUpTo8(x)
#else
#error Unsupported architecture
#endif

#endif //YAHFA_ENV_H
