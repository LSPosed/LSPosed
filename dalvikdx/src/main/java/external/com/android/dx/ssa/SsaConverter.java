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

package external.com.android.dx.ssa;

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.util.IntIterator;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Converts ROP methods to SSA Methods
 */
public class SsaConverter {
    public static final boolean DEBUG = false;

    /**
     * Returns an SSA representation, edge-split and with phi
     * functions placed.
     *
     * @param rmeth input
     * @param paramWidth the total width, in register-units, of the method's
     * parameters
     * @param isStatic {@code true} if this method has no {@code this}
     * pointer argument
     * @return output in SSA form
     */
    public static SsaMethod convertToSsaMethod(RopMethod rmeth,
            int paramWidth, boolean isStatic) {
        SsaMethod result
            = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);

        edgeSplit(result);

        LocalVariableInfo localInfo = LocalVariableExtractor.extract(result);

        placePhiFunctions(result, localInfo, 0);
        new SsaRenamer(result).run();

        /*
         * The exit block, added here, is not considered for edge splitting
         * or phi placement since no actual control flows to it.
         */
        result.makeExitBlock();

        return result;
    }

    /**
     * Updates an SSA representation, placing phi functions and renaming all
     * registers above a certain threshold number.
     *
     * @param ssaMeth input
     * @param threshold registers below this number are unchanged
     */
    public static void updateSsaMethod(SsaMethod ssaMeth, int threshold) {
        LocalVariableInfo localInfo = LocalVariableExtractor.extract(ssaMeth);
        placePhiFunctions(ssaMeth, localInfo, threshold);
        new SsaRenamer(ssaMeth, threshold).run();
    }

    /**
     * Returns an SSA represention with only the edge-splitter run.
     *
     * @param rmeth method to process
     * @param paramWidth width of all arguments in the method
     * @param isStatic {@code true} if this method has no {@code this}
     * pointer argument
     * @return an SSA represention with only the edge-splitter run
     */
    public static SsaMethod testEdgeSplit (RopMethod rmeth, int paramWidth,
            boolean isStatic) {
        SsaMethod result;

        result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);

        edgeSplit(result);
        return result;
    }

    /**
     * Returns an SSA represention with only the steps through the
     * phi placement run.
     *
     * @param rmeth method to process
     * @param paramWidth width of all arguments in the method
     * @param isStatic {@code true} if this method has no {@code this}
     * pointer argument
     * @return an SSA represention with only the edge-splitter run
     */
    public static SsaMethod testPhiPlacement (RopMethod rmeth, int paramWidth,
            boolean isStatic) {
        SsaMethod result;

        result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);

        edgeSplit(result);

        LocalVariableInfo localInfo = LocalVariableExtractor.extract(result);

        placePhiFunctions(result, localInfo, 0);
        return result;
    }

    /**
     * See Appel section 19.1:
     *
     * Converts CFG into "edge-split" form, such that each node either a
     * unique successor or unique predecessor.<p>
     *
     * In addition, the SSA form we use enforces a further constraint,
     * requiring each block with a final instruction that returns a
     * value to have a primary successor that has no other
     * predecessor. This ensures move statements can always be
     * inserted correctly when phi statements are removed.
     *
     * @param result method to process
     */
    private static void edgeSplit(SsaMethod result) {
        edgeSplitPredecessors(result);
        edgeSplitMoveExceptionsAndResults(result);
        edgeSplitSuccessors(result);
    }

    /**
     * Inserts Z nodes as new predecessors for every node that has multiple
     * successors and multiple predecessors.
     *
     * @param result {@code non-null;} method to process
     */
    private static void edgeSplitPredecessors(SsaMethod result) {
        ArrayList<SsaBasicBlock> blocks = result.getBlocks();

        /*
         * New blocks are added to the end of the block list during
         * this iteration.
         */
        for (int i = blocks.size() - 1; i >= 0; i-- ) {
            SsaBasicBlock block = blocks.get(i);
            if (nodeNeedsUniquePredecessor(block)) {
                block.insertNewPredecessor();
            }
        }
    }

    /**
     * @param block {@code non-null;} block in question
     * @return {@code true} if this node needs to have a unique
     * predecessor created for it
     */
    private static boolean nodeNeedsUniquePredecessor(SsaBasicBlock block) {
        /*
         * Any block with that has both multiple successors and multiple
         * predecessors needs a new predecessor node.
         */

        int countPredecessors = block.getPredecessors().cardinality();
        int countSuccessors = block.getSuccessors().cardinality();

        return  (countPredecessors > 1 && countSuccessors > 1);
    }

    /**
     * In ROP form, move-exception must occur as the first insn in a block
     * immediately succeeding the insn that could thrown an exception.
     * We may need room to insert move insns later, so make sure to split
     * any block that starts with a move-exception such that there is a
     * unique move-exception block for each predecessor.
     *
     * @param ssaMeth method to process
     */
    private static void edgeSplitMoveExceptionsAndResults(SsaMethod ssaMeth) {
        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();

        /*
         * New blocks are added to the end of the block list during
         * this iteration.
         */
        for (int i = blocks.size() - 1; i >= 0; i-- ) {
            SsaBasicBlock block = blocks.get(i);

            /*
             * Any block that starts with a move-exception and has more than
             * one predecessor...
             */
            if (!block.isExitBlock()
                    && block.getPredecessors().cardinality() > 1
                    && block.getInsns().get(0).isMoveException()) {

                // block.getPredecessors() is changed in the loop below.
                BitSet preds = (BitSet)block.getPredecessors().clone();
                for (int j = preds.nextSetBit(0); j >= 0;
                     j = preds.nextSetBit(j + 1)) {
                    SsaBasicBlock predecessor = blocks.get(j);
                    SsaBasicBlock zNode
                        = predecessor.insertNewSuccessor(block);

                    /*
                     * Make sure to place the move-exception as the
                     * first insn.
                     */
                    zNode.getInsns().add(0, block.getInsns().get(0).clone());
                }

                // Remove the move-exception from the original block.
                block.getInsns().remove(0);
            }
        }
    }

    /**
     * Inserts Z nodes for every node that needs a new
     * successor.
     *
     * @param result {@code non-null;} method to process
     */
    private static void edgeSplitSuccessors(SsaMethod result) {
        ArrayList<SsaBasicBlock> blocks = result.getBlocks();

        /*
         * New blocks are added to the end of the block list during
         * this iteration.
         */
        for (int i = blocks.size() - 1; i >= 0; i-- ) {
            SsaBasicBlock block = blocks.get(i);

            // Successors list is modified in loop below.
            BitSet successors = (BitSet)block.getSuccessors().clone();
            for (int j = successors.nextSetBit(0);
                 j >= 0; j = successors.nextSetBit(j+1)) {

                SsaBasicBlock succ = blocks.get(j);

                if (needsNewSuccessor(block, succ)) {
                    block.insertNewSuccessor(succ);
                }
            }
        }
    }

    /**
     * Returns {@code true} if block and successor need a Z-node
     * between them. Presently, this is {@code true} if either:
     * <p><ul>
     * <li> there is a critical edge between block and successor.
     * <li> the final instruction has any sources or results and the current
     * successor block has more than one predecessor.
     * </ul></p>
     *
     * @param block predecessor node
     * @param succ successor node
     * @return {@code true} if a Z node is needed
     */
    private static boolean needsNewSuccessor(SsaBasicBlock block,
            SsaBasicBlock succ) {
        ArrayList<SsaInsn> insns = block.getInsns();
        SsaInsn lastInsn = insns.get(insns.size() - 1);

        // This is to address b/69128828 where the moves associated
        // with a phi were being propagated along a critical
        // edge. Consequently, the move instruction inserted was
        // positioned before the first instruction in the predecessor
        // block. The generated bytecode was rejected by the ART
        // verifier.
        if (block.getSuccessors().cardinality() > 1 && succ.getPredecessors().cardinality() > 1) {
            return true;
        }

        return ((lastInsn.getResult() != null)
                    || (lastInsn.getSources().size() > 0))
                && succ.getPredecessors().cardinality() > 1;
    }

    /**
     * See Appel algorithm 19.6:
     *
     * Place Phi functions in appropriate locations.
     *
     * @param ssaMeth {@code non-null;} method to process.
     * Modifications are made in-place.
     * @param localInfo {@code non-null;} local variable info, used
     * when placing phis
     * @param threshold registers below this number are ignored
     */
    private static void placePhiFunctions (SsaMethod ssaMeth,
            LocalVariableInfo localInfo, int threshold) {
        ArrayList<SsaBasicBlock> ssaBlocks;
        int regCount;
        int blockCount;

        ssaBlocks = ssaMeth.getBlocks();
        blockCount = ssaBlocks.size();
        regCount = ssaMeth.getRegCount() - threshold;

        DomFront df = new DomFront(ssaMeth);
        DomFront.DomInfo[] domInfos = df.run();

        // Bit set of registers vs block index "definition sites"
        BitSet[] defsites = new BitSet[regCount];

        // Bit set of registers vs block index "phi placement sites"
        BitSet[] phisites = new BitSet[regCount];

        for (int i = 0; i < regCount; i++) {
            defsites[i] = new BitSet(blockCount);
            phisites[i] = new BitSet(blockCount);
        }

        /*
         * For each register, build a set of all basic blocks where
         * containing an assignment to that register.
         */
        for (int bi = 0, s = ssaBlocks.size(); bi < s; bi++) {
            SsaBasicBlock b = ssaBlocks.get(bi);

            for (SsaInsn insn : b.getInsns()) {
                RegisterSpec rs = insn.getResult();

                if (rs != null && rs.getReg() - threshold >= 0) {
                    defsites[rs.getReg() - threshold].set(bi);
                }
            }
        }

        if (DEBUG) {
            System.out.println("defsites");

            for (int i = 0; i < regCount; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append('v').append(i).append(": ");
                sb.append(defsites[i].toString());
                System.out.println(sb);
            }
        }

        BitSet worklist;

        /*
         * For each register, compute all locations for phi placement
         * based on dominance-frontier algorithm.
         */
        for (int reg = 0, s = regCount; reg < s; reg++) {
            int workBlockIndex;

            /* Worklist set starts out with each node where reg is assigned. */

            worklist = (BitSet) (defsites[reg].clone());

            while (0 <= (workBlockIndex = worklist.nextSetBit(0))) {
                worklist.clear(workBlockIndex);
                IntIterator dfIterator
                    = domInfos[workBlockIndex].dominanceFrontiers.iterator();

                while (dfIterator.hasNext()) {
                    int dfBlockIndex = dfIterator.next();

                    if (!phisites[reg].get(dfBlockIndex)) {
                        phisites[reg].set(dfBlockIndex);

                        int tReg = reg + threshold;
                        RegisterSpec rs
                            = localInfo.getStarts(dfBlockIndex).get(tReg);

                        if (rs == null) {
                            ssaBlocks.get(dfBlockIndex).addPhiInsnForReg(tReg);
                        } else {
                            ssaBlocks.get(dfBlockIndex).addPhiInsnForReg(rs);
                        }

                        if (!defsites[reg].get(dfBlockIndex)) {
                            worklist.set(dfBlockIndex);
                        }
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println("phisites");

            for (int i = 0; i < regCount; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append('v').append(i).append(": ");
                sb.append(phisites[i].toString());
                System.out.println(sb);
            }
        }
    }
}
