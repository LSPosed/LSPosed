#include "../../includes/arch_base.h"

/**
*
//aarch64 ART 寄存器使用策略


// Method register on invoke.
// 储存正在调用的代码
static const vixl::aarch64::Register kArtMethodRegister = vixl::aarch64::x0;

//参数传递
static const vixl::aarch64::Register kParameterCoreRegisters[] = {
  vixl::aarch64::x1,
  vixl::aarch64::x2,
  vixl::aarch64::x3,
  vixl::aarch64::x4,
  vixl::aarch64::x5,
  vixl::aarch64::x6,
  vixl::aarch64::x7
};

//
const vixl::aarch64::CPURegList vixl_reserved_core_registers(vixl::aarch64::ip0,
                                                             vixl::aarch64::ip1);

//浮点计算
static const vixl::aarch64::FPRegister kParameterFPRegisters[] = {
  vixl::aarch64::d0,
  vixl::aarch64::d1,
  vixl::aarch64::d2,
  vixl::aarch64::d3,
  vixl::aarch64::d4,
  vixl::aarch64::d5,
  vixl::aarch64::d6,
  vixl::aarch64::d7
};

// Thread Register.
// 线程
const vixl::aarch64::Register tr = vixl::aarch64::x19;

// Marking Register.
// GC 标记
const vixl::aarch64::Register mr = vixl::aarch64::x20;

// Callee-save registers AAPCS64, without x19 (Thread Register) (nor
// x20 (Marking Register) when emitting Baker read barriers).
const vixl::aarch64::CPURegList callee_saved_core_registers(
    vixl::aarch64::CPURegister::kRegister,
    vixl::aarch64::kXRegSize,
    ((kEmitCompilerReadBarrier && kUseBakerReadBarrier)
         ? vixl::aarch64::x21.GetCode()
         : vixl::aarch64::x20.GetCode()),
     vixl::aarch64::x30.GetCode());


结论，x16/x17

X16 = IP0
Stub 中有使用
尽量使用 X17

*
*/


#if defined(__aarch64__)

#define Reg0 x17
#define Reg1 x16
#define RegMethod x0

FUNCTION_START(REPLACEMENT_HOOK_TRAMPOLINE)
    ldr RegMethod, addr_art_method
    ldr Reg0, addr_code_entry
    ldr Reg0, [Reg0]
    br Reg0
addr_art_method:
    .long 0
    .long 0
addr_code_entry:
    .long 0
    .long 0
FUNCTION_END(REPLACEMENT_HOOK_TRAMPOLINE)

#define SIZE_JUMP #0x10
FUNCTION_START(DIRECT_JUMP_TRAMPOLINE)
    ldr Reg0, addr_target
    br Reg0
addr_target:
    .long 0
    .long 0
FUNCTION_END(DIRECT_JUMP_TRAMPOLINE)

FUNCTION_START(INLINE_HOOK_TRAMPOLINE)
    ldr Reg0, origin_art_method
    cmp RegMethod, Reg0
    bne origin_code
    ldr RegMethod, hook_art_method
    ldr Reg0, addr_hook_code_entry
    ldr Reg0, [Reg0]
    br Reg0
origin_code:
    .long 0
    .long 0
    .long 0
    .long 0
    ldr Reg0, addr_origin_code_entry
    ldr Reg0, [Reg0]
    add Reg0, Reg0, SIZE_JUMP
    br Reg0
origin_art_method:
    .long 0
    .long 0
addr_origin_code_entry:
    .long 0
    .long 0
hook_art_method:
    .long 0
    .long 0
addr_hook_code_entry:
    .long 0
    .long 0
FUNCTION_END(INLINE_HOOK_TRAMPOLINE)

FUNCTION_START(CALL_ORIGIN_TRAMPOLINE)
    ldr RegMethod, call_origin_art_method
    ldr Reg0, addr_call_origin_code
    br Reg0
call_origin_art_method:
    .long 0
    .long 0
addr_call_origin_code:
    .long 0
    .long 0
FUNCTION_END(CALL_ORIGIN_TRAMPOLINE)

#endif
