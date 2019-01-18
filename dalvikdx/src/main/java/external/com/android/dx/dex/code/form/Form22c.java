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
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 22c}. See the instruction format spec
 * for details.
 */
public final class Form22c extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form22c();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form22c() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() +
            ", " + insn.cstString();
    }

    /** {@inheritDoc} */
    @Override
    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        if (noteIndices) {
            return insn.cstComment();
        } else {
            return "";
        }
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
              (regs.size() == 2) &&
              unsignedFitsInNibble(regs.get(0).getReg()) &&
              unsignedFitsInNibble(regs.get(1).getReg()))) {
            return false;
        }

        CstInsn ci = (CstInsn) insn;
        int cpi = ci.getIndex();

        if (! unsignedFitsInShort(cpi)) {
            return false;
        }

        Constant cst = ci.getConstant();
        return (cst instanceof CstType) ||
            (cst instanceof CstFieldRef);
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);

        bits.set(0, unsignedFitsInNibble(regs.get(0).getReg()));
        bits.set(1, unsignedFitsInNibble(regs.get(1).getReg()));
        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int cpi = ((CstInsn) insn).getIndex();

        write(out,
              opcodeUnit(insn,
                         makeByte(regs.get(0).getReg(), regs.get(1).getReg())),
              (short) cpi);
    }
}
