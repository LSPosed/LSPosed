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

import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;

/**
 * All of the parts that make up a method at the rop layer.
 */
public final class RopMethod {
    /** {@code non-null;} basic block list of the method */
    private final BasicBlockList blocks;

    /** {@code >= 0;} label for the block which starts the method */
    private final int firstLabel;

    /**
     * {@code null-ok;} array of predecessors for each block, indexed by block
     * label
     */
    private IntList[] predecessors;

    /**
     * {@code null-ok;} the predecessors for the implicit "exit" block, that is
     * the labels for the blocks that return, if calculated
     */
    private IntList exitPredecessors;

    /**
     * Constructs an instance.
     *
     * @param blocks {@code non-null;} basic block list of the method
     * @param firstLabel {@code >= 0;} the label of the first block to execute
     */
    public RopMethod(BasicBlockList blocks, int firstLabel) {
        if (blocks == null) {
            throw new NullPointerException("blocks == null");
        }

        if (firstLabel < 0) {
            throw new IllegalArgumentException("firstLabel < 0");
        }

        this.blocks = blocks;
        this.firstLabel = firstLabel;

        this.predecessors = null;
        this.exitPredecessors = null;
    }

    /**
     * Gets the basic block list for this method.
     *
     * @return {@code non-null;} the list
     */
    public BasicBlockList getBlocks() {
        return blocks;
    }

    /**
     * Gets the label for the first block in the method that this list
     * represents.
     *
     * @return {@code >= 0;} the first-block label
     */
    public int getFirstLabel() {
        return firstLabel;
    }

    /**
     * Gets the predecessors associated with the given block. This throws
     * an exception if there is no block with the given label.
     *
     * @param label {@code >= 0;} the label of the block in question
     * @return {@code non-null;} the predecessors of that block
     */
    public IntList labelToPredecessors(int label) {
        if (exitPredecessors == null) {
            calcPredecessors();
        }

        IntList result = predecessors[label];

        if (result == null) {
            throw new RuntimeException("no such block: " + Hex.u2(label));
        }

        return result;
    }

    /**
     * Gets the exit predecessors for this instance.
     *
     * @return {@code non-null;} the exit predecessors
     */
    public IntList getExitPredecessors() {
        if (exitPredecessors == null) {
            calcPredecessors();
        }

        return exitPredecessors;
    }


    /**
     * Returns an instance that is identical to this one, except that
     * the registers in each instruction are offset by the given
     * amount.
     *
     * @param delta the amount to offset register numbers by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RopMethod withRegisterOffset(int delta) {
        RopMethod result = new RopMethod(blocks.withRegisterOffset(delta),
                                         firstLabel);

        if (exitPredecessors != null) {
            /*
             * The predecessors have been calculated. It's safe to
             * inject these into the new instance, since the
             * transformation being applied doesn't affect the
             * predecessors.
             */
            result.exitPredecessors = exitPredecessors;
            result.predecessors = predecessors;
        }

        return result;
    }

    /**
     * Calculates the predecessor sets for each block as well as for the
     * exit.
     */
    private void calcPredecessors() {
        int maxLabel = blocks.getMaxLabel();
        IntList[] predecessors = new IntList[maxLabel];
        IntList exitPredecessors = new IntList(10);
        int sz = blocks.size();

        /*
         * For each block, find its successors, and add the block's label to
         * the successor's predecessors.
         */
        for (int i = 0; i < sz; i++) {
            BasicBlock one = blocks.get(i);
            int label = one.getLabel();
            IntList successors = one.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                // This block exits.
                exitPredecessors.add(label);
            } else {
                for (int j = 0; j < ssz; j++) {
                    int succLabel = successors.get(j);
                    IntList succPreds = predecessors[succLabel];
                    if (succPreds == null) {
                        succPreds = new IntList(10);
                        predecessors[succLabel] = succPreds;
                    }
                    succPreds.add(label);
                }
            }
        }

        // Sort and immutablize all the predecessor lists.
        for (int i = 0; i < maxLabel; i++) {
            IntList preds = predecessors[i];
            if (preds != null) {
                preds.sort();
                preds.setImmutable();
            }
        }

        exitPredecessors.sort();
        exitPredecessors.setImmutable();

        /*
         * The start label might not ever have had any predecessors
         * added to it (probably doesn't, because of how Java gets
         * translated into rop form). So, check for this and rectify
         * the situation if required.
         */
        if (predecessors[firstLabel] == null) {
            predecessors[firstLabel] = IntList.EMPTY;
        }

        this.predecessors = predecessors;
        this.exitPredecessors = exitPredecessors;
    }
}
