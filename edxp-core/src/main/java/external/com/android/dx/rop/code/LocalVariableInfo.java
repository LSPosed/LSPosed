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

import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.MutabilityControl;
import java.util.HashMap;

/**
 * Container for local variable information for a particular {@link
 * RopMethod}.
 */
public final class LocalVariableInfo
        extends MutabilityControl {
    /** {@code >= 0;} the register count for the method */
    private final int regCount;

    /**
     * {@code non-null;} {@link RegisterSpecSet} to use when indicating a block
     * that has no locals; it is empty and immutable but has an appropriate
     * max size for the method
     */
    private final RegisterSpecSet emptySet;

    /**
     * {@code non-null;} array consisting of register sets representing the
     * sets of variables already assigned upon entry to each block,
     * where array indices correspond to block labels
     */
    private final RegisterSpecSet[] blockStarts;

    /** {@code non-null;} map from instructions to the variable each assigns */
    private final HashMap<Insn, RegisterSpec> insnAssignments;

    /**
     * Constructs an instance.
     *
     * @param method {@code non-null;} the method being represented by this instance
     */
    public LocalVariableInfo(RopMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        BasicBlockList blocks = method.getBlocks();
        int maxLabel = blocks.getMaxLabel();

        this.regCount = blocks.getRegCount();
        this.emptySet = new RegisterSpecSet(regCount);
        this.blockStarts = new RegisterSpecSet[maxLabel];
        this.insnAssignments =
            new HashMap<Insn, RegisterSpec>(blocks.getInstructionCount());

        emptySet.setImmutable();
    }

    /**
     * Sets the register set associated with the start of the block with
     * the given label.
     *
     * @param label {@code >= 0;} the block label
     * @param specs {@code non-null;} the register set to associate with the block
     */
    public void setStarts(int label, RegisterSpecSet specs) {
        throwIfImmutable();

        if (specs == null) {
            throw new NullPointerException("specs == null");
        }

        try {
            blockStarts[label] = specs;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("bogus label");
        }
    }

    /**
     * Merges the given register set into the set for the block with the
     * given label. If there was not already an associated set, then this
     * is the same as calling {@link #setStarts}. Otherwise, this will
     * merge the two sets and call {@link #setStarts} on the result of the
     * merge.
     *
     * @param label {@code >= 0;} the block label
     * @param specs {@code non-null;} the register set to merge into the start set
     * for the block
     * @return {@code true} if the merge resulted in an actual change
     * to the associated set (including storing one for the first time) or
     * {@code false} if there was no change
     */
    public boolean mergeStarts(int label, RegisterSpecSet specs) {
        RegisterSpecSet start = getStarts0(label);
        boolean changed = false;

        if (start == null) {
            setStarts(label, specs);
            return true;
        }

        RegisterSpecSet newStart = start.mutableCopy();
        if (start.size() != 0) {
            newStart.intersect(specs, true);
        } else {
            newStart = specs.mutableCopy();
        }

        if (start.equals(newStart)) {
            return false;
        }

        newStart.setImmutable();
        setStarts(label, newStart);

        return true;
    }

    /**
     * Gets the register set associated with the start of the block
     * with the given label. This returns an empty set with the appropriate
     * max size if no set was associated with the block in question.
     *
     * @param label {@code >= 0;} the block label
     * @return {@code non-null;} the associated register set
     */
    public RegisterSpecSet getStarts(int label) {
        RegisterSpecSet result = getStarts0(label);

        return (result != null) ? result : emptySet;
    }

    /**
     * Gets the register set associated with the start of the given
     * block. This is just convenient shorthand for
     * {@code getStarts(block.getLabel())}.
     *
     * @param block {@code non-null;} the block in question
     * @return {@code non-null;} the associated register set
     */
    public RegisterSpecSet getStarts(BasicBlock block) {
        return getStarts(block.getLabel());
    }

    /**
     * Gets a mutable copy of the register set associated with the
     * start of the block with the given label. This returns a
     * newly-allocated empty {@link RegisterSpecSet} of appropriate
     * max size if there is not yet any set associated with the block.
     *
     * @param label {@code >= 0;} the block label
     * @return {@code non-null;} the associated register set
     */
    public RegisterSpecSet mutableCopyOfStarts(int label) {
        RegisterSpecSet result = getStarts0(label);

        return (result != null) ?
            result.mutableCopy() : new RegisterSpecSet(regCount);
    }

    /**
     * Adds an assignment association for the given instruction and
     * register spec. This throws an exception if the instruction
     * doesn't actually perform a named variable assignment.
     *
     * <b>Note:</b> Although the instruction contains its own spec for
     * the result, it still needs to be passed in explicitly to this
     * method, since the spec that is stored here should always have a
     * simple type and the one in the instruction can be an arbitrary
     * {@link TypeBearer} (such as a constant value).
     *
     * @param insn {@code non-null;} the instruction in question
     * @param spec {@code non-null;} the associated register spec
     */
    public void addAssignment(Insn insn, RegisterSpec spec) {
        throwIfImmutable();

        if (insn == null) {
            throw new NullPointerException("insn == null");
        }

        if (spec == null) {
            throw new NullPointerException("spec == null");
        }

        insnAssignments.put(insn, spec);
    }

    /**
     * Gets the named register being assigned by the given instruction, if
     * previously stored in this instance.
     *
     * @param insn {@code non-null;} instruction in question
     * @return {@code null-ok;} the named register being assigned, if any
     */
    public RegisterSpec getAssignment(Insn insn) {
        return insnAssignments.get(insn);
    }

    /**
     * Gets the number of assignments recorded by this instance.
     *
     * @return {@code >= 0;} the number of assignments
     */
    public int getAssignmentCount() {
        return insnAssignments.size();
    }

    public void debugDump() {
        for (int label = 0 ; label < blockStarts.length; label++) {
            if (blockStarts[label] == null) {
                continue;
            }

            if (blockStarts[label] == emptySet) {
                System.out.printf("%04x: empty set\n", label);
            } else {
                System.out.printf("%04x: %s\n", label, blockStarts[label]);
            }
        }
    }

    /**
     * Helper method, to get the starts for a label, throwing the
     * right exception for range problems.
     *
     * @param label {@code >= 0;} the block label
     * @return {@code null-ok;} associated register set or {@code null} if there
     * is none
     */
    private RegisterSpecSet getStarts0(int label) {
        try {
            return blockStarts[label];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("bogus label");
        }
    }
}
