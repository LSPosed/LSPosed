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

import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * A method in SSA form.
 */
public final class SsaMethod {
    /** basic blocks, indexed by block index */
    private ArrayList<SsaBasicBlock> blocks;

    /** Index of first executed block in method */
    private int entryBlockIndex;

    /**
     * Index of exit block, which exists only in SSA form,
     * or or {@code -1} if there is none
     */
    private int exitBlockIndex;

    /** total number of registers required */
    private int registerCount;

    /** first register number to use for any temporary "spares" */
    private int spareRegisterBase;

    /** current count of spare registers used */
    private int borrowedSpareRegisters;

    /** really one greater than the max label */
    private int maxLabel;

    /** the total width, in register-units, of the method's parameters */
    private final int paramWidth;

    /** true if this method has no {@code this} pointer argument */
    private final boolean isStatic;

    /**
     * indexed by register: the insn where said register is defined or null
     * if undefined. null until (lazily) created.
     */
    private SsaInsn[] definitionList;

    /** indexed by register: the list of all insns that use a register */
    private ArrayList<SsaInsn>[] useList;

    /** A version of useList with each List unmodifiable */
    private List<SsaInsn>[] unmodifiableUseList;

    /**
     * "back-convert mode". Set during back-conversion when registers
     * are about to be mapped into a non-SSA namespace. When true,
     * use and def lists are unavailable.
     *
     * TODO: Remove this mode, and place the functionality elsewhere
     */
    private boolean backMode;

    /**
     * @param ropMethod rop-form method to convert from
     * @param paramWidth the total width, in register-units, of the
     * method's parameters
     * @param isStatic {@code true} if this method has no {@code this}
     * pointer argument
     */
    public static SsaMethod newFromRopMethod(RopMethod ropMethod,
            int paramWidth, boolean isStatic) {
        SsaMethod result = new SsaMethod(ropMethod, paramWidth, isStatic);

        result.convertRopToSsaBlocks(ropMethod);

        return result;
    }

    /**
     * Constructs an instance.
     *
     * @param ropMethod {@code non-null;} the original rop-form method that
     * this instance is based on
     * @param paramWidth the total width, in register-units, of the
     * method's parameters
     * @param isStatic {@code true} if this method has no {@code this}
     * pointer argument
     */
    private SsaMethod(RopMethod ropMethod, int paramWidth, boolean isStatic) {
        this.paramWidth = paramWidth;
        this.isStatic = isStatic;
        this.backMode = false;
        this.maxLabel = ropMethod.getBlocks().getMaxLabel();
        this.registerCount = ropMethod.getBlocks().getRegCount();
        this.spareRegisterBase = registerCount;
    }

    /**
     * Builds a BitSet of block indices from a basic block list and a list
     * of labels taken from Rop form.
     *
     * @param blocks Rop blocks
     * @param labelList list of rop block labels
     * @return BitSet of block indices
     */
    static BitSet bitSetFromLabelList(BasicBlockList blocks,
            IntList labelList) {
        BitSet result = new BitSet(blocks.size());

        for (int i = 0, sz = labelList.size(); i < sz; i++) {
            result.set(blocks.indexOfLabel(labelList.get(i)));
        }

        return result;
    }

    /**
     * Builds an IntList of block indices from a basic block list and a list
     * of labels taken from Rop form.
     *
     * @param ropBlocks Rop blocks
     * @param labelList list of rop block labels
     * @return IntList of block indices
     */
    public static IntList indexListFromLabelList(BasicBlockList ropBlocks,
            IntList labelList) {

        IntList result = new IntList(labelList.size());

        for (int i = 0, sz = labelList.size(); i < sz; i++) {
            result.add(ropBlocks.indexOfLabel(labelList.get(i)));
        }

        return result;
    }

    private void convertRopToSsaBlocks(RopMethod rmeth) {
        BasicBlockList ropBlocks = rmeth.getBlocks();
        int sz = ropBlocks.size();

        blocks = new ArrayList<SsaBasicBlock>(sz + 2);

        for (int i = 0; i < sz; i++) {
            SsaBasicBlock sbb = SsaBasicBlock.newFromRop(rmeth, i, this);
            blocks.add(sbb);
        }

        // Add an no-op entry block.
        int origEntryBlockIndex = rmeth.getBlocks()
                .indexOfLabel(rmeth.getFirstLabel());

        SsaBasicBlock entryBlock
                = blocks.get(origEntryBlockIndex).insertNewPredecessor();

        entryBlockIndex = entryBlock.getIndex();
        exitBlockIndex = -1; // This gets made later.
    }

    /**
     * Creates an exit block and attaches it to the CFG if this method
     * exits. Methods that never exit will not have an exit block. This
     * is called after edge-splitting and phi insertion, since the edges
     * going into the exit block should not be considered in those steps.
     */
    /*package*/ void makeExitBlock() {
        if (exitBlockIndex >= 0) {
            throw new RuntimeException("must be called at most once");
        }

        exitBlockIndex = blocks.size();
        SsaBasicBlock exitBlock
                = new SsaBasicBlock(exitBlockIndex, maxLabel++, this);

        blocks.add(exitBlock);

        for (SsaBasicBlock block : blocks) {
            block.exitBlockFixup(exitBlock);
        }

        if (exitBlock.getPredecessors().cardinality() == 0) {
            // In cases where there is no exit...
            blocks.remove(exitBlockIndex);
            exitBlockIndex = -1;
            maxLabel--;
        }
    }

    /**
     * Gets a new {@code GOTO} insn.
     *
     * @param block block to which this GOTO will be added
     * (not it's destination!)
     * @return an appropriately-constructed instance.
     */
    private static SsaInsn getGoto(SsaBasicBlock block) {
        return new NormalSsaInsn (
                new PlainInsn(Rops.GOTO, SourcePosition.NO_INFO,
                    null, RegisterSpecList.EMPTY), block);
    }

    /**
     * Makes a new basic block for this method, which is empty besides
     * a single {@code GOTO}. Successors and predecessors are not yet
     * set.
     *
     * @return new block
     */
    public SsaBasicBlock makeNewGotoBlock() {
        int newIndex = blocks.size();
        SsaBasicBlock newBlock = new SsaBasicBlock(newIndex, maxLabel++, this);

        newBlock.getInsns().add(getGoto(newBlock));
        blocks.add(newBlock);

        return newBlock;
    }

    /**
     * @return block index of first execution block
     */
    public int getEntryBlockIndex() {
        return entryBlockIndex;
    }

    /**
     * @return first execution block
     */
    public SsaBasicBlock getEntryBlock() {
        return blocks.get(entryBlockIndex);
    }

    /**
     * @return block index of exit block or {@code -1} if there is none
     */
    public int getExitBlockIndex() {
        return exitBlockIndex;
    }

    /**
     * @return {@code null-ok;} block of exit block or {@code null} if
     * there is none
     */
    public SsaBasicBlock getExitBlock() {
        return exitBlockIndex < 0 ? null : blocks.get(exitBlockIndex);
    }

    /**
     * @param bi block index or {@code -1} for none
     * @return rop label or {code -1} if {@code bi} was {@code -1}
     */
    public int blockIndexToRopLabel(int bi) {
        if (bi < 0) {
            return -1;
        }
        return blocks.get(bi).getRopLabel();
    }

    /**
     * @return count of registers used in this method
     */
    public int getRegCount() {
        return registerCount;
    }

    /**
     * @return the total width, in register units, of the method's
     * parameters
     */
    public int getParamWidth() {
        return paramWidth;
    }

    /**
     * Returns {@code true} if this is a static method.
     *
     * @return {@code true} if this is a static method
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Borrows a register to use as a temp. Used in the phi removal process.
     * Call returnSpareRegisters() when done.
     *
     * @param category width (1 or 2) of the register
     * @return register number to use
     */
    public int borrowSpareRegister(int category) {
        int result = spareRegisterBase + borrowedSpareRegisters;

        borrowedSpareRegisters += category;
        registerCount = Math.max(registerCount, result + category);

        return result;
    }

    /**
     * Returns all borrowed registers.
     */
    public void returnSpareRegisters() {
        borrowedSpareRegisters = 0;
    }

    /**
     * @return {@code non-null;} basic block list. Do not modify.
     */
    public ArrayList<SsaBasicBlock> getBlocks() {
        return blocks;
    }

    /**
     * Computes reachability for all blocks in the method.
     *
     * @return a BitSet of reachable blocks.
     */
    public BitSet computeReachability() {
        final int size = blocks.size();
        BitSet reachableUnvisited = new BitSet(size);
        BitSet reachableVisited = new BitSet(size);

        reachableUnvisited.set(getEntryBlock().getIndex());

        int index;
        while ((index = reachableUnvisited.nextSetBit(0)) != -1) {
            reachableVisited.set(index);
            reachableUnvisited.or(blocks.get(index).getSuccessors());
            reachableUnvisited.andNot(reachableVisited);
        }

        return reachableVisited;
    }

    /**
     * Remaps unversioned registers.
     *
     * @param mapper maps old registers to new.
     */
    public void mapRegisters(RegisterMapper mapper) {
        for (SsaBasicBlock block : getBlocks()) {
            for (SsaInsn insn : block.getInsns()) {
                insn.mapRegisters(mapper);
            }
        }

        registerCount = mapper.getNewRegisterCount();
        spareRegisterBase = registerCount;
    }

    /**
     * Returns the insn that defines the given register
     * @param reg register in question
     * @return insn (actual instance from code) that defined this reg or null
     * if reg is not defined.
     */
    public SsaInsn getDefinitionForRegister(int reg) {
        if (backMode) {
            throw new RuntimeException("No def list in back mode");
        }

        if (definitionList != null) {
            return definitionList[reg];
        }

        definitionList = new SsaInsn[getRegCount()];

        forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn (NormalSsaInsn insn) {
                definitionList[insn.getResult().getReg()] = insn;
            }
            @Override
            public void visitPhiInsn (PhiInsn phi) {
                definitionList[phi.getResult().getReg()] = phi;
            }
            @Override
            public void visitNonMoveInsn (NormalSsaInsn insn) {
                RegisterSpec result = insn.getResult();
                if (result != null) {
                    definitionList[insn.getResult().getReg()] = insn;
                }
            }
        });

        return definitionList[reg];
    }

    /**
     * Builds useList and unmodifiableUseList.
     */
    private void buildUseList() {
        if (backMode) {
            throw new RuntimeException("No use list in back mode");
        }

        useList = new ArrayList[registerCount];

        for (int i = 0; i < registerCount; i++) {
            useList[i] = new ArrayList();
        }

        forEachInsn(new SsaInsn.Visitor() {
            /** {@inheritDoc} */
            @Override
            public void visitMoveInsn (NormalSsaInsn insn) {
                addToUses(insn);
            }
            /** {@inheritDoc} */
            @Override
            public void visitPhiInsn (PhiInsn phi) {
                addToUses(phi);
            }
            /** {@inheritDoc} */
            @Override
            public void visitNonMoveInsn (NormalSsaInsn insn) {
                addToUses(insn);
            }
            /**
             * Adds specified insn to the uses list for all of its sources.
             * @param insn {@code non-null;} insn to process
             */
            private void addToUses(SsaInsn insn) {
                RegisterSpecList rl = insn.getSources();
                int sz = rl.size();

                for (int i = 0; i < sz; i++) {
                    useList[rl.get(i).getReg()].add(insn);
                }
            }
        });

        unmodifiableUseList = new List[registerCount];

        for (int i = 0; i < registerCount; i++) {
            unmodifiableUseList[i] = Collections.unmodifiableList(useList[i]);
        }
    }

    /**
     * Updates the use list for a single change in source register.
     *
     * @param insn {@code non-null;} insn being changed
     * @param oldSource {@code null-ok;} The source that was used, if
     * applicable
     * @param newSource {@code non-null;} the new source being used
     */
    /*package*/ void onSourceChanged(SsaInsn insn,
            RegisterSpec oldSource, RegisterSpec newSource) {
        if (useList == null) return;

        if (oldSource != null) {
            int reg = oldSource.getReg();
            useList[reg].remove(insn);
        }

        int reg = newSource.getReg();
        if (useList.length <= reg) {
            useList = null;
            return;
        }
        useList[reg].add(insn);
    }

    /**
     * Updates the use list for a source list change.
     *
     * @param insn {@code insn non-null;} insn being changed.
     * {@code insn.getSources()} must return the new source list.
     * @param oldSources {@code null-ok;} list of sources that were
     * previously used
     */
    /*package*/ void onSourcesChanged(SsaInsn insn,
            RegisterSpecList oldSources) {
        if (useList == null) return;

        if (oldSources != null) {
            removeFromUseList(insn, oldSources);
        }

        RegisterSpecList sources = insn.getSources();
        int szNew = sources.size();

        for (int i = 0; i < szNew; i++) {
            int reg = sources.get(i).getReg();
            useList[reg].add(insn);
        }
    }

    /**
     * Removes a given {@code insn} from the use lists for the given
     * {@code oldSources} (rather than the sources currently
     * returned by insn.getSources()).
     *
     * @param insn {@code non-null;} insn in question
     * @param oldSources {@code null-ok;} registers whose use lists
     * {@code insn} should be removed form
     */
    private void removeFromUseList(SsaInsn insn, RegisterSpecList oldSources) {
        if (oldSources == null) {
            return;
        }

        int szNew = oldSources.size();
        for (int i = 0; i < szNew; i++) {
            if (!useList[oldSources.get(i).getReg()].remove(insn)) {
                throw new RuntimeException("use not found");
            }
        }
    }

    /**
     * Adds an insn to both the use and def lists. For use when adding
     * a new insn to the method.
     *
     * @param insn {@code non-null;} insn to add
     */
    /*package*/ void onInsnAdded(SsaInsn insn) {
        onSourcesChanged(insn, null);
        updateOneDefinition(insn, null);
    }

    /**
     * Removes an instruction from use and def lists. For use during
     * instruction removal.
     *
     * @param insn {@code non-null;} insn to remove
     */
    /*package*/ void onInsnRemoved(SsaInsn insn) {
        if (useList != null) {
            removeFromUseList(insn, insn.getSources());
        }

        RegisterSpec resultReg = insn.getResult();
        if (definitionList != null && resultReg != null) {
            definitionList[resultReg.getReg()] = null;
        }
    }

    /**
     * Indicates that the instruction list has changed or the SSA register
     * count has increased, so that internal datastructures that rely on
     * it should be rebuild. In general, the various other on* methods
     * should be called in preference when changes occur if they are
     * applicable.
     */
    public void onInsnsChanged() {
        // Definition list will need to be recomputed
        definitionList = null;

        // Use list will need to be recomputed
        useList = null;
        unmodifiableUseList = null;
    }

    /**
     * Updates a single definition.
     *
     * @param insn {@code non-null;} insn who's result should be recorded as
     * a definition
     * @param oldResult {@code null-ok;} a previous result that should
     * be no longer considered a definition by this insn
     */
    /*package*/ void updateOneDefinition(SsaInsn insn,
            RegisterSpec oldResult) {
        if (definitionList == null) return;

        if (oldResult != null) {
            int reg = oldResult.getReg();
            definitionList[reg] = null;
        }

        RegisterSpec resultReg = insn.getResult();

        if (resultReg != null) {
            int reg = resultReg.getReg();

            if (definitionList[reg] != null) {
                throw new RuntimeException("Duplicate add of insn");
            } else {
                definitionList[resultReg.getReg()] = insn;
            }
        }
    }

    /**
     * Returns the list of all source uses (not results) for a register.
     *
     * @param reg register in question
     * @return unmodifiable instruction list
     */
    public List<SsaInsn> getUseListForRegister(int reg) {

        if (unmodifiableUseList == null) {
            buildUseList();
        }

        return unmodifiableUseList[reg];
    }

    /**
     * Returns a modifiable copy of the register use list.
     *
     * @return modifiable copy of the use-list, indexed by register
     */
    public ArrayList<SsaInsn>[] getUseListCopy() {
        if (useList == null) {
            buildUseList();
        }

        ArrayList<SsaInsn>[] useListCopy
                = (ArrayList<SsaInsn>[])(new ArrayList[registerCount]);

        for (int i = 0; i < registerCount; i++) {
            useListCopy[i] = (ArrayList<SsaInsn>)(new ArrayList(useList[i]));
        }

        return useListCopy;
    }

    /**
     * Checks to see if the given SSA reg is ever associated with a local
     * local variable. Each SSA reg may be associated with at most one
     * local var.
     *
     * @param spec {@code non-null;} ssa reg
     * @return true if reg is ever associated with a local
     */
    public boolean isRegALocal(RegisterSpec spec) {
        SsaInsn defn = getDefinitionForRegister(spec.getReg());

        if (defn == null) {
            // version 0 registers are never used as locals
            return false;
        }

        // Does the definition have a local associated with it?
        if (defn.getLocalAssignment() != null) return true;

        // If not, is there a mark-local insn?
        for (SsaInsn use : getUseListForRegister(spec.getReg())) {
            Insn insn = use.getOriginalRopInsn();

            if (insn != null
                    && insn.getOpcode().getOpcode() == RegOps.MARK_LOCAL) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the new register count after renaming.
     *
     * @param newRegCount new register count
     */
    /*package*/ void setNewRegCount(int newRegCount) {
        registerCount = newRegCount;
        spareRegisterBase = registerCount;
        onInsnsChanged();
    }

    /**
     * Makes a new SSA register. For use after renaming has completed.
     *
     * @return {@code >=0;} new SSA register.
     */
    public int makeNewSsaReg() {
        int reg = registerCount++;
        spareRegisterBase = registerCount;
        onInsnsChanged();
        return reg;
    }

    /**
     * Visits all insns in this method.
     *
     * @param visitor {@code non-null;} callback interface
     */
    public void forEachInsn(SsaInsn.Visitor visitor) {
        for (SsaBasicBlock block : blocks) {
            block.forEachInsn(visitor);
        }
    }

    /**
     * Visits each phi insn in this method
     * @param v {@code non-null;} callback.
     *
     */
    public void forEachPhiInsn(PhiInsn.Visitor v) {
        for (SsaBasicBlock block : blocks) {
            block.forEachPhiInsn(v);
        }
    }


    /**
     * Walks the basic block tree in depth-first order, calling the visitor
     * method once for every block. This depth-first walk may be run forward
     * from the method entry point or backwards from the method exit points.
     *
     * @param reverse true if this should walk backwards from the exit points
     * @param v {@code non-null;} callback interface. {@code parent} is set
     * unless this is the root node
     */
    public void forEachBlockDepthFirst(boolean reverse,
            SsaBasicBlock.Visitor v) {
        BitSet visited = new BitSet(blocks.size());

        // We push the parent first, then the child on the stack.
        Stack<SsaBasicBlock> stack = new Stack<SsaBasicBlock>();

        SsaBasicBlock rootBlock = reverse ? getExitBlock() : getEntryBlock();

        if (rootBlock == null) {
            // in the case there's no exit block
            return;
        }

        stack.add(null);    // Start with null parent.
        stack.add(rootBlock);

        while (stack.size() > 0) {
            SsaBasicBlock cur = stack.pop();
            SsaBasicBlock parent = stack.pop();

            if (!visited.get(cur.getIndex())) {
                BitSet children
                    = reverse ? cur.getPredecessors() : cur.getSuccessors();
                for (int i = children.nextSetBit(0); i >= 0
                        ; i = children.nextSetBit(i + 1)) {
                    stack.add(cur);
                    stack.add(blocks.get(i));
                }
                visited.set(cur.getIndex());
                v.visitBlock(cur, parent);
            }
        }
    }

    /**
     * Visits blocks in dom-tree order, starting at the current node.
     * The {@code parent} parameter of the Visitor.visitBlock callback
     * is currently always set to null.
     *
     * @param v {@code non-null;} callback interface
     */
    public void forEachBlockDepthFirstDom(SsaBasicBlock.Visitor v) {
        BitSet visited = new BitSet(getBlocks().size());
        Stack<SsaBasicBlock> stack = new Stack<SsaBasicBlock>();

        stack.add(getEntryBlock());

        while (stack.size() > 0) {
            SsaBasicBlock cur = stack.pop();
            ArrayList<SsaBasicBlock> curDomChildren = cur.getDomChildren();

            if (!visited.get(cur.getIndex())) {
                // We walk the tree this way for historical reasons...
                for (int i = curDomChildren.size() - 1; i >= 0; i--) {
                    SsaBasicBlock child = curDomChildren.get(i);
                    stack.add(child);
                }
                visited.set(cur.getIndex());
                v.visitBlock(cur, null);
            }
        }
    }

    /**
     * Deletes all insns in the set from this method.
     *
     * @param deletedInsns {@code non-null;} insns to delete
     */
    public void deleteInsns(Set<SsaInsn> deletedInsns) {
        for (SsaInsn deletedInsn : deletedInsns) {
            SsaBasicBlock block = deletedInsn.getBlock();
            ArrayList<SsaInsn> insns = block.getInsns();

            for (int i = insns.size() - 1; i >= 0; i--) {
                SsaInsn insn = insns.get(i);

                if (deletedInsn == insn) {
                    onInsnRemoved(insn);
                    insns.remove(i);
                    break;
                }
            }

            // Check to see if we need to add a GOTO

            int insnsSz = insns.size();
            SsaInsn lastInsn = (insnsSz == 0) ? null : insns.get(insnsSz - 1);

            if (block != getExitBlock() && (insnsSz == 0
                    || lastInsn.getOriginalRopInsn() == null
                    || lastInsn.getOriginalRopInsn().getOpcode()
                        .getBranchingness() == Rop.BRANCH_NONE)) {
                // We managed to eat a throwable insn

                Insn gotoInsn = new PlainInsn(Rops.GOTO,
                        SourcePosition.NO_INFO, null, RegisterSpecList.EMPTY);
                insns.add(SsaInsn.makeFromRop(gotoInsn, block));

                // Remove secondary successors from this block
                BitSet succs = block.getSuccessors();
                for (int i = succs.nextSetBit(0); i >= 0;
                         i = succs.nextSetBit(i + 1)) {
                    if (i != block.getPrimarySuccessorIndex()) {
                        block.removeSuccessor(i);
                    }
                }
            }
        }
    }

    /**
     * Sets "back-convert mode". Set during back-conversion when registers
     * are about to be mapped into a non-SSA namespace. When true,
     * use and def lists are unavailable.
     */
    public void setBackMode() {
        backMode = true;
        useList = null;
        definitionList = null;
    }
}
