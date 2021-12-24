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
#include <atomic>
#include <bit>
#include <climits>

#include "common.h"
#include "trampoline.h"

static_assert(std::endian::native == std::endian::little, "Unsupported architecture");

union Trampoline {
    uintptr_t mixed;
    struct {
        unsigned count : 12;
        uintptr_t addr : sizeof(uintptr_t) * CHAR_BIT - 12;
    };
};

static_assert(sizeof(Trampoline) == sizeof(uintptr_t), "Unsupported architecture");
static_assert(std::atomic_uintptr_t ::is_always_lock_free, "Unsupported architecture");

// trampoline:
// 1. set eax/r0/x0 to the hook ArtMethod addr
// 2. jump into its entry point
#if defined(__i386__)
// b8 78 56 34 12 ; mov eax, 0x12345678 (addr of the hook method)
// ff 70 20 ; push DWORD PTR [eax + 0x20]
// c3 ; ret
unsigned char trampoline[] = "\xb8\x78\x56\x34\x12\xff\x70\x20\xc3";
#elif defined(__x86_64__)
// 48 bf 78 56 34 12 78 56 34 12 ; movabs rdi, 0x1234567812345678
// ff 77 20 ; push QWORD PTR [rdi + 0x20]
// c3 ; ret
unsigned char trampoline[] = "\x48\xbf\x78\x56\x34\x12\x78\x56\x34\x12\xff\x77\x20\xc3";

#elif defined(__arm__)
// 00 00 9F E5 ; ldr r0, [pc, #0]
// 20 F0 90 E5 ; ldr pc, [r0, 0x20]
// 78 56 34 12 ; 0x12345678 (addr of the hook method)
unsigned char trampoline[] = "\x00\x00\x9f\xe5\x20\xf0\x90\xe5\x78\x56\x34\x12";

#elif defined(__aarch64__)
// 60 00 00 58 ; ldr x0, 12
// 10 00 40 F8 ; ldr x16, [x0, #0x00]
// 00 02 1f d6 ; br x16
// 78 56 34 12
// 78 56 34 12 ; 0x1234567812345678 (addr of the hook method)
unsigned char trampoline[] = "\x60\x00\x00\x58\x10\x00\x40\xf8\x00\x02\x1f\xd6\x78\x56\x34\x12\x78\x56\x34\x12";
#endif
static std::atomic_uintptr_t trampoline_pool{Trampoline{.count = 0, .addr = 0}.mixed};
static std::atomic_flag trampoline_lock{false};
static constexpr size_t trampolineSize = roundUpToPtrSize(sizeof(trampoline));
static constexpr size_t pageSize = 4096;
static constexpr size_t trampolineNumPerPage = pageSize / trampolineSize;

static inline void FlushCache(void *addr, size_t size) {
    __builtin___clear_cache((char *) addr, (char *) ((uintptr_t) addr + size));
}

void *genTrampoline(void *hookMethod) {
    unsigned count;
    uintptr_t addr;
    while (true) {
        auto tl = Trampoline {.mixed = trampoline_pool.fetch_add(1, std::memory_order_release)};
        count = tl.count;
        addr = tl.addr;
        if (addr == 0 || count >= trampolineNumPerPage) {
            if (trampoline_lock.test_and_set(std::memory_order_acq_rel)) {
                trampoline_lock.wait(true, std::memory_order_acquire);
                continue;
            } else {
                addr = reinterpret_cast<uintptr_t>(mmap(nullptr, pageSize,
                                                        PROT_READ | PROT_WRITE | PROT_EXEC,
                                                        MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
                if (addr == reinterpret_cast<uintptr_t>(MAP_FAILED)) {
                    LOGE("mmap failed, errno = %s", strerror(errno));
                    trampoline_lock.clear(std::memory_order_release);
                    trampoline_lock.notify_all();
                    return nullptr;
                }
                count = 0;
                tl.addr = addr;
                tl.count = count + 1;
                trampoline_pool.store(tl.mixed, std::memory_order_release);
                trampoline_lock.clear(std::memory_order_release);
                trampoline_lock.notify_all();
            }
        }
        addr = addr + count * trampolineSize;
        break;
    }
    unsigned char *targetAddr = reinterpret_cast<unsigned char *>(addr);
//    unsigned char*targetAddr = reinterpret_cast<unsigned char*>(mmap(NULL, trampolineSize, PROT_READ | PROT_WRITE | PROT_EXEC,
//                                     MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
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
