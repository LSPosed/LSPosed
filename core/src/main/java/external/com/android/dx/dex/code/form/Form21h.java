/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package external.com.android.dx.dex.code.form;

import external.com.android.dx.dex.code.CstInsn;
import external.com.android.dx.dex.code.DalvInsn;
import external.com.android.dx.dex.code.InsnFormat;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstLiteralBits;
import external.com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 21h}. See the instruction format spec
 * for details.
 */
public final class Form21h extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form21h();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form21h() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        CstLiteralBits value = (CstLiteralBits) ((CstInsn) insn).getConstant();

        return regs.get(0).regString() + ", " + literalBitsString(value);
    }

    /** {@inheritDoc} */
    @Override
    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        RegisterSpecList regs = insn.getRegisters();
        CstLiteralBits value = (CstLiteralBits) ((CstInsn) insn).getConstant();

        return
            literalBitsComment(value,
                    (regs.get(0).getCategory() == 1) ? 32 : 64);
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        if (!((insn instanceof CstInsn) &&
              (regs.size() == 1) &&
              unsignedFitsInByte(regs.get(0).getReg()))) {
            return false;
        }

        CstInsn ci = (CstInsn) insn;
        Constant cst = ci.getConstant();

        if (!(cst instanceof CstLiteralBits)) {
            return false;
        }

        CstLiteralBits cb = (CstLiteralBits) cst;

        // Where the high bits are depends on the category of the target.
        if (regs.get(0).getCategory() == 1) {
            int bits = cb.getIntBits();
            return ((bits & 0xffff) == 0);
        } else {
            long bits = cb.getLongBits();
            return ((bits & 0xffffffffffffL) == 0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(1);

        bits.set(0, unsignedFitsInByte(regs.get(0).getReg()));
        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        CstLiteralBits cb = (CstLiteralBits) ((CstInsn) insn).getConstant();
        short bits;

        // Where the high bits are depends on the category of the target.
        if (regs.get(0).getCategory() == 1) {
            bits = (short) (cb.getIntBits() >>> 16);
        } else {
            bits = (short) (cb.getLongBits() >>> 48);
        }

        write(out, opcodeUnit(insn, regs.get(0).getReg()), bits);
    }
}
