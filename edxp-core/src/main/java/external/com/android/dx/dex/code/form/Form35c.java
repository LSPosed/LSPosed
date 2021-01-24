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
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 35c}. See the instruction format spec
 * for details.
 */
public final class Form35c extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form35c();

    /** Maximal number of operands */
    private static final int MAX_NUM_OPS = 5;

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form35c() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = explicitize(insn.getRegisters());
        return regListString(regs) + ", " + insn.cstString();
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
        return 3;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }

        CstInsn ci = (CstInsn) insn;
        int cpi = ci.getIndex();

        if (! unsignedFitsInShort(cpi)) {
            return false;
        }

        Constant cst = ci.getConstant();
        if (!((cst instanceof CstMethodRef) ||
              (cst instanceof CstType) ||
              (cst instanceof CstCallSiteRef))) {
            return false;
        }

        RegisterSpecList regs = ci.getRegisters();
        return (wordCount(regs) >= 0);
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();
        BitSet bits = new BitSet(sz);

        for (int i = 0; i < sz; i++) {
            RegisterSpec reg = regs.get(i);
            /*
             * The check below adds (category - 1) to the register, to
             * account for the fact that the second half of a
             * category-2 register has to be represented explicitly in
             * the result.
             */
            bits.set(i, unsignedFitsInNibble(reg.getReg() +
                                             reg.getCategory() - 1));
        }

        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        int cpi = ((CstInsn) insn).getIndex();
        RegisterSpecList regs = explicitize(insn.getRegisters());
        int sz = regs.size();
        int r0 = (sz > 0) ? regs.get(0).getReg() : 0;
        int r1 = (sz > 1) ? regs.get(1).getReg() : 0;
        int r2 = (sz > 2) ? regs.get(2).getReg() : 0;
        int r3 = (sz > 3) ? regs.get(3).getReg() : 0;
        int r4 = (sz > 4) ? regs.get(4).getReg() : 0;

        write(out,
              opcodeUnit(insn,
                         makeByte(r4, sz)), // encode the fifth operand here
              (short) cpi,
              codeUnit(r0, r1, r2, r3));
    }

    /**
     * Gets the number of words required for the given register list, where
     * category-2 values count as two words. Return {@code -1} if the
     * list requires more than five words or contains registers that need
     * more than a nibble to identify them.
     *
     * @param regs {@code non-null;} the register list in question
     * @return {@code >= -1;} the number of words required, or {@code -1}
     * if the list couldn't possibly fit in this format
     */
    private static int wordCount(RegisterSpecList regs) {
        int sz = regs.size();

        if (sz > MAX_NUM_OPS) {
            // It can't possibly fit.
            return -1;
        }

        int result = 0;

        for (int i = 0; i < sz; i++) {
            RegisterSpec one = regs.get(i);
            result += one.getCategory();
            /*
             * The check below adds (category - 1) to the register, to
             * account for the fact that the second half of a
             * category-2 register has to be represented explicitly in
             * the result.
             */
            if (!unsignedFitsInNibble(one.getReg() + one.getCategory() - 1)) {
                return -1;
            }
        }

        return (result <= MAX_NUM_OPS) ? result : -1;
    }

    /**
     * Returns a register list which is equivalent to the given one,
     * except that it splits category-2 registers into two explicit
     * entries. This returns the original list if no modification is
     * required
     *
     * @param orig {@code non-null;} the original list
     * @return {@code non-null;} the list with the described transformation
     */
    private static RegisterSpecList explicitize(RegisterSpecList orig) {
        int wordCount = wordCount(orig);
        int sz = orig.size();

        if (wordCount == sz) {
            return orig;
        }

        RegisterSpecList result = new RegisterSpecList(wordCount);
        int wordAt = 0;

        for (int i = 0; i < sz; i++) {
            RegisterSpec one = orig.get(i);
            result.set(wordAt, one);
            if (one.getCategory() == 2) {
                result.set(wordAt + 1,
                           RegisterSpec.make(one.getReg() + 1, Type.VOID));
                wordAt += 2;
            } else {
                wordAt++;
            }
        }

        result.setImmutable();
        return result;
    }
}
