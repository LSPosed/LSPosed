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
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;

/**
 * Instruction which contains an explicit reference to a constant
 * but which cannot throw an exception.
 */
public final class PlainCstInsn
        extends CstInsn {
    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param result {@code null-ok;} spec for the result, if any
     * @param sources {@code non-null;} specs for all the sources
     * @param cst {@code non-null;} the constant
     */
    public PlainCstInsn(Rop opcode, SourcePosition position,
                        RegisterSpec result, RegisterSpecList sources,
                        Constant cst) {
        super(opcode, position, result, sources, cst);

        if (opcode.getBranchingness() != Rop.BRANCH_NONE) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
    }

    /** {@inheritDoc} */
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Visitor visitor) {
        visitor.visitPlainCstInsn(this);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withAddedCatch(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    /** {@inheritDoc} */
    @Override
    public Insn withRegisterOffset(int delta) {
        return new PlainCstInsn(getOpcode(), getPosition(),
                                getResult().withOffset(delta),
                                getSources().withOffset(delta),
                                getConstant());
    }

    /** {@inheritDoc} */
    @Override
    public Insn withNewRegisters(RegisterSpec result,
            RegisterSpecList sources) {

        return new PlainCstInsn(getOpcode(), getPosition(),
                                result,
                                sources,
                                getConstant());

    }
}
