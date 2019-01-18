/*
 * Copyright (C) 2008 The Android Open Source Project
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
import java.util.ArrayList;

/**
 * Instruction which fills a newly created array with a predefined list of
 * constant values.
 */
public final class FillArrayDataInsn
        extends Insn {

    /** non-null: initial values to fill the newly created array */
    private final ArrayList<Constant> initValues;

    /**
     * non-null: type of the array. Will be used to determine the width of
     * elements in the array-data table.
     */
    private final Constant arrayType;

    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param sources {@code non-null;} specs for all the sources
     * @param initValues {@code non-null;} list of initial values to fill the array
     * @param cst {@code non-null;} type of the new array
     */
    public FillArrayDataInsn(Rop opcode, SourcePosition position,
                             RegisterSpecList sources,
                             ArrayList<Constant> initValues,
                             Constant cst) {
        super(opcode, position, null, sources);

        if (opcode.getBranchingness() != Rop.BRANCH_NONE) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }

        this.initValues = initValues;
        this.arrayType = cst;
    }


    /** {@inheritDoc} */
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    /**
     * Return the list of init values
     * @return {@code non-null;} list of init values
     */
    public ArrayList<Constant> getInitValues() {
        return initValues;
    }

    /**
     * Return the type of the newly created array
     * @return {@code non-null;} array type
     */
    public Constant getConstant() {
        return arrayType;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Visitor visitor) {
        visitor.visitFillArrayDataInsn(this);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withAddedCatch(Type type) {
        throw new  UnsupportedOperationException("unsupported");
    }

    /** {@inheritDoc} */
    @Override
    public Insn withRegisterOffset(int delta) {
        return new FillArrayDataInsn(getOpcode(), getPosition(),
                                     getSources().withOffset(delta),
                                     initValues, arrayType);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withNewRegisters(RegisterSpec result,
            RegisterSpecList sources) {

        return new FillArrayDataInsn(getOpcode(), getPosition(),
                                     sources, initValues, arrayType);
    }
}
