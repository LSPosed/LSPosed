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

import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import external.com.android.dx.util.LabeledList;

/**
 * List of {@link BasicBlock} instances.
 */
public final class BasicBlockList extends LabeledList {
    /**
     * {@code >= -1;} the count of registers required by this method or
     * {@code -1} if not yet calculated
     */
    private int regCount;

    /**
     * Constructs an instance. All indices initially contain {@code null},
     * and the first-block label is initially {@code -1}.
     *
     * @param size the size of the list
     */
    public BasicBlockList(int size) {
        super(size);

        regCount = -1;
    }

    /**
     * Constructs a mutable copy for {@code getMutableCopy()}.
     *
     * @param old block to copy
     */
    private BasicBlockList(BasicBlockList old) {
        super(old);
        regCount = old.regCount;
    }


    /**
     * Gets the element at the given index. It is an error to call
     * this with the index for an element which was never set; if you
     * do that, this will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which index
     * @return {@code non-null;} element at that index
     */
    public BasicBlock get(int n) {
        return (BasicBlock) get0(n);
    }

    /**
     * Sets the basic block at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param bb {@code null-ok;} the element to set at {@code n}
     */
    public void set(int n, BasicBlock bb) {
        super.set(n, bb);

        // Reset regCount, since it will need to be recalculated.
        regCount = -1;
    }

    /**
     * Returns how many registers this method requires. This is simply
     * the maximum of register-number-plus-category referred to by this
     * instance's instructions (indirectly through {@link BasicBlock}
     * instances).
     *
     * @return {@code >= 0;} the register count
     */
    public int getRegCount() {
        if (regCount == -1) {
            RegCountVisitor visitor = new RegCountVisitor();
            forEachInsn(visitor);
            regCount = visitor.getRegCount();
        }

        return regCount;
    }

    /**
     * Gets the total instruction count for this instance. This is the
     * sum of the instruction counts of each block.
     *
     * @return {@code >= 0;} the total instruction count
     */
    public int getInstructionCount() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) getOrNull0(i);
            if (one != null) {
                result += one.getInsns().size();
            }
        }

        return result;
    }

    /**
     * Gets the total instruction count for this instance, ignoring
     * mark-local instructions which are not actually emitted.
     *
     * @return {@code >= 0;} the total instruction count
     */
    public int getEffectiveInstructionCount() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) getOrNull0(i);
            if (one != null) {
                InsnList insns = one.getInsns();
                int insnsSz = insns.size();

                for (int j = 0; j < insnsSz; j++) {
                    Insn insn = insns.get(j);

                    if (insn.getOpcode().getOpcode() != RegOps.MARK_LOCAL) {
                        result++;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets the first block in the list with the given label, if any.
     *
     * @param label {@code label >= 0;} the label to look for
     * @return {@code non-null;} the so-labelled block
     * @throws IllegalArgumentException thrown if the label isn't found
     */
    public BasicBlock labelToBlock(int label) {
        int idx = indexOfLabel(label);

        if (idx < 0) {
            throw new IllegalArgumentException("no such label: "
                    + Hex.u2(label));
        }

        return get(idx);
    }

    /**
     * Visits each instruction of each block in the list, in order.
     *
     * @param visitor {@code non-null;} visitor to use
     */
    public void forEachInsn(Insn.Visitor visitor) {
        int sz = size();

        for (int i = 0; i < sz; i++) {
            BasicBlock one = get(i);
            InsnList insns = one.getInsns();
            insns.forEach(visitor);
        }
    }

    /**
     * Returns an instance that is identical to this one, except that
     * the registers in each instruction are offset by the given
     * amount. Mutability of the result is inherited from the
     * original.
     *
     * @param delta the amount to offset register numbers by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public BasicBlockList withRegisterOffset(int delta) {
        int sz = size();
        BasicBlockList result = new BasicBlockList(sz);

        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) get0(i);
            if (one != null) {
                result.set(i, one.withRegisterOffset(delta));
            }
        }

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns a mutable copy of this list.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public BasicBlockList getMutableCopy() {
        return new BasicBlockList(this);
    }

    /**
     * Gets the preferred successor for the given block. If the block
     * only has one successor, then that is the preferred successor.
     * Otherwise, if the block has a primay successor, then that is
     * the preferred successor. If the block has no successors, then
     * this returns {@code null}.
     *
     * @param block {@code non-null;} the block in question
     * @return {@code null-ok;} the preferred successor, if any
     */
    public BasicBlock preferredSuccessorOf(BasicBlock block) {
        int primarySuccessor = block.getPrimarySuccessor();
        IntList successors = block.getSuccessors();
        int succSize = successors.size();

        switch (succSize) {
            case 0: {
                return null;
            }
            case 1: {
                return labelToBlock(successors.get(0));
            }
        }

        if (primarySuccessor != -1) {
            return labelToBlock(primarySuccessor);
        } else {
            return labelToBlock(successors.get(0));
        }
    }

    /**
     * Compares the catches of two blocks for equality. This includes
     * both the catch types and target labels.
     *
     * @param block1 {@code non-null;} one block to compare
     * @param block2 {@code non-null;} the other block to compare
     * @return {@code true} if the two blocks' non-primary successors
     * are identical
     */
    public boolean catchesEqual(BasicBlock block1, BasicBlock block2) {
        TypeList catches1 = block1.getExceptionHandlerTypes();
        TypeList catches2 = block2.getExceptionHandlerTypes();

        if (!StdTypeList.equalContents(catches1, catches2)) {
            return false;
        }

        IntList succ1 = block1.getSuccessors();
        IntList succ2 = block2.getSuccessors();
        int size = succ1.size(); // Both are guaranteed to be the same size.

        int primary1 = block1.getPrimarySuccessor();
        int primary2 = block2.getPrimarySuccessor();

        if (((primary1 == -1) || (primary2 == -1))
                && (primary1 != primary2)) {
            /*
             * For the current purpose, both blocks in question must
             * either both have a primary or both not have a primary to
             * be considered equal, and it turns out here that that's not
             * the case.
             */
            return false;
        }

        for (int i = 0; i < size; i++) {
            int label1 = succ1.get(i);
            int label2 = succ2.get(i);

            if (label1 == primary1) {
                /*
                 * It should be the case that block2's primary is at the
                 * same index. If not, we consider the blocks unequal for
                 * the current purpose.
                 */
                if (label2 != primary2) {
                    return false;
                }
                continue;
            }

            if (label1 != label2) {
                return false;
            }
        }

        return true;
    }

    /**
     * Instruction visitor class for counting registers used.
     */
    private static class RegCountVisitor
            implements Insn.Visitor {
        /** {@code >= 0;} register count in-progress */
        private int regCount;

        /**
         * Constructs an instance.
         */
        public RegCountVisitor() {
            regCount = 0;
        }

        /**
         * Gets the register count.
         *
         * @return {@code >= 0;} the count
         */
        public int getRegCount() {
            return regCount;
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainInsn(PlainInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainCstInsn(PlainCstInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitSwitchInsn(SwitchInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingInsn(ThrowingInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
            visit(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
            visit(insn);
        }

        /**
         * Helper for all the {@code visit*} methods.
         *
         * @param insn {@code non-null;} instruction being visited
         */
        private void visit(Insn insn) {
            RegisterSpec result = insn.getResult();

            if (result != null) {
                processReg(result);
            }

            RegisterSpecList sources = insn.getSources();
            int sz = sources.size();

            for (int i = 0; i < sz; i++) {
                processReg(sources.get(i));
            }
        }

        /**
         * Processes the given register spec.
         *
         * @param spec {@code non-null;} the register spec
         */
        private void processReg(RegisterSpec spec) {
            int reg = spec.getNextReg();

            if (reg > regCount) {
                regCount = reg;
            }
        }
    }
}
