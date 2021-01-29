//
// Created by SwiftGan on 2019/2/11.
//

#if defined(__aarch64__)

#include "../includes/inst.h"
#include "../includes/trampoline.h"

namespace SandHook {

    class InstArm64 : public Inst {

    public:

        Arm32Code code;
        InstType_Arm64 instType;

        InstArm64(uint32_t code) {
            this->code.code = code;
            instType = initType();
        }

    private:

        int instLen() const override {
            return 4;
        }

        InstArch instArch() const override {
            return Arm64;
        }

        bool pcRelated() override {
            return instType < InstType_Arm64::PC_NO_RELATED;
        }

        Size bin() override {
            return code.code;
        }

    private:

        InstType_Arm64 initType() {
            CASE(code.code, 0x7e000000, 0x34000000, InstType_Arm64::CBZ_CBNZ);
            CASE(code.code, 0xff000010, 0x54000000, InstType_Arm64::B_COND);
            CASE(code.code, 0x7e000000, 0x36000000, InstType_Arm64::TBZ_TBNZ);
            CASE(code.code, 0x7c000000, 0x14000000, InstType_Arm64::B_BL);
            CASE(code.code, 0x3b000000, 0x18000000, InstType_Arm64::LDR_LIT);
            CASE(code.code, 0x1f000000, 0x10000000, InstType_Arm64::ADR_ADRP);
            return InstType_Arm64::PC_NO_RELATED;
        }

    };

    void InstDecode::decode(void *codeStart, Size codeLen, InstVisitor *visitor) {
        Size offset = 0;
        Inst *inst = nullptr;
        codeStart = Trampoline::getThumbCodeAddress(static_cast<Code>(codeStart));
        Size codeAddr = reinterpret_cast<Size>(codeStart);
        while (offset < codeLen) {
            uint32_t ram32 = *reinterpret_cast<uint32_t *>(codeAddr + offset);
            inst = new InstArm64(ram32);
            if (!visitor->visit(inst, offset, codeLen)) {
                delete inst;
                break;
            }
            offset += inst->instLen();
            delete inst;
        }
    }
}

#endif