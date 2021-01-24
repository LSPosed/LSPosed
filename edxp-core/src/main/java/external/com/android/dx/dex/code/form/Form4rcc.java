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
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.util.AnnotatedOutput;

/**
 * Instruction format {@code 4rcc}. See the instruction format spec
 * for details.
 */
public final class Form4rcc extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form4rcc();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form4rcc() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        return regRangeString(insn.getRegisters()) + ", " +
            insn.cstString();
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
        int sz = regs.size();
        if (sz == 0) {
            return true;
        }

        return (unsignedFitsInByte(regs.getWordCount()) &&
                unsignedFitsInShort(sz) &&
                unsignedFitsInShort(regs.get(0).getReg()) &&
                isRegListSequential(regs));
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        MultiCstInsn mci = (MultiCstInsn) insn;
        short regB = (short) mci.getIndex(0);  // B is the method index
        short regH = (short) mci.getIndex(1);  // H is the call site proto index

        RegisterSpecList regs = insn.getRegisters();
        short regC = 0;
        if (regs.size() > 0) {
            regC = (short) regs.get(0).getReg();
        }
        int regA = regs.getWordCount();

        // The output format is: AA|op BBBB CCCC HHHH
        write(out, opcodeUnit(insn,regA), regB, regC, regH);
    }
}
