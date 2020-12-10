//
// Created by swift on 2019/2/3.
//

#ifndef SANDHOOK_INST_VISTOR_H
#define SANDHOOK_INST_VISTOR_H

#include <cstdint>
#include "arch.h"

#define CASE(inst,mask,match,type) \
if ((inst & mask) == match) { return type; } \

namespace SandHook {

    union Arm32Code {
        uint32_t code;
        struct {
            uint32_t cond:4;
            uint32_t empty:2;
            uint32_t opcode:4;
            uint32_t s:1;
            uint32_t rn:4;
            uint32_t rd:4;
            uint32_t operand2:12;
        } units;
    };

    union Arm16Code {
        uint16_t code;
        struct {
            uint32_t cond:16;
        } units;
    };

    enum InstArch {
        ARM32 = 0,
        Thumb16,
        Thumb32,
        Arm64,
        X86,
        X64
    };

    enum class InstType_Thumb32 {
        // BLX <label>
                BLX_THUMB32 = 0,
        // BL <label>
                BL_THUMB32,
        // B.W <label>
                B1_THUMB32,
        // B.W <label>
                B2_THUMB32,
        // ADR.W Rd, <label>
                ADR1_THUMB32,
        // ADR.W Rd, <label>
                ADR2_THUMB32,
        // LDR.W Rt, <label>
                LDR_THUMB32,
        // TBB [PC, Rm]
                TBB_THUMB32,
        // TBH [PC, Rm, LSL #1]
                TBH_THUMB32,
                PC_NO_RELATED
    };

    enum class InstType_Thumb16 {
        // B <label>
                B1_THUMB16 = 0,
        // B <label>
                B2_THUMB16,
        // BX PC
                BX_THUMB16,
        // ADD <Rdn>, PC (Rd != PC, Rn != PC) 在对ADD进行修正时，
        //采用了替换PC为Rr的方法，当Rd也为PC时，由于之前更改了Rr的值，
        //可能会影响跳转后的正常功能。
                ADD_THUMB16,
        // MOV Rd, PC
                MOV_THUMB16,
        // ADR Rd, <label>
                ADR_THUMB16,
        // LDR Rt, <label>
                LDR_THUMB16,
                PC_NO_RELATED
    };

    enum class InstType_Arm64 {
        CBZ_CBNZ = 0,
        B_COND,
        TBZ_TBNZ,
        B_BL,
        LDR_LIT,
        ADR_ADRP,
        PC_NO_RELATED
    };

    class Inst {
    public:
        virtual int instLen() const = 0;

        virtual InstArch instArch() const = 0;

        virtual bool pcRelated() = 0;

        virtual Size bin() = 0;
    };

    class InstVisitor {
    public:
        virtual bool visit(Inst* inst, Size offset, Size length) = 0;
    };

    class InstDecode {
    public:
        static void decode(void* codeStart, Size codeLen, InstVisitor* visitor);
    };

}

#endif //SANDHOOK_INST_VISTOR_H
