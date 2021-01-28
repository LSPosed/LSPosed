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

package external.com.android.dx.dex.code;

import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;

/**
 * Instruction which has a single branch target.
 */
public final class TargetInsn extends FixedSizeInsn {
    /** {@code non-null;} the branch target */
    private CodeAddress target;

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}), and the target is initially
     * {@code null}.
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins or outs)
     * @param target {@code non-null;} the branch target
     */
    public TargetInsn(Dop opcode, SourcePosition position,
                      RegisterSpecList registers, CodeAddress target) {
        super(opcode, position, registers);

        if (target == null) {
            throw new NullPointerException("target == null");
        }

        this.target = target;
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withOpcode(Dop opcode) {
        return new TargetInsn(opcode, getPosition(), getRegisters(), target);
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new TargetInsn(getOpcode(), getPosition(), registers, target);
    }

    /**
     * Returns an instance that is just like this one, except that its
     * opcode has the opposite sense (as a test; e.g. a
     * {@code lt} test becomes a {@code ge}), and its branch
     * target is replaced by the one given, and all set-once values
     * associated with the class (such as its address) are reset.
     *
     * @param target {@code non-null;} the new branch target
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public TargetInsn withNewTargetAndReversed(CodeAddress target) {
        Dop opcode = getOpcode().getOppositeTest();

        return new TargetInsn(opcode, getPosition(), getRegisters(), target);
    }

    /**
     * Gets the unique branch target of this instruction.
     *
     * @return {@code non-null;} the branch target
     */
    public CodeAddress getTarget() {
        return target;
    }

    /**
     * Gets the target address of this instruction. This is only valid
     * to call if the target instruction has been assigned an address,
     * and it is merely a convenient shorthand for
     * {@code getTarget().getAddress()}.
     *
     * @return {@code >= 0;} the target address
     */
    public int getTargetAddress() {
        return target.getAddress();
    }

    /**
     * Gets the branch offset of this instruction. This is only valid to
     * call if both this and the target instruction each has been assigned
     * an address, and it is merely a convenient shorthand for
     * {@code getTargetAddress() - getAddress()}.
     *
     * @return the branch offset
     */
    public int getTargetOffset() {
        return target.getAddress() - getAddress();
    }

    /**
     * Returns whether the target offset is known.
     *
     * @return {@code true} if the target offset is known or
     * {@code false} if not
     */
    public boolean hasTargetOffset() {
        return hasAddress() && target.hasAddress();
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        if (target == null) {
            return "????";
        }

        return target.identifierString();
    }
}
