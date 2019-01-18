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

import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;

/**
 * Instruction which possibly throws. The {@code successors} list in the
 * basic block an instance of this class is inside corresponds in-order to
 * the list of exceptions handled by this instruction, with the
 * no-exception case appended as the final target.
 */
public final class ThrowingInsn
        extends Insn {
    /** {@code non-null;} list of exceptions caught */
    private final TypeList catches;

    /**
     * Gets the string form of a register spec list to be used as a catches
     * list.
     *
     * @param catches {@code non-null;} the catches list
     * @return {@code non-null;} the string form
     */
    public static String toCatchString(TypeList catches) {
        StringBuilder sb = new StringBuilder(100);

        sb.append("catch");

        int sz = catches.size();
        for (int i = 0; i < sz; i++) {
            sb.append(" ");
            sb.append(catches.getType(i).toHuman());
        }

        return sb.toString();
    }

    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param sources {@code non-null;} specs for all the sources
     * @param catches {@code non-null;} list of exceptions caught
     */
    public ThrowingInsn(Rop opcode, SourcePosition position,
                        RegisterSpecList sources,
                        TypeList catches) {
        super(opcode, position, null, sources);

        if (opcode.getBranchingness() != Rop.BRANCH_THROW) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }

        if (catches == null) {
            throw new NullPointerException("catches == null");
        }

        this.catches = catches;
    }

    /** {@inheritDoc} */
    @Override
    public String getInlineString() {
        return toCatchString(catches);
    }

    /** {@inheritDoc} */
    @Override
    public TypeList getCatches() {
        return catches;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Visitor visitor) {
        visitor.visitThrowingInsn(this);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withAddedCatch(Type type) {
        return new ThrowingInsn(getOpcode(), getPosition(),
                                getSources(), catches.withAddedType(type));
    }

    /** {@inheritDoc} */
    @Override
    public Insn withRegisterOffset(int delta) {
        return new ThrowingInsn(getOpcode(), getPosition(),
                                getSources().withOffset(delta),
                                catches);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withNewRegisters(RegisterSpec result,
            RegisterSpecList sources) {

        return new ThrowingInsn(getOpcode(), getPosition(),
                                sources,
                                catches);
    }
}
