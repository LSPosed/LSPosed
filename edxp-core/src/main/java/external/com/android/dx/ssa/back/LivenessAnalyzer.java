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

package external.com.android.dx.ssa.back;

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.ssa.PhiInsn;
import external.com.android.dx.ssa.SsaBasicBlock;
import external.com.android.dx.ssa.SsaInsn;
import external.com.android.dx.ssa.SsaMethod;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * From Appel "Modern Compiler Implementation in Java" algorithm 19.17
 * Calculate the live ranges for register {@code reg}.<p>
 *
 * v = regV <p>
 * s = insn <p>
 * M = visitedBlocks <p>
 */
public class LivenessAnalyzer {
    /**
     * {@code non-null;} index by basic block indexed set of basic blocks
     * that have already been visited. "M" as written in the original Appel
     * algorithm.
     */
    private final BitSet visitedBlocks;

    /**
     * {@code non-null;} set of blocks remaing to visit as "live out as block"
     */
    private final BitSet liveOutBlocks;

    /**
     * {@code >=0;} SSA register currently being analyzed.
     * "v" in the original Appel algorithm.
     */
    private final int regV;

    /** method to process */
    private final SsaMethod ssaMeth;

    /** interference graph being updated */
    private final InterferenceGraph interference;

    /** block "n" in Appel 19.17 */
    private SsaBasicBlock blockN;

    /** index of statement {@code s} in {@code blockN} */
    private int statementIndex;

    /** the next function to call */
    private NextFunction nextFunction;

    /** constants for {@link #nextFunction} */
    private static enum NextFunction {
        LIVE_IN_AT_STATEMENT,
            LIVE_OUT_AT_STATEMENT,
            LIVE_OUT_AT_BLOCK,
            DONE;
    }

    /**
     * Runs register liveness algorithm for a method, updating the
     * live in/out information in {@code SsaBasicBlock} instances and
     * returning an interference graph.
     *
     * @param ssaMeth {@code non-null;} method to process
     * @return {@code non-null;} interference graph indexed by SSA
     * registers in both directions
     */
    public static InterferenceGraph constructInterferenceGraph(
            SsaMethod ssaMeth) {
        int szRegs = ssaMeth.getRegCount();
        InterferenceGraph interference = new InterferenceGraph(szRegs);

        for (int i = 0; i < szRegs; i++) {
            new LivenessAnalyzer(ssaMeth, i, interference).run();
        }

        coInterferePhis(ssaMeth, interference);

        return interference;
    }

    /**
     * Makes liveness analyzer instance for specific register.
     *
     * @param ssaMeth {@code non-null;} method to process
     * @param reg register whose liveness to analyze
     * @param interference {@code non-null;} indexed by SSA reg in
     * both dimensions; graph to update
     *
     */
    private LivenessAnalyzer(SsaMethod ssaMeth, int reg,
            InterferenceGraph interference) {
        int blocksSz = ssaMeth.getBlocks().size();

        this.ssaMeth = ssaMeth;
        this.regV = reg;
        visitedBlocks = new BitSet(blocksSz);
        liveOutBlocks = new BitSet(blocksSz);
        this.interference = interference;
    }

    /**
     * The algorithm in Appel is presented in partial tail-recursion
     * form. Obviously, that's not efficient in java, so this function
     * serves as the dispatcher instead.
     */
    private void handleTailRecursion() {
        while (nextFunction != NextFunction.DONE) {
            switch (nextFunction) {
                case LIVE_IN_AT_STATEMENT:
                    nextFunction = NextFunction.DONE;
                    liveInAtStatement();
                    break;

                case LIVE_OUT_AT_STATEMENT:
                    nextFunction = NextFunction.DONE;
                    liveOutAtStatement();
                    break;

                case LIVE_OUT_AT_BLOCK:
                    nextFunction = NextFunction.DONE;
                    liveOutAtBlock();
                    break;

                default:
            }
        }
    }

    /**
     * From Appel algorithm 19.17.
     */
    public void run() {
        List<SsaInsn> useList = ssaMeth.getUseListForRegister(regV);

        for (SsaInsn insn : useList) {
            nextFunction = NextFunction.DONE;

            if (insn instanceof PhiInsn) {
                // If s is a phi-function with V as it's ith argument.
                PhiInsn phi = (PhiInsn) insn;

                for (SsaBasicBlock pred :
                         phi.predBlocksForReg(regV, ssaMeth)) {
                    blockN = pred;

                    nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
                    handleTailRecursion();
                }
            } else {
                blockN = insn.getBlock();
                statementIndex = blockN.getInsns().indexOf(insn);

                if (statementIndex < 0) {
                    throw new RuntimeException(
                            "insn not found in it's own block");
                }

                nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
                handleTailRecursion();
            }
        }

        int nextLiveOutBlock;
        while ((nextLiveOutBlock = liveOutBlocks.nextSetBit(0)) >= 0) {
            blockN = ssaMeth.getBlocks().get(nextLiveOutBlock);
            liveOutBlocks.clear(nextLiveOutBlock);
            nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
            handleTailRecursion();
        }
    }

    /**
     * "v is live-out at n."
     */
    private void liveOutAtBlock() {
        if (! visitedBlocks.get(blockN.getIndex())) {
            visitedBlocks.set(blockN.getIndex());

            blockN.addLiveOut(regV);

            ArrayList<SsaInsn> insns;

            insns = blockN.getInsns();

            // Live out at last statement in blockN
            statementIndex = insns.size() - 1;
            nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
        }
    }

    /**
     * "v is live-in at s."
     */
    private void liveInAtStatement() {
        // if s is the first statement in block N
        if (statementIndex == 0) {
            // v is live-in at n
            blockN.addLiveIn(regV);

            BitSet preds = blockN.getPredecessors();

            liveOutBlocks.or(preds);
        } else {
            // Let s' be the statement preceeding s
            statementIndex -= 1;
            nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
        }
    }

    /**
     * "v is live-out at s."
     */
    private void liveOutAtStatement() {
        SsaInsn statement = blockN.getInsns().get(statementIndex);
        RegisterSpec rs = statement.getResult();

        if (!statement.isResultReg(regV)) {
            if (rs != null) {
                interference.add(regV, rs.getReg());
            }
            nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
        }
    }

    /**
     * Ensures that all the phi result registers for all the phis in the
     * same basic block interfere with each other, and also that a phi's source
     * registers interfere with the result registers from other phis. This is needed since
     * the dead code remover has allowed through "dead-end phis" whose
     * results are not used except as local assignments. Without this step,
     * a the result of a dead-end phi might be assigned the same register
     * as the result of another phi, and the phi removal move scheduler may
     * generate moves that over-write the live result.
     *
     * @param ssaMeth {@code non-null;} method to process
     * @param interference {@code non-null;} interference graph
     */
    private static void coInterferePhis(SsaMethod ssaMeth,
            InterferenceGraph interference) {
        for (SsaBasicBlock b : ssaMeth.getBlocks()) {
            List<SsaInsn> phis = b.getPhiInsns();

            int szPhis = phis.size();

            for (int i = 0; i < szPhis; i++) {
                for (int j = 0; j < szPhis; j++) {
                    if (i == j) {
                        continue;
                    }

                    SsaInsn first = phis.get(i);
                    SsaInsn second = phis.get(j);
                    coInterferePhiRegisters(interference, first.getResult(), second.getSources());
                    coInterferePhiRegisters(interference, second.getResult(), first.getSources());
                    interference.add(first.getResult().getReg(), second.getResult().getReg());
                }
            }
        }
    }

    private static void coInterferePhiRegisters(InterferenceGraph interference, RegisterSpec result,
            RegisterSpecList sources) {
        int resultReg = result.getReg();
        for (int i = 0; i < sources.size(); ++i) {
            interference.add(resultReg, sources.get(i).getReg());
        }
    }
}
