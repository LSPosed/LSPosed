/*
 * Copyright (C) 2017 The Android Open Source Project
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

import external.com.android.dx.dex.code.DalvInsn;
import external.com.android.dx.dex.code.InsnFormat;
import external.com.android.dx.dex.code.MultiCstInsn;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 45cc}. See the instruction format spec
 * for details.
 */
public final class Form45cc extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form45cc();

    /** Maximal number of operands */
    private static final int MAX_NUM_OPS = 5;

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form45cc() {
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
        return 4;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof MultiCstInsn)) {
            return false;
        }

        MultiCstInsn mci = (MultiCstInsn) insn;
        if (mci.getNumberOfConstants() != 2) {
            return false;
        }

        int methodIdx = mci.getIndex(0);
        int protoIdx = mci.getIndex(1);
        if (!unsignedFitsInShort(methodIdx) || !unsignedFitsInShort(protoIdx)) {
            return false;
        }

        Constant methodRef = mci.getConstant(0);
        if (!(methodRef instanceof CstMethodRef)) {
            return false;
        }

        Constant protoRef = mci.getConstant(1);
        if (!(protoRef instanceof CstProtoRef)) {
            return false;
        }

        RegisterSpecList regs = mci.getRegisters();
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
        MultiCstInsn mci = (MultiCstInsn) insn;
        short regB = (short) mci.getIndex(0);  // B is the method index
        short regH = (short) mci.getIndex(1);  // H is the call site proto index

        RegisterSpecList regs = explicitize(insn.getRegisters());
        int regA = regs.size();
        int regC = (regA > 0) ? regs.get(0).getReg() : 0;
        int regD = (regA > 1) ? regs.get(1).getReg() : 0;
        int regE = (regA > 2) ? regs.get(2).getReg() : 0;
        int regF = (regA > 3) ? regs.get(3).getReg() : 0;
        int regG = (regA > 4) ? regs.get(4).getReg() : 0;

        // The output format is: A|G|op BBBB F|E|D|C HHHHH
        write(out,
              opcodeUnit(insn, makeByte(regG, regA)),
              regB,
              codeUnit(regC, regD, regE, regF),
              regH);
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
