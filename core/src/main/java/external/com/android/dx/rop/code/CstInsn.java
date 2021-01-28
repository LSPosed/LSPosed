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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.cst.Constant;

/**
 * Instruction which contains an explicit reference to a constant.
 */
public abstract class CstInsn
        extends Insn {
    /** {@code non-null;} the constant */
    private final Constant cst;

    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param result {@code null-ok;} spec for the result, if any
     * @param sources {@code non-null;} specs for all the sources
     * @param cst {@code non-null;} constant
     */
    public CstInsn(Rop opcode, SourcePosition position, RegisterSpec result,
                   RegisterSpecList sources, Constant cst) {
        super(opcode, position, result, sources);

        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        this.cst = cst;
    }

    /** {@inheritDoc} */
    @Override
    public String getInlineString() {
        return cst.toHuman();
    }

    /**
     * Gets the constant.
     *
     * @return {@code non-null;} the constant
     */
    public Constant getConstant() {
        return cst;
    }

    /** {@inheritDoc} */
    @Override
    public boolean contentEquals(Insn b) {
        /*
         * The cast (CstInsn)b below should always succeed since
         * Insn.contentEquals compares classes of this and b.
         */
        return super.contentEquals(b)
                && cst.equals(((CstInsn)b).getConstant());
    }
}
