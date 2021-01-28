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
import external.com.android.dx.rop.code.RegisterSpecList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/**
 * A variation on Appel Algorithm 19.12 "Dead code elimination in SSA form".
 *
 * TODO this algorithm is more efficient if run in reverse from exit
 * block to entry block.
 */
public class DeadCodeRemover {
    /** method we're processing */
    private final SsaMethod ssaMeth;

    /** ssaMeth.getRegCount() */
    private final int regCount;

    /**
     * indexed by register: whether reg should be examined
     * (does it correspond to a no-side-effect insn?)
     */
    private final BitSet worklist;

    /** use list indexed by register; modified during operation */
    private final ArrayList<SsaInsn>[] useList;

    /**
     * Process a method with the dead-code remver
     *
     * @param ssaMethod method to process
     */
    public static void process(SsaMethod ssaMethod) {
        DeadCodeRemover dc = new DeadCodeRemover(ssaMethod);
        dc.run();
    }

    /**
     * Constructs an instance.
     *
     * @param ssaMethod method to process
     */
    private DeadCodeRemover(SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;

        regCount = ssaMethod.getRegCount();
        worklist = new BitSet(regCount);
        useList = ssaMeth.getUseListCopy();
    }

    /**
     * Runs the dead code remover.
     */
    private void run() {
        pruneDeadInstructions();

        HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();

        ssaMeth.forEachInsn(new NoSideEffectVisitor(worklist));

        int regV;

        while ( 0 <= (regV = worklist.nextSetBit(0)) ) {
            worklist.clear(regV);

            if (useList[regV].size() == 0
                    || isCircularNoSideEffect(regV, null)) {

                SsaInsn insnS = ssaMeth.getDefinitionForRegister(regV);

                // This insn has already been deleted.
                if (deletedInsns.contains(insnS)) {
                    continue;
                }

                RegisterSpecList sources = insnS.getSources();

                int sz = sources.size();
                for (int i = 0; i < sz; i++) {
                    // Delete this insn from all usage lists.
                    RegisterSpec source = sources.get(i);
                    useList[source.getReg()].remove(insnS);

                    if (!hasSideEffect(
                            ssaMeth.getDefinitionForRegister(
                                    source.getReg()))) {
                        /*
                         * Only registers whose definition has no side effect
                         * should be added back to the worklist.
                         */
                        worklist.set(source.getReg());
                    }
                }

                // Schedule this insn for later deletion.
                deletedInsns.add(insnS);
            }
        }

        ssaMeth.deleteInsns(deletedInsns);
    }

    /**
     * Removes all instructions from every unreachable block.
     */
    private void pruneDeadInstructions() {
        HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();

        BitSet reachable = ssaMeth.computeReachability();
        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();
        int blockIndex = 0;

        while ((blockIndex = reachable.nextClearBit(blockIndex)) < blocks.size()) {
            SsaBasicBlock block = blocks.get(blockIndex);
            blockIndex++;

            // Prune instructions from unreachable blocks
            for (int i = 0; i < block.getInsns().size(); i++) {
                SsaInsn insn = block.getInsns().get(i);
                RegisterSpecList sources = insn.getSources();
                int sourcesSize = sources.size();

                // Delete this instruction completely if it has sources
                if (sourcesSize != 0) {
                    deletedInsns.add(insn);
                }

                // Delete this instruction from all usage lists.
                for (int j = 0; j < sourcesSize; j++) {
                    RegisterSpec source = sources.get(j);
                    useList[source.getReg()].remove(insn);
                }

                // Remove this instruction result from the sources of any phis
                RegisterSpec result = insn.getResult();
                if (result == null) continue;
                for (SsaInsn use : useList[result.getReg()]) {
                    if (use instanceof PhiInsn) {
                        PhiInsn phiUse = (PhiInsn) use;
                        phiUse.removePhiRegister(result);
                    }
                }
            }
        }

        ssaMeth.deleteInsns(deletedInsns);
    }

    /**
     * Returns true if the only uses of this register form a circle of
     * operations with no side effects.
     *
     * @param regV register to examine
     * @param set a set of registers that we've already determined
     * are only used as sources in operations with no side effect or null
     * if this is the first recursion
     * @return true if usage is circular without side effect
     */
    private boolean isCircularNoSideEffect(int regV, BitSet set) {
        if ((set != null) && set.get(regV)) {
            return true;
        }

        for (SsaInsn use : useList[regV]) {
            if (hasSideEffect(use)) {
                return false;
            }
        }

        if (set == null) {
            set = new BitSet(regCount);
        }

        // This register is only used in operations that have no side effect.
        set.set(regV);

        for (SsaInsn use : useList[regV]) {
            RegisterSpec result = use.getResult();

            if (result == null
                    || !isCircularNoSideEffect(result.getReg(), set)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if this insn has a side-effect. Returns true
     * if the insn is null for reasons stated in the code block.
     *
     * @param insn {@code null-ok;} instruction in question
     * @return true if it has a side-effect
     */
    private static boolean hasSideEffect(SsaInsn insn) {
        if (insn == null) {
            /* While false would seem to make more sense here, true
             * prevents us from adding this back to a worklist unnecessarally.
             */
            return true;
        }

        return insn.hasSideEffect();
    }

    /**
     * A callback class used to build up the initial worklist of
     * registers defined by an instruction with no side effect.
     */
    static private class NoSideEffectVisitor implements SsaInsn.Visitor {
        BitSet noSideEffectRegs;

        /**
         * Passes in data structures that will be filled out after
         * ssaMeth.forEachInsn() is called with this instance.
         *
         * @param noSideEffectRegs to-build bitset of regs that are
         * results of regs with no side effects
         */
        public NoSideEffectVisitor(BitSet noSideEffectRegs) {
            this.noSideEffectRegs = noSideEffectRegs;
        }

        /** {@inheritDoc} */
        @Override
        public void visitMoveInsn (NormalSsaInsn insn) {
            // If we're tracking local vars, some moves have side effects.
            if (!hasSideEffect(insn)) {
                noSideEffectRegs.set(insn.getResult().getReg());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitPhiInsn (PhiInsn phi) {
            // If we're tracking local vars, then some phis have side effects.
            if (!hasSideEffect(phi)) {
                noSideEffectRegs.set(phi.getResult().getReg());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitNonMoveInsn (NormalSsaInsn insn) {
            RegisterSpec result = insn.getResult();
            if (!hasSideEffect(insn) && result != null) {
                noSideEffectRegs.set(result.getReg());
            }
        }
    }
}
