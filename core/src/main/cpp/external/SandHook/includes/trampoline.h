//
// Created by SwiftGan on 2019/1/17.
//

#ifndef SANDHOOK_TRAMPOLINE_H
#define SANDHOOK_TRAMPOLINE_H

#include <cstdint>
#include <string.h>
#include "arch.h"
#include "arch_base.h"
#include "stdlib.h"
#include <sys/mman.h>

#define Code unsigned char *

#if defined(__i386__)
#define SIZE_REPLACEMENT_HOOK_TRAMPOLINE 4 * 9
#define OFFSET_REPLACEMENT_ADDR_ART_METHOD 4 * 5
#define OFFSET_REPLACEMENT_OFFSET_ENTRY_CODE 4 * 7

#define SIZE_DIRECT_JUMP_TRAMPOLINE 4 * 4
#define OFFSET_JUMP_ADDR_TARGET 4 * 2

#define SIZE_INLINE_HOOK_TRAMPOLINE 4 * 24
#define OFFSET_INLINE_ADDR_ORIGIN_METHOD 4 * 10
#define OFFSET_INLINE_ORIGIN_CODE 4 * 12
#define OFFSET_INLINE_OFFSET_ENTRY_CODE 4 * 20
#define OFFSET_INLINE_ADDR_HOOK_METHOD 4 * 22
#elif defined(__x86_64__)
#define SIZE_REPLACEMENT_HOOK_TRAMPOLINE 4 * 9
#define OFFSET_REPLACEMENT_ADDR_ART_METHOD 4 * 5
#define OFFSET_REPLACEMENT_OFFSET_ENTRY_CODE 4 * 7

#define SIZE_DIRECT_JUMP_TRAMPOLINE 4 * 4
#define OFFSET_JUMP_ADDR_TARGET 4 * 2

#define SIZE_INLINE_HOOK_TRAMPOLINE 4 * 24
#define OFFSET_INLINE_ADDR_ORIGIN_METHOD 4 * 10
#define OFFSET_INLINE_ORIGIN_CODE 4 * 12
#define OFFSET_INLINE_OFFSET_ENTRY_CODE 4 * 20
#define OFFSET_INLINE_ADDR_HOOK_METHOD 4 * 22
#elif defined(__arm__)
#define SIZE_REPLACEMENT_HOOK_TRAMPOLINE 4 * 5
#define OFFSET_REPLACEMENT_ART_METHOD 4 * 3
#define OFFSET_REPLACEMENT_OFFSET_CODE_ENTRY 4 * 4

#define SIZE_DIRECT_JUMP_TRAMPOLINE 4 * 2
#define OFFSET_JUMP_ADDR_TARGET 4 * 1

#define SIZE_INLINE_HOOK_TRAMPOLINE 4 * 17
#define OFFSET_INLINE_ORIGIN_CODE 4 * 6
#define OFFSET_INLINE_ORIGIN_ART_METHOD 4 * 13
#define OFFSET_INLINE_ADDR_ORIGIN_CODE_ENTRY 4 * 14
#define OFFSET_INLINE_HOOK_ART_METHOD 4 * 15
#define OFFSET_INLINE_ADDR_HOOK_CODE_ENTRY 4 * 16
#define OFFSET_INLINE_OP_ORIGIN_OFFSET_CODE 4 * 11

#define SIZE_CALL_ORIGIN_TRAMPOLINE 4 * 4
#define OFFSET_CALL_ORIGIN_ART_METHOD 4 * 2
#define OFFSET_CALL_ORIGIN_JUMP_ADDR 4 * 3

#define SIZE_ORIGIN_PLACE_HOLDER 4 * 3
#elif defined(__aarch64__)
#define SIZE_REPLACEMENT_HOOK_TRAMPOLINE 4 * 8
#define OFFSET_REPLACEMENT_ART_METHOD 4 * 4
#define OFFSET_REPLACEMENT_OFFSET_CODE_ENTRY 4 * 6

#define SIZE_DIRECT_JUMP_TRAMPOLINE 4 * 4
#define OFFSET_JUMP_ADDR_TARGET 4 * 2

#define SIZE_INLINE_HOOK_TRAMPOLINE 4 * 23
#define OFFSET_INLINE_ORIGIN_CODE 4 * 7
#define OFFSET_INLINE_ORIGIN_ART_METHOD 4 * 15
#define OFFSET_INLINE_ADDR_ORIGIN_CODE_ENTRY 4 * 17
#define OFFSET_INLINE_HOOK_ART_METHOD 4 * 19
#define OFFSET_INLINE_ADDR_HOOK_CODE_ENTRY 4 * 21

#define SIZE_CALL_ORIGIN_TRAMPOLINE 4 * 7
#define OFFSET_CALL_ORIGIN_ART_METHOD 4 * 3
#define OFFSET_CALL_ORIGIN_JUMP_ADDR 4 * 5

#define SIZE_ORIGIN_PLACE_HOLDER 4 * 4
#else
#endif

extern "C" void DIRECT_JUMP_TRAMPOLINE();
extern "C" void INLINE_HOOK_TRAMPOLINE();
extern "C" void REPLACEMENT_HOOK_TRAMPOLINE();
extern "C" void CALL_ORIGIN_TRAMPOLINE();

#if defined(__arm__)
#include <unistd.h>
extern "C" void DIRECT_JUMP_TRAMPOLINE_T();
extern "C" void INLINE_HOOK_TRAMPOLINE_T();
extern "C" void CALL_ORIGIN_TRAMPOLINE_T();
#endif


namespace SandHook {

    //deal with little or big edn
    union Code32Bit {
        uint32_t code;
        struct {
            uint32_t op1:8;
            uint32_t op2:8;
            uint32_t op3:8;
            uint32_t op4:8;
        } op;
    };

    class Trampoline {
    public:
        Code code;

        Trampoline() = default;

        virtual void init() {
            codeLen = codeLength();
            tempCode = templateCode();
        }

        void setThumb(bool thumb) {
            isThumb = thumb;
        }

        bool isThumbCode() {
            return isThumb;
        }

        void setExecuteSpace(Code start) {
            code = start;
            memcpy(code, tempCode, codeLen);
            flushCache(reinterpret_cast<Size>(code), codeLen);
        }

        void setEntryCodeOffset(Size offSet) {
            this->codeEntryOffSet = offSet;
        }

        void codeCopy(Code src, Size targetOffset, Size len) {
            memcpy(reinterpret_cast<void*>((Size)code + targetOffset), src, len);
            flushCache((Size)code + targetOffset, len);
        }

        static bool flushCache(Size addr, Size len) {
            #if defined(__arm__)
            //clearCacheArm32(reinterpret_cast<char*>(addr), reinterpret_cast<char*>(addr + len));
            int i = cacheflush(addr, addr + len, 0);
            if (i == -1) {
                return false;
            }
            return true;
            #elif defined(__aarch64__)
            char *begin = reinterpret_cast<char *>(addr);
            __builtin___clear_cache(begin, begin + len);
            #endif
            return true;
        }

        void clone(Code dest) {
            memcpy(dest, code, codeLen);
        }

        Code getCode() {
            if (isThumbCode()) {
                return getThumbCodePcAddress(code);
            } else {
                return code;
            }
        }

        Size getCodeLen() {
            return codeLen;
        }

        bool isBigEnd(void) {
            int i = 1;
            unsigned char *pointer;
            pointer = (unsigned char *) &i;
            return *pointer == 0;
        }

        //tweak imm of a 32bit asm code
        void tweakOpImm(Size codeOffset, unsigned char imm) {
            Code32Bit code32Bit;
            code32Bit.code = *reinterpret_cast<uint32_t*>(((Size)code + codeOffset));
            if (isBigEnd()) {
                code32Bit.op.op2 = imm;
            } else {
                code32Bit.op.op3 = imm;
            }
            codeCopy(reinterpret_cast<Code>(&code32Bit.code), codeOffset, 4);
            flushCache((Size)code + codeOffset, 4);
        }

        //work for thumb
        static Code getThumbCodeAddress(Code code) {
            Size addr = reinterpret_cast<Size>(code) & (~0x00000001);
            return reinterpret_cast<Code>(addr);
        }

        static Code getThumbCodePcAddress(Code code) {
            Size addr = reinterpret_cast<Size>(code) & (~0x00000001);
            return reinterpret_cast<Code>(addr + 1);
        }

        void* getEntryCodeAddr(void* method) {
            return reinterpret_cast<void*>((Size)method + codeEntryOffSet);
        }

        Code getEntryCode(void* method) {
            Code entryCode = *reinterpret_cast<Code*>((Size)method + codeEntryOffSet);
            return entryCode;
        }

    protected:
        virtual Size codeLength() = 0;
        virtual Code templateCode() = 0;
    private:
        Code tempCode;
        Size codeLen;
        Size codeEntryOffSet;
        bool isThumb = false;
    };

}


#endif //SANDHOOK_TRAMPOLINE_H
