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

import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import external.com.android.dx.util.LabeledItem;

/**
 * Basic block of register-based instructions.
 */
public final class BasicBlock implements LabeledItem {
    /** {@code >= 0;} target label for this block */
    private final int label;

    /** {@code non-null;} list of instructions in this block */
    private final InsnList insns;

    /**
     * {@code non-null;} full list of successors that this block may
     * branch to
     */
    private final IntList successors;

    /**
     * {@code >= -1;} the primary / standard-flow / "default" successor, or
     * {@code -1} if this block has no successors (that is, it
     * exits the function/method)
     */
    private final int primarySuccessor;

    /**
     * Constructs an instance. The predecessor set is set to {@code null}.
     *
     * @param label {@code >= 0;} target label for this block
     * @param insns {@code non-null;} list of instructions in this block
     * @param successors {@code non-null;} full list of successors that this
     * block may branch to
     * @param primarySuccessor {@code >= -1;} the primary / standard-flow /
     * "default" successor, or {@code -1} if this block has no
     * successors (that is, it exits the function/method or is an
     * unconditional throw)
     */
    public BasicBlock(int label, InsnList insns, IntList successors,
                      int primarySuccessor) {
        if (label < 0) {
            throw new IllegalArgumentException("label < 0");
        }

        try {
            insns.throwIfMutable();
        } catch (NullPointerException ex) {
            // Elucidate exception.
            throw new NullPointerException("insns == null");
        }

        int sz = insns.size();

        if (sz == 0) {
            throw new IllegalArgumentException("insns.size() == 0");
        }

        for (int i = sz - 2; i >= 0; i--) {
            Rop one = insns.get(i).getOpcode();
            if (one.getBranchingness() != Rop.BRANCH_NONE) {
                throw new IllegalArgumentException("insns[" + i + "] is a " +
                                                   "branch or can throw");
            }
        }

        Insn lastInsn = insns.get(sz - 1);
        if (lastInsn.getOpcode().getBranchingness() == Rop.BRANCH_NONE) {
            throw new IllegalArgumentException("insns does not end with " +
                                               "a branch or throwing " +
                                               "instruction");
        }

        try {
            successors.throwIfMutable();
        } catch (NullPointerException ex) {
            // Elucidate exception.
            throw new NullPointerException("successors == null");
        }

        if (primarySuccessor < -1) {
            throw new IllegalArgumentException("primarySuccessor < -1");
        }

        if (primarySuccessor >= 0 && !successors.contains(primarySuccessor)) {
            throw new IllegalArgumentException(
                    "primarySuccessor " + primarySuccessor + " not in successors " + successors);
        }

        this.label = label;
        this.insns = insns;
        this.successors = successors;
        this.primarySuccessor = primarySuccessor;
    }

    /**
     * {@inheritDoc}
     *
     * Instances of this class compare by identity. That is,
     * {@code x.equals(y)} is only true if {@code x == y}.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other);
    }

    /**
     * {@inheritDoc}
     *
     * Return the identity hashcode of this instance. This is proper,
     * since instances of this class compare by identity (see {@link #equals}).
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Gets the target label of this block.
     *
     * @return {@code >= 0;} the label
     */
    @Override
    public int getLabel() {
        return label;
    }

    /**
     * Gets the list of instructions inside this block.
     *
     * @return {@code non-null;} the instruction list
     */
    public InsnList getInsns() {
        return insns;
    }

    /**
     * Gets the list of successors that this block may branch to.
     *
     * @return {@code non-null;} the successors list
     */
    public IntList getSuccessors() {
        return successors;
    }

    /**
     * Gets the primary successor of this block.
     *
     * @return {@code >= -1;} the primary successor, or {@code -1} if this
     * block has no successors at all
     */
    public int getPrimarySuccessor() {
        return primarySuccessor;
    }

    /**
     * Gets the secondary successor of this block. It is only valid to call
     * this method on blocks that have exactly two successors.
     *
     * @return {@code >= 0;} the secondary successor
     */
    public int getSecondarySuccessor() {
        if (successors.size() != 2) {
            throw new UnsupportedOperationException(
                    "block doesn't have exactly two successors");
        }

        int succ = successors.get(0);
        if (succ == primarySuccessor) {
            succ = successors.get(1);
        }

        return succ;
    }

    /**
     * Gets the first instruction of this block. This is just a
     * convenient shorthand for {@code getInsns().get(0)}.
     *
     * @return {@code non-null;} the first instruction
     */
    public Insn getFirstInsn() {
        return insns.get(0);
    }

    /**
     * Gets the last instruction of this block. This is just a
     * convenient shorthand for {@code getInsns().getLast()}.
     *
     * @return {@code non-null;} the last instruction
     */
    public Insn getLastInsn() {
        return insns.getLast();
    }

    /**
     * Returns whether this block might throw an exception. This is
     * just a convenient shorthand for {@code getLastInsn().canThrow()}.
     *
     * @return {@code true} iff this block might throw an
     * exception
     */
    public boolean canThrow() {
        return insns.getLast().canThrow();
    }

    /**
     * Returns whether this block has any associated exception handlers.
     * This is just a shorthand for inspecting the last instruction in
     * the block to see if it could throw, and if so, whether it in fact
     * has any associated handlers.
     *
     * @return {@code true} iff this block has any associated
     * exception handlers
     */
    public boolean hasExceptionHandlers() {
        Insn lastInsn = insns.getLast();
        return lastInsn.getCatches().size() != 0;
    }

    /**
     * Returns the exception handler types associated with this block,
     * if any. This is just a shorthand for inspecting the last
     * instruction in the block to see if it could throw, and if so,
     * grabbing the catch list out of it. If not, this returns an
     * empty list (not {@code null}).
     *
     * @return {@code non-null;} the exception handler types associated with
     * this block
     */
    public TypeList getExceptionHandlerTypes() {
        Insn lastInsn = insns.getLast();
        return lastInsn.getCatches();
    }

    /**
     * Returns an instance that is identical to this one, except that
     * the registers in each instruction are offset by the given
     * amount.
     *
     * @param delta the amount to offset register numbers by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public BasicBlock withRegisterOffset(int delta) {
        return new BasicBlock(label, insns.withRegisterOffset(delta),
                              successors, primarySuccessor);
    }

    @Override
    public String toString() {
        return '{' + Hex.u2(label) + '}';
    }

    /**
     * BasicBlock visitor interface
     */
    public interface Visitor {
        /**
         * Visits a basic block
         * @param b block visited
         */
        public void visitBlock (BasicBlock b);
    }
}
