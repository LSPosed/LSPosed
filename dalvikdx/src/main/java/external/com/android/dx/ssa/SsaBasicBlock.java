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

import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InsnList;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import external.com.android.dx.util.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An SSA representation of a basic block.
 */
public final class SsaBasicBlock {
    /**
     * {@code non-null;} comparator for instances of this class that
     * just compares block labels
     */
    public static final Comparator<SsaBasicBlock> LABEL_COMPARATOR =
        new LabelComparator();

    /** {@code non-null;} insn list associated with this instance */
    private final ArrayList<SsaInsn> insns;

    /** {@code non-null;} predecessor set (by block list index) */
    private BitSet predecessors;

    /** {@code non-null;} successor set (by block list index) */
    private BitSet successors;

    /**
     * {@code non-null;} ordered successor list
     * (same block may be listed more than once)
     */
    private IntList successorList;

    /**
     * block list index of primary successor, or {@code -1} for no primary
     * successor
     */
    private int primarySuccessor = -1;

    /** label of block in rop form */
    private final int ropLabel;

    /** {@code non-null;} method we belong to */
    private final SsaMethod parent;

    /** our index into parent.getBlock() */
    private final int index;

    /** list of dom children */
    private final ArrayList<SsaBasicBlock> domChildren;

    /**
     * the number of moves added to the end of the block during the
     * phi-removal process. Retained for subsequent move scheduling.
     */
    private int movesFromPhisAtEnd = 0;

    /**
     * the number of moves added to the beginning of the block during the
     * phi-removal process. Retained for subsequent move scheduling.
     */
    private int movesFromPhisAtBeginning = 0;

    /**
     * {@code null-ok;} indexed by reg: the regs that are live-in at
     * this block
     */
    private IntSet liveIn;

    /**
     * {@code null-ok;} indexed by reg: the regs that are live-out at
     * this block
     */
    private IntSet liveOut;

    /**
     * Creates a new empty basic block.
     *
     * @param basicBlockIndex index this block will have
     * @param ropLabel original rop-form label
     * @param parent method of this block
     */
    public SsaBasicBlock(final int basicBlockIndex, final int ropLabel,
            final SsaMethod parent) {
        this.parent = parent;
        this.index = basicBlockIndex;
        this.insns = new ArrayList<SsaInsn>();
        this.ropLabel = ropLabel;

        this.predecessors = new BitSet(parent.getBlocks().size());
        this.successors = new BitSet(parent.getBlocks().size());
        this.successorList = new IntList();

        domChildren = new ArrayList<SsaBasicBlock>();
    }

    /**
     * Creates a new SSA basic block from a ROP form basic block.
     *
     * @param rmeth original method
     * @param basicBlockIndex index this block will have
     * @param parent method of this block predecessor set will be
     * updated
     * @return new instance
     */
    public static SsaBasicBlock newFromRop(RopMethod rmeth,
            int basicBlockIndex, final SsaMethod parent) {
        BasicBlockList ropBlocks = rmeth.getBlocks();
        BasicBlock bb = ropBlocks.get(basicBlockIndex);
        SsaBasicBlock result =
            new SsaBasicBlock(basicBlockIndex, bb.getLabel(), parent);
        InsnList ropInsns = bb.getInsns();

        result.insns.ensureCapacity(ropInsns.size());

        for (int i = 0, sz = ropInsns.size() ; i < sz ; i++) {
            result.insns.add(new NormalSsaInsn (ropInsns.get(i), result));
        }

        result.predecessors = SsaMethod.bitSetFromLabelList(
                ropBlocks,
                rmeth.labelToPredecessors(bb.getLabel()));

        result.successors
                = SsaMethod.bitSetFromLabelList(ropBlocks, bb.getSuccessors());

        result.successorList
                = SsaMethod.indexListFromLabelList(ropBlocks,
                    bb.getSuccessors());

        if (result.successorList.size() != 0) {
            int primarySuccessor = bb.getPrimarySuccessor();

            result.primarySuccessor = (primarySuccessor < 0)
                    ? -1 : ropBlocks.indexOfLabel(primarySuccessor);
        }

        return result;
    }

    /**
     * Adds a basic block as a dom child for this block. Used when constructing
     * the dom tree.
     *
     * @param child {@code non-null;} new dom child
     */
    public void addDomChild(SsaBasicBlock child) {
        domChildren.add(child);
    }

    /**
     * Gets the dom children for this node. Don't modify this list.
     *
     * @return {@code non-null;} list of dom children
     */
    public ArrayList<SsaBasicBlock> getDomChildren() {
        return domChildren;
    }

    /**
     * Adds a phi insn to the beginning of this block. The result type of
     * the phi will be set to void, to indicate that it's currently unknown.
     *
     * @param reg {@code >=0;} result reg
     */
    public void addPhiInsnForReg(int reg) {
        insns.add(0, new PhiInsn(reg, this));
    }

    /**
     * Adds a phi insn to the beginning of this block. This is to be used
     * when the result type or local-association can be determined at phi
     * insert time.
     *
     * @param resultSpec {@code non-null;} reg
     */
    public void addPhiInsnForReg(RegisterSpec resultSpec) {
        insns.add(0, new PhiInsn(resultSpec, this));
    }

    /**
     * Adds an insn to the head of this basic block, just after any phi
     * insns.
     *
     * @param insn {@code non-null;} rop-form insn to add
     */
    public void addInsnToHead(Insn insn) {
        SsaInsn newInsn = SsaInsn.makeFromRop(insn, this);
        insns.add(getCountPhiInsns(), newInsn);
        parent.onInsnAdded(newInsn);
    }

    /**
     * Replaces the last insn in this block. The provided insn must have
     * some branchingness.
     *
     * @param insn {@code non-null;} rop-form insn to add, which must branch.
     */
    public void replaceLastInsn(Insn insn) {
        if (insn.getOpcode().getBranchingness() == Rop.BRANCH_NONE) {
            throw new IllegalArgumentException("last insn must branch");
        }

        SsaInsn oldInsn = insns.get(insns.size() - 1);
        SsaInsn newInsn = SsaInsn.makeFromRop(insn, this);

        insns.set(insns.size() - 1, newInsn);

        parent.onInsnRemoved(oldInsn);
        parent.onInsnAdded(newInsn);
    }

    /**
     * Visits each phi insn.
     *
     * @param v {@code non-null;} the callback
     */
    public void forEachPhiInsn(PhiInsn.Visitor v) {
        int sz = insns.size();

        for (int i = 0; i < sz; i++) {
            SsaInsn insn = insns.get(i);
            if (insn instanceof PhiInsn) {
                v.visitPhiInsn((PhiInsn) insn);
            } else {
                /*
                 * Presently we assume PhiInsn's are in a continuous
                 * block at the top of the list
                 */
                break;
            }
        }
    }

    /**
     * Deletes all phi insns. Do this after adding appropriate move insns.
     */
    public void removeAllPhiInsns() {
        /*
         * Presently we assume PhiInsn's are in a continuous
         * block at the top of the list.
         */

        insns.subList(0, getCountPhiInsns()).clear();
    }

    /**
     * Gets the number of phi insns at the top of this basic block.
     *
     * @return count of phi insns
     */
    private int getCountPhiInsns() {
        int countPhiInsns;

        int sz = insns.size();
        for (countPhiInsns = 0; countPhiInsns < sz; countPhiInsns++) {
            SsaInsn insn = insns.get(countPhiInsns);
            if (!(insn instanceof PhiInsn)) {
                break;
            }
        }

        return countPhiInsns;
    }

    /**
     * @return {@code non-null;} the (mutable) instruction list for this block,
     * with phi insns at the beginning
     */
    public ArrayList<SsaInsn> getInsns() {
        return insns;
    }

    /**
     * @return {@code non-null;} the (mutable) list of phi insns for this block
     */
    public List<SsaInsn> getPhiInsns() {
        return insns.subList(0, getCountPhiInsns());
    }

    /**
     * @return the block index of this block
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the label of this block in rop form
     */
    public int getRopLabel() {
        return ropLabel;
    }

    /**
     * @return the label of this block in rop form as a hex string
     */
    public String getRopLabelString() {
        return Hex.u2(ropLabel);
    }

    /**
     * @return {@code non-null;} predecessors set, indexed by block index
     */
    public BitSet getPredecessors() {
        return predecessors;
    }

    /**
     * @return {@code non-null;} successors set, indexed by block index
     */
    public BitSet getSuccessors() {
        return successors;
    }

    /**
     * @return {@code non-null;} ordered successor list, containing block
     * indicies
     */
    public IntList getSuccessorList() {
        return successorList;
    }

    /**
     * @return {@code >= -1;} block index of primary successor or
     * {@code -1} if no primary successor
     */
    public int getPrimarySuccessorIndex() {
        return primarySuccessor;
    }

    /**
     * @return rop label of primary successor
     */
    public int getPrimarySuccessorRopLabel() {
        return parent.blockIndexToRopLabel(primarySuccessor);
    }

    /**
     * @return {@code null-ok;} the primary successor block or {@code null}
     * if there is none
     */
    public SsaBasicBlock getPrimarySuccessor() {
        if (primarySuccessor < 0) {
            return null;
        } else {
            return parent.getBlocks().get(primarySuccessor);
        }
    }

    /**
     * @return successor list of rop labels
     */
    public IntList getRopLabelSuccessorList() {
        IntList result = new IntList(successorList.size());

        int sz = successorList.size();

        for (int i = 0; i < sz; i++) {
            result.add(parent.blockIndexToRopLabel(successorList.get(i)));
        }
        return result;
    }

    /**
     * @return {@code non-null;} method that contains this block
     */
    public SsaMethod getParent() {
        return parent;
    }

    /**
     * Inserts a new empty GOTO block as a predecessor to this block.
     * All previous predecessors will be predecessors to the new block.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public SsaBasicBlock insertNewPredecessor() {
        SsaBasicBlock newPred = parent.makeNewGotoBlock();

        // Update the new block.
        newPred.predecessors = predecessors;
        newPred.successors.set(index) ;
        newPred.successorList.add(index);
        newPred.primarySuccessor = index;


        // Update us.
        predecessors = new BitSet(parent.getBlocks().size());
        predecessors.set(newPred.index);

        // Update our (soon-to-be) old predecessors.
        for (int i = newPred.predecessors.nextSetBit(0); i >= 0;
                i = newPred.predecessors.nextSetBit(i + 1)) {

            SsaBasicBlock predBlock = parent.getBlocks().get(i);

            predBlock.replaceSuccessor(index, newPred.index);
        }

        return newPred;
    }

    /**
     * Constructs and inserts a new empty GOTO block {@code Z} between
     * this block ({@code A}) and a current successor block
     * ({@code B}). The new block will replace B as A's successor and
     * A as B's predecessor. A and B will no longer be directly connected.
     * If B is listed as a successor multiple times, all references
     * are replaced.
     *
     * @param other current successor (B)
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public SsaBasicBlock insertNewSuccessor(SsaBasicBlock other) {
        SsaBasicBlock newSucc = parent.makeNewGotoBlock();

        if (!successors.get(other.index)) {
            throw new RuntimeException("Block " + other.getRopLabelString()
                    + " not successor of " + getRopLabelString());
        }

        // Update the new block.
        newSucc.predecessors.set(this.index);
        newSucc.successors.set(other.index) ;
        newSucc.successorList.add(other.index);
        newSucc.primarySuccessor = other.index;

        // Update us.
        for (int i = successorList.size() - 1 ;  i >= 0; i--) {
            if (successorList.get(i) == other.index) {
                successorList.set(i, newSucc.index);
            }
        }

        if (primarySuccessor == other.index) {
            primarySuccessor = newSucc.index;
        }
        successors.clear(other.index);
        successors.set(newSucc.index);

        // Update "other".
        other.predecessors.set(newSucc.index);
        other.predecessors.set(index, successors.get(other.index));

        return newSucc;
    }

    /**
     * Replaces an old successor with a new successor. This will throw
     * RuntimeException if {@code oldIndex} was not a successor.
     *
     * @param oldIndex index of old successor block
     * @param newIndex index of new successor block
     */
    public void replaceSuccessor(int oldIndex, int newIndex) {
        if (oldIndex == newIndex) {
            return;
        }

        // Update us.
        successors.set(newIndex);

        if (primarySuccessor == oldIndex) {
            primarySuccessor = newIndex;
        }

        for (int i = successorList.size() - 1 ;  i >= 0; i--) {
            if (successorList.get(i) == oldIndex) {
                successorList.set(i, newIndex);
            }
        }

        successors.clear(oldIndex);

        // Update new successor.
        parent.getBlocks().get(newIndex).predecessors.set(index);

        // Update old successor.
        parent.getBlocks().get(oldIndex).predecessors.clear(index);
    }

    /**
     * Removes a successor from this block's successor list.
     *
     * @param oldIndex index of successor block to remove
     */
    public void removeSuccessor(int oldIndex) {
        int removeIndex = 0;

        for (int i = successorList.size() - 1; i >= 0; i--) {
            if (successorList.get(i) == oldIndex) {
                removeIndex = i;
            } else {
                primarySuccessor = successorList.get(i);
            }
        }

        successorList.removeIndex(removeIndex);
        successors.clear(oldIndex);
        parent.getBlocks().get(oldIndex).predecessors.clear(index);
    }

    /**
     * Attaches block to an exit block if necessary. If this block
     * is not an exit predecessor or is the exit block, this block does
     * nothing. For use by {@link external.com.android.dx.ssa.SsaMethod#makeExitBlock}
     *
     * @param exitBlock {@code non-null;} exit block
     */
    public void exitBlockFixup(SsaBasicBlock exitBlock) {
        if (this == exitBlock) {
            return;
        }

        if (successorList.size() == 0) {
            /*
             * This is an exit predecessor.
             * Set the successor to the exit block
             */
            successors.set(exitBlock.index);
            successorList.add(exitBlock.index);
            primarySuccessor = exitBlock.index;
            exitBlock.predecessors.set(this.index);
        }
    }

    /**
     * Adds a move instruction to the end of this basic block, just
     * before the last instruction. If the result of the final instruction
     * is the source in question, then the move is placed at the beginning of
     * the primary successor block. This is for unversioned registers.
     *
     * @param result move destination
     * @param source move source
     */
    public void addMoveToEnd(RegisterSpec result, RegisterSpec source) {
        /*
         * Check that there are no other successors otherwise we may
         * insert a move that affects those (b/69128828).
         */
        if (successors.cardinality() > 1) {
            throw new IllegalStateException("Inserting a move to a block with multiple successors");
        }

        if (result.getReg() == source.getReg()) {
            // Sometimes we end up with no-op moves. Ignore them here.
            return;
        }

        /*
         * The last Insn has to be a normal SSA insn: a phi can't branch
         * or return or cause an exception, etc.
         */
        NormalSsaInsn lastInsn;
        lastInsn = (NormalSsaInsn)insns.get(insns.size()-1);

        if (lastInsn.getResult() != null || lastInsn.getSources().size() > 0) {
            /*
             * The final insn in this block has a source or result
             * register, and the moves we may need to place and
             * schedule may interfere. We need to insert this
             * instruction at the beginning of the primary successor
             * block instead. We know this is safe, because when we
             * edge-split earlier, we ensured that each successor has
             * only us as a predecessor.
             */

            for (int i = successors.nextSetBit(0)
                    ; i >= 0
                    ; i = successors.nextSetBit(i + 1)) {

                SsaBasicBlock succ;

                succ = parent.getBlocks().get(i);
                succ.addMoveToBeginning(result, source);
            }
        } else {
            /*
             * We can safely add a move to the end of the block just
             * before the last instruction, because the final insn does
             * not assign to anything.
             */
            RegisterSpecList sources = RegisterSpecList.make(source);
            NormalSsaInsn toAdd = new NormalSsaInsn(
                    new PlainInsn(Rops.opMove(result.getType()),
                            SourcePosition.NO_INFO, result, sources), this);

            insns.add(insns.size() - 1, toAdd);

            movesFromPhisAtEnd++;
        }
    }

    /**
     * Adds a move instruction after the phi insn block.
     *
     * @param result move destination
     * @param source move source
     */
    public void addMoveToBeginning (RegisterSpec result, RegisterSpec source) {
        if (result.getReg() == source.getReg()) {
            // Sometimes we end up with no-op moves. Ignore them here.
            return;
        }

        RegisterSpecList sources = RegisterSpecList.make(source);
        NormalSsaInsn toAdd = new NormalSsaInsn(
                new PlainInsn(Rops.opMove(result.getType()),
                        SourcePosition.NO_INFO, result, sources), this);

        insns.add(getCountPhiInsns(), toAdd);
        movesFromPhisAtBeginning++;
    }

    /**
     * Sets the register as used in a bitset, taking into account its
     * category/width.
     *
     * @param regsUsed set, indexed by register number
     * @param rs register to mark as used
     */
    private static void setRegsUsed (BitSet regsUsed, RegisterSpec rs) {
        regsUsed.set(rs.getReg());
        if (rs.getCategory() > 1) {
            regsUsed.set(rs.getReg() + 1);
        }
    }

    /**
     * Checks to see if the register is used in a bitset, taking
     * into account its category/width.
     *
     * @param regsUsed set, indexed by register number
     * @param rs register to mark as used
     * @return true if register is fully or partially (for the case of wide
     * registers) used.
     */
    private static boolean checkRegUsed (BitSet regsUsed, RegisterSpec rs) {
        int reg = rs.getReg();
        int category = rs.getCategory();

        return regsUsed.get(reg)
                || (category == 2 ? regsUsed.get(reg + 1) : false);
    }

    /**
     * Ensures that all move operations in this block occur such that
     * reads of any register happen before writes to that register.
     * NOTE: caller is expected to returnSpareRegisters()!
     *
     * TODO: See Briggs, et al "Practical Improvements to the Construction and
     * Destruction of Static Single Assignment Form" section 5. a) This can
     * be done in three passes.
     *
     * @param toSchedule List of instructions. Must consist only of moves.
     */
    private void scheduleUseBeforeAssigned(List<SsaInsn> toSchedule) {
        BitSet regsUsedAsSources = new BitSet(parent.getRegCount());

        // TODO: Get rid of this.
        BitSet regsUsedAsResults = new BitSet(parent.getRegCount());

        int sz = toSchedule.size();

        int insertPlace = 0;

        while (insertPlace < sz) {
            int oldInsertPlace = insertPlace;

            // Record all registers used as sources in this block.
            for (int i = insertPlace; i < sz; i++) {
                setRegsUsed(regsUsedAsSources,
                        toSchedule.get(i).getSources().get(0));

                setRegsUsed(regsUsedAsResults,
                        toSchedule.get(i).getResult());
            }

            /*
             * If there are no circular dependencies, then there exists
             * n instructions where n > 1 whose result is not used as a source.
             */
            for (int i = insertPlace; i <sz; i++) {
                SsaInsn insn = toSchedule.get(i);

                /*
                 * Move these n registers to the front, since they overwrite
                 * nothing.
                 */
                if (!checkRegUsed(regsUsedAsSources, insn.getResult())) {
                    Collections.swap(toSchedule, i, insertPlace++);
                }
            }

            /*
             * If we've made no progress in this iteration, there's a
             * circular dependency. Split it using the temp reg.
             */
            if (oldInsertPlace == insertPlace) {

                SsaInsn insnToSplit = null;

                // Find an insn whose result is used as a source.
                for (int i = insertPlace; i < sz; i++) {
                    SsaInsn insn = toSchedule.get(i);
                    if (checkRegUsed(regsUsedAsSources, insn.getResult())
                            && checkRegUsed(regsUsedAsResults,
                                insn.getSources().get(0))) {

                        insnToSplit = insn;
                        /*
                         * We're going to split this insn; move it to the
                         * front.
                         */
                        Collections.swap(toSchedule, insertPlace, i);
                        break;
                    }
                }

                // At least one insn will be set above.

                RegisterSpec result = insnToSplit.getResult();
                RegisterSpec tempSpec = result.withReg(
                        parent.borrowSpareRegister(result.getCategory()));

                NormalSsaInsn toAdd = new NormalSsaInsn(
                        new PlainInsn(Rops.opMove(result.getType()),
                                SourcePosition.NO_INFO,
                                tempSpec,
                                insnToSplit.getSources()), this);

                toSchedule.add(insertPlace++, toAdd);

                RegisterSpecList newSources = RegisterSpecList.make(tempSpec);

                NormalSsaInsn toReplace = new NormalSsaInsn(
                        new PlainInsn(Rops.opMove(result.getType()),
                                SourcePosition.NO_INFO,
                                result,
                                newSources), this);

                toSchedule.set(insertPlace, toReplace);

                // The size changed.
                sz = toSchedule.size();
            }

            regsUsedAsSources.clear();
            regsUsedAsResults.clear();
        }
    }

    /**
     * Adds {@code regV} to the live-out list for this block. This is called
     * by the liveness analyzer.
     *
     * @param regV register that is live-out for this block.
     */
    public void addLiveOut (int regV) {
        if (liveOut == null) {
            liveOut = SetFactory.makeLivenessSet(parent.getRegCount());
        }

        liveOut.add(regV);
    }

    /**
     * Adds {@code regV} to the live-in list for this block. This is
     * called by the liveness analyzer.
     *
     * @param regV register that is live-in for this block.
     */
    public void addLiveIn (int regV) {
        if (liveIn == null) {
            liveIn = SetFactory.makeLivenessSet(parent.getRegCount());
        }

        liveIn.add(regV);
    }

    /**
     * Returns the set of live-in registers. Valid after register
     * interference graph has been generated, otherwise empty.
     *
     * @return {@code non-null;} live-in register set.
     */
    public IntSet getLiveInRegs() {
        if (liveIn == null) {
            liveIn = SetFactory.makeLivenessSet(parent.getRegCount());
        }
        return liveIn;
    }

    /**
     * Returns the set of live-out registers. Valid after register
     * interference graph has been generated, otherwise empty.
     *
     * @return {@code non-null;} live-out register set
     */
    public IntSet getLiveOutRegs() {
        if (liveOut == null) {
            liveOut = SetFactory.makeLivenessSet(parent.getRegCount());
        }
        return liveOut;
    }

    /**
     * @return true if this is the one-and-only exit block for this method
     */
    public boolean isExitBlock() {
        return index == parent.getExitBlockIndex();
    }

    /**
     * Sorts move instructions added via {@code addMoveToEnd} during
     * phi removal so that results don't overwrite sources that are used.
     * For use after all phis have been removed and all calls to
     * addMoveToEnd() have been made.<p>
     *
     * This is necessary because copy-propogation may have left us in a state
     * where the same basic block has the same register as a phi operand
     * and a result. In this case, the register in the phi operand always
     * refers value before any other phis have executed.
     */
    public void scheduleMovesFromPhis() {
        if (movesFromPhisAtBeginning > 1) {
            List<SsaInsn> toSchedule;

            toSchedule = insns.subList(0, movesFromPhisAtBeginning);

            scheduleUseBeforeAssigned(toSchedule);

            SsaInsn firstNonPhiMoveInsn = insns.get(movesFromPhisAtBeginning);

            /*
             * TODO: It's actually possible that this case never happens,
             * because a move-exception block, having only one predecessor
             * in SSA form, perhaps is never on a dominance frontier.
             */
            if (firstNonPhiMoveInsn.isMoveException()) {
                if (true) {
                    /*
                     * We've yet to observe this case, and if it can
                     * occur the code written to handle it probably
                     * does not work.
                     */
                    throw new RuntimeException(
                            "Unexpected: moves from "
                                    +"phis before move-exception");
                } else {
                    /*
                     * A move-exception insn must be placed first in this block
                     * We need to move it there, and deal with possible
                     * interference.
                     */
                    boolean moveExceptionInterferes = false;

                    int moveExceptionResult
                            = firstNonPhiMoveInsn.getResult().getReg();

                    /*
                     * Does the move-exception result reg interfere with the
                     * phi moves?
                     */
                    for (SsaInsn insn : toSchedule) {
                        if (insn.isResultReg(moveExceptionResult)
                                || insn.isRegASource(moveExceptionResult)) {
                            moveExceptionInterferes = true;
                            break;
                        }
                    }

                    if (!moveExceptionInterferes) {
                        // This is the easy case.
                        insns.remove(movesFromPhisAtBeginning);
                        insns.add(0, firstNonPhiMoveInsn);
                    } else {
                        /*
                         * We need to move the result to a spare reg
                         * and move it back.
                         */
                        RegisterSpec originalResultSpec
                            = firstNonPhiMoveInsn.getResult();
                        int spareRegister = parent.borrowSpareRegister(
                                originalResultSpec.getCategory());

                        // We now move it to a spare register.
                        firstNonPhiMoveInsn.changeResultReg(spareRegister);
                        RegisterSpec tempSpec =
                            firstNonPhiMoveInsn.getResult();

                        insns.add(0, firstNonPhiMoveInsn);

                        // And here we move it back.

                        NormalSsaInsn toAdd = new NormalSsaInsn(
                                new PlainInsn(
                                        Rops.opMove(tempSpec.getType()),
                                        SourcePosition.NO_INFO,
                                        originalResultSpec,
                                        RegisterSpecList.make(tempSpec)),
                                this);


                        /*
                         * Place it immediately after the phi-moves,
                         * overwriting the move-exception that was there.
                         */
                        insns.set(movesFromPhisAtBeginning + 1, toAdd);
                    }
                }
            }
        }

        if (movesFromPhisAtEnd > 1) {
            scheduleUseBeforeAssigned(
                    insns.subList(insns.size() - movesFromPhisAtEnd - 1,
                                insns.size() - 1));
        }

        // Return registers borrowed here and in scheduleUseBeforeAssigned().
        parent.returnSpareRegisters();

    }

    /**
     * Visits all insns in this block.
     *
     * @param visitor {@code non-null;} callback interface
     */
    public void forEachInsn(SsaInsn.Visitor visitor) {
        // This gets called a LOT, and not using an iterator
        // saves a lot of allocations and reduces memory usage
        int len = insns.size();
        for (int i = 0; i < len; i++) {
            insns.get(i).accept(visitor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "{" + index + ":" + Hex.u2(ropLabel) + '}';
    }

    /**
     * Visitor interface for basic blocks.
     */
    public interface Visitor {
        /**
         * Indicates a block has been visited by an iterator method.
         *
         * @param v {@code non-null;} block visited
         * @param parent {@code null-ok;} parent node if applicable
         */
        void visitBlock (SsaBasicBlock v, SsaBasicBlock parent);
    }

    /**
     * Label comparator.
     */
    public static final class LabelComparator
            implements Comparator<SsaBasicBlock> {
        /** {@inheritDoc} */
        @Override
        public int compare(SsaBasicBlock b1, SsaBasicBlock b2) {
            int label1 = b1.ropLabel;
            int label2 = b2.ropLabel;

            if (label1 < label2) {
                return -1;
            } else if (label1 > label2) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
