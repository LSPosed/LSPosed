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

import external.com.android.dx.dex.code.DalvInsn;
import external.com.android.dx.dex.code.InsnFormat;
import external.com.android.dx.dex.code.SimpleInsn;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 12x}. See the instruction format spec
 * for details.
 */
public final class Form12x extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form12x();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form12x() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();

        /*
         * The (sz - 2) and (sz - 1) below makes this code work for
         * both the two- and three-register ops. (See "case 3" in
         * isCompatible(), below.)
         */

        return regs.get(sz - 2).regString() + ", " +
            regs.get(sz - 1).regString();
    }

    /** {@inheritDoc} */
    @Override
    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        // This format has no comment.
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof SimpleInsn)) {
            return false;
        }

        RegisterSpecList regs = insn.getRegisters();
        RegisterSpec rs1;
        RegisterSpec rs2;

        switch (regs.size()) {
            case 2: {
                rs1 = regs.get(0);
                rs2 = regs.get(1);
                break;
            }
            case 3: {
                /*
                 * This format is allowed for ops that are effectively
                 * 3-arg but where the first two args are identical.
                 */
                rs1 = regs.get(1);
                rs2 = regs.get(2);
                if (rs1.getReg() != regs.get(0).getReg()) {
                    return false;
                }
                break;
            }
            default: {
                return false;
            }
        }

        return unsignedFitsInNibble(rs1.getReg()) &&
            unsignedFitsInNibble(rs2.getReg());
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);
        int r0 = regs.get(0).getReg();
        int r1 = regs.get(1).getReg();

        switch (regs.size()) {
          case 2: {
            bits.set(0, unsignedFitsInNibble(r0));
            bits.set(1, unsignedFitsInNibble(r1));
            break;
          }
          case 3: {
            if (r0 != r1) {
                bits.set(0, false);
                bits.set(1, false);
            } else {
                boolean dstRegComp = unsignedFitsInNibble(r1);
                bits.set(0, dstRegComp);
                bits.set(1, dstRegComp);
            }

            bits.set(2, unsignedFitsInNibble(regs.get(2).getReg()));
            break;
          }
          default: {
            throw new AssertionError();
          }
        }

        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();

        /*
         * The (sz - 2) and (sz - 1) below makes this code work for
         * both the two- and three-register ops. (See "case 3" in
         * isCompatible(), above.)
         */

        write(out, opcodeUnit(insn,
                              makeByte(regs.get(sz - 2).getReg(),
                                       regs.get(sz - 1).getReg())));
    }
}
