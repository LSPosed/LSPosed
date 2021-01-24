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

import external.com.android.dx.util.Bits;
import external.com.android.dx.util.IntList;

/**
 * Code to figure out which local variables are active at which points in
 * a method.
 */
public final class LocalVariableExtractor {
    /** {@code non-null;} method being extracted from */
    private final RopMethod method;

    /** {@code non-null;} block list for the method */
    private final BasicBlockList blocks;

    /** {@code non-null;} result in-progress */
    private final LocalVariableInfo resultInfo;

    /** {@code non-null;} work set indicating blocks needing to be processed */
    private final int[] workSet;

    /**
     * Extracts out all the local variable information from the given method.
     *
     * @param method {@code non-null;} the method to extract from
     * @return {@code non-null;} the extracted information
     */
    public static LocalVariableInfo extract(RopMethod method) {
        LocalVariableExtractor lve = new LocalVariableExtractor(method);
        return lve.doit();
    }

    /**
     * Constructs an instance. This method is private. Use {@link #extract}.
     *
     * @param method {@code non-null;} the method to extract from
     */
    private LocalVariableExtractor(RopMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        BasicBlockList blocks = method.getBlocks();
        int maxLabel = blocks.getMaxLabel();

        this.method = method;
        this.blocks = blocks;
        this.resultInfo = new LocalVariableInfo(method);
        this.workSet = Bits.makeBitSet(maxLabel);
    }

    /**
     * Does the extraction.
     *
     * @return {@code non-null;} the extracted information
     */
    private LocalVariableInfo doit() {
        for (int label = method.getFirstLabel();
             label >= 0;
             label = Bits.findFirst(workSet, 0)) {
            Bits.clear(workSet, label);
            processBlock(label);
        }

        resultInfo.setImmutable();
        return resultInfo;
    }

    /**
     * Processes a single block.
     *
     * @param label {@code >= 0;} label of the block to process
     */
    private void processBlock(int label) {
        RegisterSpecSet primaryState = resultInfo.mutableCopyOfStarts(label);
        BasicBlock block = blocks.labelToBlock(label);
        InsnList insns = block.getInsns();
        int insnSz = insns.size();

        /*
         * We may have to treat the last instruction specially: If it
         * can (but doesn't always) throw, and the exception can be
         * caught within the same method, then we need to use the
         * state *before* executing it to be what is merged into
         * exception targets.
         */
        boolean canThrowDuringLastInsn = block.hasExceptionHandlers() &&
            (insns.getLast().getResult() != null);
        int freezeSecondaryStateAt = insnSz - 1;
        RegisterSpecSet secondaryState = primaryState;

        /*
         * Iterate over the instructions, adding information for each place
         * that the active variable set changes.
         */

        for (int i = 0; i < insnSz; i++) {
            if (canThrowDuringLastInsn && (i == freezeSecondaryStateAt)) {
                // Until this point, primaryState == secondaryState.
                primaryState.setImmutable();
                primaryState = primaryState.mutableCopy();
            }

            Insn insn = insns.get(i);
            RegisterSpec result;

            result = insn.getLocalAssignment();

            if (result == null) {
                /*
                 * If an assignment assigns over an existing local, make
                 * sure to mark the local as going out of scope.
                 */

                result = insn.getResult();

                if (result != null
                        && primaryState.get(result.getReg()) != null) {
                    primaryState.remove(primaryState.get(result.getReg()));
                }
                continue;
            }

            result = result.withSimpleType();

            RegisterSpec already = primaryState.get(result);
            /*
             * The equals() check ensures we only add new info if
             * the instruction causes a change to the set of
             * active variables.
             */
            if (!result.equals(already)) {
                /*
                 * If this insn represents a local moving from one register
                 * to another, remove the association between the old register
                 * and the local.
                 */
                RegisterSpec previous
                        = primaryState.localItemToSpec(result.getLocalItem());

                if (previous != null
                        && (previous.getReg() != result.getReg())) {

                    primaryState.remove(previous);
                }

                resultInfo.addAssignment(insn, result);
                primaryState.put(result);
            }
        }

        primaryState.setImmutable();

        /*
         * Merge this state into the start state for each successor,
         * and update the work set where required (that is, in cases
         * where the start state for a block changes).
         */

        IntList successors = block.getSuccessors();
        int succSz = successors.size();
        int primarySuccessor = block.getPrimarySuccessor();

        for (int i = 0; i < succSz; i++) {
            int succ = successors.get(i);
            RegisterSpecSet state = (succ == primarySuccessor) ?
                primaryState : secondaryState;

            if (resultInfo.mergeStarts(succ, state)) {
                Bits.set(workSet, succ);
            }
        }
    }
}
