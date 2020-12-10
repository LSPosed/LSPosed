//
// Created by SwiftGan on 2019/2/11.
//

#if defined(__arm__)

#include "../includes/inst.h"
#include "../includes/trampoline.h"

namespace SandHook {

    class InstThumb32 : public Inst {
    public:

        Arm32Code code;
        InstType_Thumb32 instType = InstType_Thumb32::PC_NO_RELATED;

        InstThumb32(uint32_t code) {
            this->code.code = code;
            instType = initType();
        }

    private:

        int instLen() const override {
            return 4;
        }

        InstArch instArch() const override {
            return Thumb32;
        }

        bool pcRelated() override {
            return instType < InstType_Thumb32::PC_NO_RELATED;
        }

        Size bin() override {
            return code.code;
        }

        InstType_Thumb32 initType() {
            CASE(code.code, 0xF800D000, 0xF000C000, InstType_Thumb32::BLX_THUMB32)
            CASE(code.code, 0xF800D000, 0xF000D000, InstType_Thumb32::BL_THUMB32)
            CASE(code.code, 0xF800D000, 0xF0008000, InstType_Thumb32::B1_THUMB32)
            CASE(code.code, 0xF800D000, 0xF0009000, InstType_Thumb32::B2_THUMB32)
            CASE(code.code, 0xFBFF8000, 0xF2AF0000, InstType_Thumb32::ADR1_THUMB32)
            CASE(code.code, 0xFBFF8000, 0xF20F0000, InstType_Thumb32::ADR2_THUMB32)
            CASE(code.code, 0xFF7F0000, 0xF85F0000, InstType_Thumb32::LDR_THUMB32)
            CASE(code.code, 0xFFFF00F0, 0xE8DF0000, InstType_Thumb32::TBB_THUMB32)
            CASE(code.code, 0xFFFF00F0, 0xE8DF0010, InstType_Thumb32::TBH_THUMB32)
            return InstType_Thumb32::PC_NO_RELATED;
        }

    };

    class InstThumb16 : public Inst {
    public:


        Arm16Code code;
        InstType_Thumb16 instType = InstType_Thumb16::PC_NO_RELATED;

        InstThumb16(uint16_t code) {
            this->code.code = code;
            instType = initType();
        }

    private:

        int instLen() const override {
            return 2;
        }

        InstArch instArch() const override {
            return Thumb16;
        }

        bool pcRelated() override {
            return instType < InstType_Thumb16 ::PC_NO_RELATED;
        }

        Size bin() override {
            return code.code;
        }

        InstType_Thumb16 initType() {
            CASE(code.code, 0xF000, 0xD000, InstType_Thumb16::B1_THUMB16)
            CASE(code.code, 0xF800, 0xE000, InstType_Thumb16::B2_THUMB16)
            CASE(code.code, 0xFFF8, 0x4778, InstType_Thumb16::BX_THUMB16)
            CASE(code.code, 0xFF78, 0x4478, InstType_Thumb16::ADD_THUMB16)
            CASE(code.code, 0xFF78, 0x4678, InstType_Thumb16::MOV_THUMB16)
            CASE(code.code, 0xF800, 0xA000, InstType_Thumb16::ADR_THUMB16)
            CASE(code.code, 0xF800, 0x4800, InstType_Thumb16::LDR_THUMB16)
            return InstType_Thumb16::PC_NO_RELATED;
        }

    };

    bool isThumbCode(Size codeAddr) {
        return (codeAddr & 0x1) == 0x1;
    }

    bool isThumb32(uint16_t code) {
        return ((code & 0xF000) == 0xF000) || ((code & 0xF800) == 0xE800);
    }

    void InstDecode::decode(void *codeStart, Size codeLen, InstVisitor *visitor) {
        Size offset = 0;
        Inst* inst = nullptr;
        if (isThumbCode(reinterpret_cast<Size>(codeStart))) {
            codeStart = Trampoline::getThumbCodeAddress(static_cast<Code>(codeStart));
            Size codeAddr = reinterpret_cast<Size>(codeStart);
            while (offset < codeLen) {
                uint16_t ram16 = *reinterpret_cast<uint16_t*>(codeAddr + offset);
                uint32_t ram32 = *reinterpret_cast<uint32_t*>(codeAddr + offset);
                if (isThumb32(ram16)) {
                    //thumb32
                    inst = new InstThumb32(ram32);
                } else {
                    //thumb16
                    inst = new InstThumb16(ram16);
                }
                if (!visitor->visit(inst, offset, codeLen)) {
                    delete inst;
                    break;
                }
                offset += inst->instLen();
                delete inst;
            }
        }
    }
}

#endif