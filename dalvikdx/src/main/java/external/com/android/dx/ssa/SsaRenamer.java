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

import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Complete transformation to SSA form by renaming all registers accessed.<p>
 *
 * See Appel algorithm 19.7<p>
 *
 * Unlike the original algorithm presented in Appel, this renamer converts
 * to a new flat (versionless) register space. The "version 0" registers,
 * which represent the initial state of the Rop registers and should never
 * actually be meaningfully accessed in a legal program, are represented
 * as the first N registers in the SSA namespace. Subsequent assignments
 * are assigned new unique names. Note that the incoming Rop representation
 * has a concept of register widths, where 64-bit values are stored into
 * two adjoining Rop registers. This adjoining register representation is
 * ignored in SSA form conversion and while in SSA form, each register can be e
 * either 32 or 64 bits wide depending on use. The adjoining-register
 * represention is re-created later when converting back to Rop form. <p>
 *
 * But, please note, the SSA Renamer's ignoring of the adjoining-register ROP
 * representation means that unaligned accesses to 64-bit registers are not
 * supported. For example, you cannot do a 32-bit operation on a portion of
 * a 64-bit register. This will never be observed to happen when coming
 * from Java code, of course.<p>
 *
 * The implementation here, rather than keeping a single register version
 * stack for the entire method as the dom tree is walked, instead keeps
 * a mapping table for the current block being processed. Once the
 * current block has been processed, this mapping table is then copied
 * and used as the initial state for child blocks.<p>
 */
public class SsaRenamer implements Runnable {
    /** debug flag */
    private static final boolean DEBUG = false;

    /** method we're processing */
    private final SsaMethod ssaMeth;

    /** next available SSA register */
    private int nextSsaReg;

    /** the number of original rop registers */
    private final int ropRegCount;

    /** work only on registers above this value */
    private int threshold;

    /**
     * indexed by block index; register version state for each block start.
     * This list is updated by each dom parent for its children. The only
     * sub-arrays that exist at any one time are the start states for blocks
     * yet to be processed by a {@code BlockRenamer} instance.
     */
    private final RegisterSpec[][] startsForBlocks;

    /** map of SSA register number to debug (local var names) or null of n/a */
    private final ArrayList<LocalItem> ssaRegToLocalItems;

    /**
     * maps SSA registers back to the original rop number. Used for
     * debug only.
     */
    private IntList ssaRegToRopReg;

    /**
     * Constructs an instance of the renamer
     *
     * @param ssaMeth {@code non-null;} un-renamed SSA method that will
     * be renamed.
     */
    public SsaRenamer(SsaMethod ssaMeth) {
        ropRegCount = ssaMeth.getRegCount();

        this.ssaMeth = ssaMeth;

        /*
         * Reserve the first N registers in the SSA register space for
         * "version 0" registers.
         */
        nextSsaReg = ropRegCount;
        threshold = 0;
        startsForBlocks = new RegisterSpec[ssaMeth.getBlocks().size()][];

        ssaRegToLocalItems = new ArrayList<LocalItem>();

        if (DEBUG) {
            ssaRegToRopReg = new IntList(ropRegCount);
        }

        /*
         * Appel 19.7
         *
         * Initialization:
         *   for each variable a        // register i
         *      Count[a] <- 0           // nextSsaReg, flattened
         *      Stack[a] <- 0           // versionStack
         *      push 0 onto Stack[a]
         *
         */

        // top entry for the version stack is version 0
        RegisterSpec[] initialRegMapping = new RegisterSpec[ropRegCount];
        for (int i = 0; i < ropRegCount; i++) {
            // everyone starts with a version 0 register
            initialRegMapping[i] = RegisterSpec.make(i, Type.VOID);

            if (DEBUG) {
                ssaRegToRopReg.add(i);
            }
        }

        // Initial state for entry block
        startsForBlocks[ssaMeth.getEntryBlockIndex()] = initialRegMapping;
    }

    /**
    * Constructs an instance of the renamer with threshold set
    *
    * @param ssaMeth {@code non-null;} un-renamed SSA method that will
    * be renamed.
    * @param thresh registers below this number are unchanged
    */
   public SsaRenamer(SsaMethod ssaMeth, int thresh) {
       this(ssaMeth);
       threshold = thresh;
   }

    /**
     * Performs renaming transformation, modifying the method's instructions
     * in-place.
     */
    @Override
    public void run() {
        // Rename each block in dom-tree DFS order.
        ssaMeth.forEachBlockDepthFirstDom(new SsaBasicBlock.Visitor() {
            @Override
            public void visitBlock (SsaBasicBlock block,
                    SsaBasicBlock unused) {
                new BlockRenamer(block).process();
            }
        });

        ssaMeth.setNewRegCount(nextSsaReg);
        ssaMeth.onInsnsChanged();

        if (DEBUG) {
            System.out.println("SSA\tRop");
            /*
             * We're going to compute the version of the rop register
             * by keeping a running total of how many times the rop
             * register has been mapped.
             */
            int[] versions = new int[ropRegCount];

            int sz = ssaRegToRopReg.size();
            for (int i = 0; i < sz; i++) {
                int ropReg = ssaRegToRopReg.get(i);
                System.out.println(i + "\t" + ropReg + "["
                        + versions[ropReg] + "]");
                versions[ropReg]++;
            }
        }
    }

    /**
     * Duplicates a RegisterSpec array.
     *
     * @param orig {@code non-null;} array to duplicate
     * @return {@code non-null;} new instance
     */
    private static  RegisterSpec[] dupArray(RegisterSpec[] orig) {
        RegisterSpec[] copy = new RegisterSpec[orig.length];

        System.arraycopy(orig, 0, copy, 0, orig.length);

        return copy;
    }

    /**
     * Gets a local variable item for a specified register.
     *
     * @param ssaReg register in SSA name space
     * @return {@code null-ok;} Local variable name or null if none
     */
    private LocalItem getLocalForNewReg(int ssaReg) {
        if (ssaReg < ssaRegToLocalItems.size()) {
            return ssaRegToLocalItems.get(ssaReg);
        } else {
            return null;
        }
    }

    /**
     * Records a debug (local variable) name for a specified register.
     *
     * @param ssaReg non-null named register spec in SSA name space
     */
    private void setNameForSsaReg(RegisterSpec ssaReg) {
        int reg = ssaReg.getReg();
        LocalItem local = ssaReg.getLocalItem();

        ssaRegToLocalItems.ensureCapacity(reg + 1);
        while (ssaRegToLocalItems.size() <= reg) {
            ssaRegToLocalItems.add(null);
        }

        ssaRegToLocalItems.set(reg, local);
    }

    /**
     * Returns true if this SSA register is below the specified threshold.
     * Used when most code is already in SSA form, and renaming is needed only
     * for registers above a certain threshold.
     *
     * @param ssaReg the SSA register in question
     * @return {@code true} if its register number is below the threshold
     */
    private boolean isBelowThresholdRegister(int ssaReg) {
        return ssaReg < threshold;
    }

    /**
     * Returns true if this SSA register is a "version 0"
     * register. All version 0 registers are assigned the first N register
     * numbers, where N is the count of original rop registers.
     *
     * @param ssaReg the SSA register in question
     * @return true if it is a version 0 register.
     */
    private boolean isVersionZeroRegister(int ssaReg) {
        return ssaReg < ropRegCount;
    }

    /**
     * Returns true if a and b are equal or are both null.
     *
     * @param a null-ok
     * @param b null-ok
     * @return Returns true if a and b are equal or are both null
     */
    private static boolean equalsHandlesNulls(Object a, Object b) {
        return a == b ||  (a != null && a.equals(b));
    }

    /**
     * Processes all insns in a block and renames their registers
     * as appropriate.
     */
    private class BlockRenamer implements SsaInsn.Visitor{
        /** {@code non-null;} block we're processing. */
        private final SsaBasicBlock block;

        /**
         * {@code non-null;} indexed by old register name. The current
         * top of the version stack as seen by this block. It's
         * initialized from the ending state of its dom parent,
         * updated as the block's instructions are processed, and then
         * copied to each one of its dom children.
         */
        private final RegisterSpec[] currentMapping;

        /**
         * contains the set of moves we need to keep to preserve local
         * var info. All other moves will be deleted.
         */
        private final HashSet<SsaInsn> movesToKeep;

        /**
         * maps the set of insns to replace after renaming is finished
         * on the block.
         */
        private final HashMap<SsaInsn, SsaInsn> insnsToReplace;

        private final RenamingMapper mapper;

        /**
         * Constructs a block renamer instance. Call {@code process}
         * to process.
         *
         * @param block {@code non-null;} block to process
         */
        BlockRenamer(final SsaBasicBlock block) {
            this.block = block;
            currentMapping = startsForBlocks[block.getIndex()];
            movesToKeep = new HashSet<SsaInsn>();
            insnsToReplace = new HashMap<SsaInsn, SsaInsn>();
            mapper =  new RenamingMapper();

            // We don't need our own start state anymore
            startsForBlocks[block.getIndex()] = null;
        }

        /**
         * Provides a register mapping between the old register space
         * and the current renaming mapping. The mapping is updated
         * as the current block's instructions are processed.
         */
        private class RenamingMapper extends RegisterMapper {
            public RenamingMapper() {
                // This space intentionally left blank.
            }

            /** {@inheritDoc} */
            @Override
            public int getNewRegisterCount() {
                return nextSsaReg;
            }

            /** {@inheritDoc} */
            @Override
            public RegisterSpec map(RegisterSpec registerSpec) {
                if (registerSpec == null) return null;

                int reg = registerSpec.getReg();

                // For debugging: assert that the mapped types are compatible.
                if (DEBUG) {
                    RegisterSpec newVersion = currentMapping[reg];
                    if (newVersion.getBasicType() != Type.BT_VOID
                            && registerSpec.getBasicFrameType()
                                != newVersion.getBasicFrameType()) {

                        throw new RuntimeException(
                                "mapping registers of incompatible types! "
                                + registerSpec
                                + " " + currentMapping[reg]);
                    }
                }

                return registerSpec.withReg(currentMapping[reg].getReg());
            }
        }

        /**
         * Renames all the variables in this block and inserts appriopriate
         * phis in successor blocks.
         */
        public void process() {
            /*
             * From Appel:
             *
             * Rename(n) =
             *   for each statement S in block n   // 'statement' in 'block'
             */

            block.forEachInsn(this);

            updateSuccessorPhis();

            // Delete all move insns in this block.
            ArrayList<SsaInsn> insns = block.getInsns();
            int szInsns = insns.size();

            for (int i = szInsns - 1; i >= 0 ; i--) {
                SsaInsn insn = insns.get(i);
                SsaInsn replaceInsn;

                replaceInsn = insnsToReplace.get(insn);

                if (replaceInsn != null) {
                    insns.set(i, replaceInsn);
                } else if (insn.isNormalMoveInsn()
                        && !movesToKeep.contains(insn)) {
                    insns.remove(i);
                }
            }

            // Store the start states for our dom children.
            boolean first = true;
            for (SsaBasicBlock child : block.getDomChildren()) {
                if (child != block) {
                    // Don't bother duplicating the array for the first child.
                    RegisterSpec[] childStart = first ? currentMapping
                        : dupArray(currentMapping);

                    startsForBlocks[child.getIndex()] = childStart;
                    first = false;
                }
            }

            // currentMapping is owned by a child now.
        }

        /**
         * Enforces a few contraints when a register mapping is added.
         *
         * <ol>
         * <li> Ensures that all new SSA registers specs in the mapping
         * table with the same register number are identical. In effect, once
         * an SSA register spec has received or lost a local variable name,
         * then every old-namespace register that maps to it should gain or
         * lose its local variable name as well.
         * <li> Records the local name associated with the
         * register so that a register is never associated with more than one
         * local.
         * <li> ensures that only one SSA register
         * at a time is considered to be associated with a local variable. When
         * {@code currentMapping} is updated and the newly added element
         * is named, strip that name from any other SSA registers.
         * </ol>
         *
         * @param ropReg {@code >= 0;} rop register number
         * @param ssaReg {@code non-null;} an SSA register that has just
         * been added to {@code currentMapping}
         */
        private void addMapping(int ropReg, RegisterSpec ssaReg) {
            int ssaRegNum = ssaReg.getReg();
            LocalItem ssaRegLocal = ssaReg.getLocalItem();

            currentMapping[ropReg] = ssaReg;

            /*
             * Ensure all SSA register specs with the same reg are identical.
             */
            for (int i = currentMapping.length - 1; i >= 0; i--) {
                RegisterSpec cur = currentMapping[i];

                if (ssaRegNum == cur.getReg()) {
                    currentMapping[i] = ssaReg;
                }
            }

            // All further steps are for registers with local information.
            if (ssaRegLocal == null) {
                return;
            }

            // Record that this SSA reg has been associated with a local.
            setNameForSsaReg(ssaReg);

            // Ensure that no other SSA regs are associated with this local.
            for (int i = currentMapping.length - 1; i >= 0; i--) {
                RegisterSpec cur = currentMapping[i];

                if (ssaRegNum != cur.getReg()
                        && ssaRegLocal.equals(cur.getLocalItem())) {
                    currentMapping[i] = cur.withLocalItem(null);
                }
            }
        }

        /**
         * {@inheritDoc}
         *
         * Phi insns have their result registers renamed.
         */
        @Override
        public void visitPhiInsn(PhiInsn phi) {
            /* don't process sources for phi's */
            processResultReg(phi);
        }

        /**
         * {@inheritDoc}
         *
         * Move insns are treated as a simple mapping operation, and
         * will later be removed unless they represent a local variable
         * assignment. If they represent a local variable assignement, they
         * are preserved.
         */
        @Override
        public void visitMoveInsn(NormalSsaInsn insn) {
            /*
             * For moves: copy propogate the move if we can, but don't
             * if we need to preserve local variable info and the
             * result has a different name than the source.
             */

            RegisterSpec ropResult = insn.getResult();
            int ropResultReg = ropResult.getReg();
            int ropSourceReg = insn.getSources().get(0).getReg();

            insn.mapSourceRegisters(mapper);
            int ssaSourceReg = insn.getSources().get(0).getReg();

            LocalItem sourceLocal
                = currentMapping[ropSourceReg].getLocalItem();
            LocalItem resultLocal = ropResult.getLocalItem();

            /*
             * A move from a register that's currently associated with a local
             * to one that will not be associated with a local does not need
             * to be preserved, but the local association should remain.
             * Hence, we inherit the sourceLocal where the resultLocal is null.
             */

            LocalItem newLocal
                = (resultLocal == null) ? sourceLocal : resultLocal;
            LocalItem associatedLocal = getLocalForNewReg(ssaSourceReg);

            /*
             * If we take the new local, will only one local have ever
             * been associated with this SSA reg?
             */
            boolean onlyOneAssociatedLocal
                    = associatedLocal == null || newLocal == null
                    || newLocal.equals(associatedLocal);

            /*
             * If we're going to copy-propogate, then the ssa register
             * spec that's going to go into the mapping is made up of
             * the source register number mapped from above, the type
             * of the result, and the name either from the result (if
             * specified) or inherited from the existing mapping.
             *
             * The move source has incomplete type information in null
             * object cases, so the result type is used.
             */
            RegisterSpec ssaReg
                    = RegisterSpec.makeLocalOptional(
                        ssaSourceReg, ropResult.getType(), newLocal);

            if (!Optimizer.getPreserveLocals() || (onlyOneAssociatedLocal
                    && equalsHandlesNulls(newLocal, sourceLocal)) &&
                    threshold == 0) {
                /*
                 * We don't have to keep this move to preserve local
                 * information. Either the name is the same, or the result
                 * register spec is unnamed.
                 */

                addMapping(ropResultReg, ssaReg);
            } else if (onlyOneAssociatedLocal && sourceLocal == null &&
                    threshold == 0) {
                /*
                 * The register was previously unnamed. This means that a
                 * local starts after it's first assignment in SSA form
                 */

                RegisterSpecList ssaSources = RegisterSpecList.make(
                        RegisterSpec.make(ssaReg.getReg(),
                                ssaReg.getType(), newLocal));

                SsaInsn newInsn
                        = SsaInsn.makeFromRop(
                            new PlainInsn(Rops.opMarkLocal(ssaReg),
                            SourcePosition.NO_INFO, null, ssaSources),block);

                insnsToReplace.put(insn, newInsn);

                // Just map as above.
                addMapping(ropResultReg, ssaReg);
            } else {
                /*
                 * Do not copy-propogate, since the two registers have
                 * two different local-variable names.
                 */
                processResultReg(insn);

                movesToKeep.add(insn);
            }
        }

        /**
         * {@inheritDoc}
         *
         * All insns that are not move or phi insns have their source registers
         * mapped ot the current mapping. Their result registers are then
         * renamed to a new SSA register which is then added to the current
         * register mapping.
         */
        @Override
        public void visitNonMoveInsn(NormalSsaInsn insn) {
            /* for each use of some variable X in S */
            insn.mapSourceRegisters(mapper);

            processResultReg(insn);
        }

        /**
         * Renames the result register of this insn and updates the
         * current register mapping. Does nothing if this insn has no result.
         * Applied to all non-move insns.
         *
         * @param insn insn to process.
         */
        void processResultReg(SsaInsn insn) {
            RegisterSpec ropResult = insn.getResult();

            if (ropResult == null) {
                return;
            }

            int ropReg = ropResult.getReg();
            if (isBelowThresholdRegister(ropReg)) {
                return;
            }

            insn.changeResultReg(nextSsaReg);
            addMapping(ropReg, insn.getResult());

            if (DEBUG) {
                ssaRegToRopReg.add(ropReg);
            }

            nextSsaReg++;
        }

        /**
         * Updates the phi insns in successor blocks with operands based
         * on the current mapping of the rop register the phis represent.
         */
        private void updateSuccessorPhis() {
            PhiInsn.Visitor visitor = new PhiInsn.Visitor() {
                @Override
                public void visitPhiInsn (PhiInsn insn) {
                    int ropReg;

                    ropReg = insn.getRopResultReg();
                    if (isBelowThresholdRegister(ropReg)) {
                        return;
                    }

                    /*
                     * Never add a version 0 register as a phi
                     * operand. Version 0 registers represent the
                     * initial register state, and thus are never
                     * significant. Furthermore, the register liveness
                     * algorithm doesn't properly count them as "live
                     * in" at the beginning of the method.
                     */

                    RegisterSpec stackTop = currentMapping[ropReg];
                    if (!isVersionZeroRegister(stackTop.getReg())) {
                        insn.addPhiOperand(stackTop, block);
                    }
                }
            };

            BitSet successors = block.getSuccessors();
            for (int i = successors.nextSetBit(0); i >= 0;
                    i = successors.nextSetBit(i + 1)) {
                SsaBasicBlock successor = ssaMeth.getBlocks().get(i);
                successor.forEachPhiInsn(visitor);
            }
        }
    }
}
