#include "../../includes/arch_base.h"

#if defined(__arm__)

#define Reg0 ip
//need restore
#define RegT ip
#define RegMethod r0

FUNCTION_START(REPLACEMENT_HOOK_TRAMPOLINE)
    ldr RegMethod, addr_art_method
    ldr Reg0, addr_code_entry
    ldr pc, [Reg0]
addr_art_method:
    .long 0
addr_code_entry:
    .long 0
FUNCTION_END(REPLACEMENT_HOOK_TRAMPOLINE)


#define SIZE_JUMP #0x8
FUNCTION_START(DIRECT_JUMP_TRAMPOLINE)
    ldr pc, addr_target
addr_target:
    .long 0
FUNCTION_END(DIRECT_JUMP_TRAMPOLINE)



FUNCTION_START(INLINE_HOOK_TRAMPOLINE)
    ldr Reg0, origin_art_method
    cmp RegMethod, Reg0
    bne origin_code
    ldr RegMethod, hook_art_method
    ldr Reg0, addr_hook_code_entry
    ldr pc, [Reg0]
origin_code:
    .long 0
    .long 0
    nop
    ldr Reg0, addr_origin_code_entry
    ldr Reg0, [Reg0]
    add Reg0, Reg0, SIZE_JUMP
    mov pc, Reg0
origin_art_method:
    .long 0
addr_origin_code_entry:
    .long 0
hook_art_method:
    .long 0
addr_hook_code_entry:
    .long 0
FUNCTION_END(INLINE_HOOK_TRAMPOLINE)


FUNCTION_START(CALL_ORIGIN_TRAMPOLINE)
    ldr RegMethod, origin_method
    ldr pc, addr_origin
origin_method:
    .long 0
addr_origin:
    .long 0
FUNCTION_END(CALL_ORIGIN_TRAMPOLINE)

//thumb-2
FUNCTION_START_T(DIRECT_JUMP_TRAMPOLINE_T)
    ldr pc, addr_target_t
addr_target_t:
    .long 0
FUNCTION_END(DIRECT_JUMP_TRAMPOLINE_T)


FUNCTION_START_T(INLINE_HOOK_TRAMPOLINE_T)
    //4 byte
    ldr RegT, origin_art_method_t
    //2 byte
    cmp RegMethod, RegT
    nop
    //2 byte
    bne origin_code_t
    nop
    //4 byte
    ldr RegMethod, hook_art_method_t
    //4 byte
    ldr RegT, addr_hook_code_entry_t
    //4 byte
    ldr pc, [RegT]
origin_code_t:
    //4 byte
    .long 0
    //4 byte
    .long 0
    //4byte
    nop
    nop
    //4 byte
    ldr RegT, addr_origin_code_entry_t
    //4 byte
    ldr RegT, [RegT]
    //4 byte
    add RegT, RegT, SIZE_JUMP
    //2 byte
    mov pc, RegT
    nop
origin_art_method_t:
    .long 0
addr_origin_code_entry_t:
    .long 0
hook_art_method_t:
    .long 0
addr_hook_code_entry_t:
    .long 0
FUNCTION_END(INLINE_HOOK_TRAMPOLINE_T)

FUNCTION_START_T(CALL_ORIGIN_TRAMPOLINE_T)
    ldr RegMethod, origin_method_t
    ldr pc, addr_origin_t
origin_method_t:
    .long 0
addr_origin_t:
    .long 0
FUNCTION_END(CALL_ORIGIN_TRAMPOLINE_T)

#endif