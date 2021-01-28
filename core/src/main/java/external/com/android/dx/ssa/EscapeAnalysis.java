/*
 * Copyright (C) 2010 The Android Open Source Project
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

import external.com.android.dx.rop.code.Exceptions;
import external.com.android.dx.rop.code.FillArrayDataInsn;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.PlainCstInsn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.ThrowingCstInsn;
import external.com.android.dx.rop.code.ThrowingInsn;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstLiteralBits;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.cst.TypedConstant;
import external.com.android.dx.rop.cst.Zeroes;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

/**
 * Simple intraprocedural escape analysis. Finds new arrays that don't escape
 * the method they are created in and replaces the array values with registers.
 */
public class EscapeAnalysis {
    /**
     * Struct used to generate and maintain escape analysis results.
     */
    static class EscapeSet {
        /** set containing all registers related to an object */
        BitSet regSet;
        /** escape state of the object */
        EscapeState escape;
        /** list of objects that are put into this object */
        ArrayList<EscapeSet> childSets;
        /** list of objects that this object is put into */
        ArrayList<EscapeSet> parentSets;
        /** flag to indicate this object is a scalar replaceable array */
        boolean replaceableArray;

        /**
         * Constructs an instance of an EscapeSet
         *
         * @param reg the SSA register that defines the object
         * @param size the number of registers in the method
         * @param escState the lattice value to initially set this to
         */
        EscapeSet(int reg, int size, EscapeState escState) {
            regSet = new BitSet(size);
            regSet.set(reg);
            escape = escState;
            childSets = new ArrayList<EscapeSet>();
            parentSets = new ArrayList<EscapeSet>();
            replaceableArray = false;
        }
    }

    /**
     * Lattice values used to indicate escape state for an object. Analysis can
     * only raise escape state values, not lower them.
     *
     * TOP - Used for objects that haven't been analyzed yet
     * NONE - Object does not escape, and is eligible for scalar replacement.
     * METHOD - Object remains local to method, but can't be scalar replaced.
     * INTER - Object is passed between methods. (treated as globally escaping
     *         since this is an intraprocedural analysis)
     * GLOBAL - Object escapes globally.
     */
    public enum EscapeState {
        TOP, NONE, METHOD, INTER, GLOBAL
    }

    /** method we're processing */
    private final SsaMethod ssaMeth;
    /** ssaMeth.getRegCount() */
    private final int regCount;
    /** Lattice values for each object register group */
    private final ArrayList<EscapeSet> latticeValues;

    /**
     * Constructs an instance.
     *
     * @param ssaMeth method to process
     */
    private EscapeAnalysis(SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.regCount = ssaMeth.getRegCount();
        this.latticeValues = new ArrayList<EscapeSet>();
    }

    /**
     * Finds the index in the lattice for a particular register.
     * Returns the size of the lattice if the register wasn't found.
     *
     * @param reg {@code non-null;} register being looked up
     * @return index of the register or size of the lattice if it wasn't found.
     */
    private int findSetIndex(RegisterSpec reg) {
        int i;
        for (i = 0; i < latticeValues.size(); i++) {
            EscapeSet e = latticeValues.get(i);
            if (e.regSet.get(reg.getReg())) {
                return i;
            }
        }
        return i;
    }

    /**
     * Finds the corresponding instruction for a given move result
     *
     * @param moveInsn {@code non-null;} a move result instruction
     * @return {@code non-null;} the instruction that produces the result for
     * the move
     */
    private SsaInsn getInsnForMove(SsaInsn moveInsn) {
        int pred = moveInsn.getBlock().getPredecessors().nextSetBit(0);
        ArrayList<SsaInsn> predInsns = ssaMeth.getBlocks().get(pred).getInsns();
        return predInsns.get(predInsns.size()-1);
    }

    /**
     * Finds the corresponding move result for a given instruction
     *
     * @param insn {@code non-null;} an instruction that must always be
     * followed by a move result
     * @return {@code non-null;} the move result for the given instruction
     */
    private SsaInsn getMoveForInsn(SsaInsn insn) {
        int succ = insn.getBlock().getSuccessors().nextSetBit(0);
        ArrayList<SsaInsn> succInsns = ssaMeth.getBlocks().get(succ).getInsns();
        return succInsns.get(0);
    }

    /**
     * Creates a link in the lattice between two EscapeSets due to a put
     * instruction. The object being put is the child and the object being put
     * into is the parent. A child set must always have an escape state at
     * least as high as its parent.
     *
     * @param parentSet {@code non-null;} the EscapeSet for the object being put
     * into
     * @param childSet {@code non-null;} the EscapeSet for the object being put
     */
    private void addEdge(EscapeSet parentSet, EscapeSet childSet) {
        if (!childSet.parentSets.contains(parentSet)) {
            childSet.parentSets.add(parentSet);
        }
        if (!parentSet.childSets.contains(childSet)) {
            parentSet.childSets.add(childSet);
        }
    }

    /**
     * Merges all links in the lattice among two EscapeSets. On return, the
     * newNode will have its old links as well as all links from the oldNode.
     * The oldNode has all its links removed.
     *
     * @param newNode {@code non-null;} the EscapeSet to merge all links into
     * @param oldNode {@code non-null;} the EscapeSet to remove all links from
     */
    private void replaceNode(EscapeSet newNode, EscapeSet oldNode) {
        for (EscapeSet e : oldNode.parentSets) {
            e.childSets.remove(oldNode);
            e.childSets.add(newNode);
            newNode.parentSets.add(e);
        }
        for (EscapeSet e : oldNode.childSets) {
            e.parentSets.remove(oldNode);
            e.parentSets.add(newNode);
            newNode.childSets.add(e);
        }
    }

    /**
     * Performs escape analysis on a method. Finds scalar replaceable arrays and
     * replaces them with equivalent registers.
     *
     * @param ssaMethod {@code non-null;} method to process
     */
    public static void process(SsaMethod ssaMethod) {
        new EscapeAnalysis(ssaMethod).run();
    }

    /**
     * Process a single instruction, looking for new objects resulting from
     * move result or move param.
     *
     * @param insn {@code non-null;} instruction to process
     */
    private void processInsn(SsaInsn insn) {
        int op = insn.getOpcode().getOpcode();
        RegisterSpec result = insn.getResult();
        EscapeSet escSet;

        // Identify new objects
        if (op == RegOps.MOVE_RESULT_PSEUDO &&
                result.getTypeBearer().getBasicType() == Type.BT_OBJECT) {
            // Handle objects generated through move_result_pseudo
            escSet = processMoveResultPseudoInsn(insn);
            processRegister(result, escSet);
        } else if (op == RegOps.MOVE_PARAM &&
                      result.getTypeBearer().getBasicType() == Type.BT_OBJECT) {
            // Track method arguments that are objects
            escSet = new EscapeSet(result.getReg(), regCount, EscapeState.NONE);
            latticeValues.add(escSet);
            processRegister(result, escSet);
        } else if (op == RegOps.MOVE_RESULT &&
                result.getTypeBearer().getBasicType() == Type.BT_OBJECT) {
            // Track method return values that are objects
            escSet = new EscapeSet(result.getReg(), regCount, EscapeState.NONE);
            latticeValues.add(escSet);
            processRegister(result, escSet);
        }
    }

    /**
     * Determine the origin of a move result pseudo instruction that generates
     * an object. Creates a new EscapeSet for the new object accordingly.
     *
     * @param insn {@code non-null;} move result pseudo instruction to process
     * @return {@code non-null;} an EscapeSet for the object referred to by the
     * move result pseudo instruction
     */
    private EscapeSet processMoveResultPseudoInsn(SsaInsn insn) {
        RegisterSpec result = insn.getResult();
        SsaInsn prevSsaInsn = getInsnForMove(insn);
        int prevOpcode = prevSsaInsn.getOpcode().getOpcode();
        EscapeSet escSet;
        RegisterSpec prevSource;

        switch(prevOpcode) {
           // New instance / Constant
            case RegOps.NEW_INSTANCE:
            case RegOps.CONST:
                escSet = new EscapeSet(result.getReg(), regCount,
                                           EscapeState.NONE);
                break;
            // New array
            case RegOps.NEW_ARRAY:
            case RegOps.FILLED_NEW_ARRAY:
                prevSource = prevSsaInsn.getSources().get(0);
                if (prevSource.getTypeBearer().isConstant()) {
                    // New fixed array
                    escSet = new EscapeSet(result.getReg(), regCount,
                                               EscapeState.NONE);
                    escSet.replaceableArray = true;
                } else {
                    // New variable array
                    escSet = new EscapeSet(result.getReg(), regCount,
                                               EscapeState.GLOBAL);
                }
                break;
            // Loading a static object
            case RegOps.GET_STATIC:
                escSet = new EscapeSet(result.getReg(), regCount,
                                           EscapeState.GLOBAL);
                break;
            // Type cast / load an object from a field or array
            case RegOps.CHECK_CAST:
            case RegOps.GET_FIELD:
            case RegOps.AGET:
                prevSource = prevSsaInsn.getSources().get(0);
                int setIndex = findSetIndex(prevSource);

                // Set should already exist, try to find it
                if (setIndex != latticeValues.size()) {
                    escSet = latticeValues.get(setIndex);
                    escSet.regSet.set(result.getReg());
                    return escSet;
                }

                // Set not found, must be either null or unknown
                if (prevSource.getType() == Type.KNOWN_NULL) {
                    escSet = new EscapeSet(result.getReg(), regCount,
                                               EscapeState.NONE);
               } else {
                    escSet = new EscapeSet(result.getReg(), regCount,
                                               EscapeState.GLOBAL);
                }
                break;
            default:
                return null;
        }

        // Add the newly created escSet to the lattice and return it
        latticeValues.add(escSet);
        return escSet;
    }

    /**
     * Iterate through all the uses of a new object.
     *
     * @param result {@code non-null;} register where new object is stored
     * @param escSet {@code non-null;} EscapeSet for the new object
     */
    private void processRegister(RegisterSpec result, EscapeSet escSet) {
        ArrayList<RegisterSpec> regWorklist = new ArrayList<RegisterSpec>();
        regWorklist.add(result);

        // Go through the worklist
        while (!regWorklist.isEmpty()) {
            int listSize = regWorklist.size() - 1;
            RegisterSpec def = regWorklist.remove(listSize);
            List<SsaInsn> useList = ssaMeth.getUseListForRegister(def.getReg());

            // Handle all the uses of this register
            for (SsaInsn use : useList) {
                Rop useOpcode = use.getOpcode();

                if (useOpcode == null) {
                    // Handle phis
                    processPhiUse(use, escSet, regWorklist);
                } else {
                    // Handle other opcodes
                    processUse(def, use, escSet, regWorklist);
                }
            }
        }
    }

    /**
     * Handles phi uses of new objects. Will merge together the sources of a phi
     * into a single EscapeSet. Adds the result of the phi to the worklist so
     * its uses can be followed.
     *
     * @param use {@code non-null;} phi use being processed
     * @param escSet {@code non-null;} EscapeSet for the object
     * @param regWorklist {@code non-null;} worklist of instructions left to
     * process for this object
     */
    private void processPhiUse(SsaInsn use, EscapeSet escSet,
                                   ArrayList<RegisterSpec> regWorklist) {
        int setIndex = findSetIndex(use.getResult());
        if (setIndex != latticeValues.size()) {
            // Check if result is in a set already
            EscapeSet mergeSet = latticeValues.get(setIndex);
            if (mergeSet != escSet) {
                // If it is, merge the sets and states, then delete the copy
                escSet.replaceableArray = false;
                escSet.regSet.or(mergeSet.regSet);
                if (escSet.escape.compareTo(mergeSet.escape) < 0) {
                    escSet.escape = mergeSet.escape;
                }
                replaceNode(escSet, mergeSet);
                latticeValues.remove(setIndex);
            }
        } else {
            // If no set is found, add it to this escSet and the worklist
            escSet.regSet.set(use.getResult().getReg());
            regWorklist.add(use.getResult());
        }
    }

    /**
     * Handles non-phi uses of new objects. Checks to see how instruction is
     * used and updates the escape state accordingly.
     *
     * @param def {@code non-null;} register holding definition of new object
     * @param use {@code non-null;} use of object being processed
     * @param escSet {@code non-null;} EscapeSet for the object
     * @param regWorklist {@code non-null;} worklist of instructions left to
     * process for this object
     */
    private void processUse(RegisterSpec def, SsaInsn use, EscapeSet escSet,
                                ArrayList<RegisterSpec> regWorklist) {
        int useOpcode = use.getOpcode().getOpcode();
        switch (useOpcode) {
            case RegOps.MOVE:
                // Follow uses of the move by adding it to the worklist
                escSet.regSet.set(use.getResult().getReg());
                regWorklist.add(use.getResult());
                break;
            case RegOps.IF_EQ:
            case RegOps.IF_NE:
            case RegOps.CHECK_CAST:
                // Compared objects can't be replaced, so promote if necessary
                if (escSet.escape.compareTo(EscapeState.METHOD) < 0) {
                    escSet.escape = EscapeState.METHOD;
                }
                break;
            case RegOps.APUT:
                // For array puts, check for a constant array index
                RegisterSpec putIndex = use.getSources().get(2);
                if (!putIndex.getTypeBearer().isConstant()) {
                    // If not constant, array can't be replaced
                    escSet.replaceableArray = false;
                }
                // Intentional fallthrough
            case RegOps.PUT_FIELD:
                // Skip non-object puts
                RegisterSpec putValue = use.getSources().get(0);
                if (putValue.getTypeBearer().getBasicType() != Type.BT_OBJECT) {
                    break;
                }
                escSet.replaceableArray = false;

                // Raise 1st object's escape state to 2nd if 2nd is higher
                RegisterSpecList sources = use.getSources();
                if (sources.get(0).getReg() == def.getReg()) {
                    int setIndex = findSetIndex(sources.get(1));
                    if (setIndex != latticeValues.size()) {
                        EscapeSet parentSet = latticeValues.get(setIndex);
                        addEdge(parentSet, escSet);
                        if (escSet.escape.compareTo(parentSet.escape) < 0) {
                            escSet.escape = parentSet.escape;
                        }
                    }
                } else {
                    int setIndex = findSetIndex(sources.get(0));
                    if (setIndex != latticeValues.size()) {
                        EscapeSet childSet = latticeValues.get(setIndex);
                        addEdge(escSet, childSet);
                        if (childSet.escape.compareTo(escSet.escape) < 0) {
                            childSet.escape = escSet.escape;
                        }
                    }
                }
                break;
            case RegOps.AGET:
                // For array gets, check for a constant array index
                RegisterSpec getIndex = use.getSources().get(1);
                if (!getIndex.getTypeBearer().isConstant()) {
                    // If not constant, array can't be replaced
                    escSet.replaceableArray = false;
                }
                break;
            case RegOps.PUT_STATIC:
                // Static puts cause an object to escape globally
                escSet.escape = EscapeState.GLOBAL;
                break;
            case RegOps.INVOKE_STATIC:
            case RegOps.INVOKE_VIRTUAL:
            case RegOps.INVOKE_SUPER:
            case RegOps.INVOKE_DIRECT:
            case RegOps.INVOKE_INTERFACE:
            case RegOps.RETURN:
            case RegOps.THROW:
                // These operations cause an object to escape interprocedurally
                escSet.escape = EscapeState.INTER;
                break;
            default:
                break;
        }
    }

    /**
     * Performs scalar replacement on all eligible arrays.
     */
    private void scalarReplacement() {
        // Iterate through lattice, looking for non-escaping replaceable arrays
        for (EscapeSet escSet : latticeValues) {
            if (!escSet.replaceableArray || escSet.escape != EscapeState.NONE) {
                continue;
            }

            // Get the instructions for the definition and move of the array
            int e = escSet.regSet.nextSetBit(0);
            SsaInsn def = ssaMeth.getDefinitionForRegister(e);
            SsaInsn prev = getInsnForMove(def);

            // Create a map for the new registers that will be created
            TypeBearer lengthReg = prev.getSources().get(0).getTypeBearer();
            int length = ((CstLiteralBits) lengthReg).getIntBits();
            ArrayList<RegisterSpec> newRegs =
                new ArrayList<RegisterSpec>(length);
            HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();

            // Replace the definition of the array with registers
            replaceDef(def, prev, length, newRegs);

            // Mark definition instructions for deletion
            deletedInsns.add(prev);
            deletedInsns.add(def);

            // Go through all uses of the array
            List<SsaInsn> useList = ssaMeth.getUseListForRegister(e);
            for (SsaInsn use : useList) {
                // Replace the use with scalars and then mark it for deletion
                replaceUse(use, prev, newRegs, deletedInsns);
                deletedInsns.add(use);
            }

            // Delete all marked instructions
            ssaMeth.deleteInsns(deletedInsns);
            ssaMeth.onInsnsChanged();

            // Convert the method back to SSA form
            SsaConverter.updateSsaMethod(ssaMeth, regCount);

            // Propagate and remove extra moves added by scalar replacement
            movePropagate();
        }
    }

    /**
     * Replaces the instructions that define an array with equivalent registers.
     * For each entry in the array, a register is created, initialized to zero.
     * A mapping between this register and the corresponding array index is
     * added.
     *
     * @param def {@code non-null;} move result instruction for array
     * @param prev {@code non-null;} instruction for instantiating new array
     * @param length size of the new array
     * @param newRegs {@code non-null;} mapping of array indices to new
     * registers to be populated
     */
    private void replaceDef(SsaInsn def, SsaInsn prev, int length,
                                ArrayList<RegisterSpec> newRegs) {
        Type resultType = def.getResult().getType();

        // Create new zeroed out registers for each element in the array
        for (int i = 0; i < length; i++) {
            Constant newZero = Zeroes.zeroFor(resultType.getComponentType());
            TypedConstant typedZero = (TypedConstant) newZero;
            RegisterSpec newReg =
                RegisterSpec.make(ssaMeth.makeNewSsaReg(), typedZero);
            newRegs.add(newReg);
            insertPlainInsnBefore(def, RegisterSpecList.EMPTY, newReg,
                                      RegOps.CONST, newZero);
        }
    }

    /**
     * Replaces the use for a scalar replaceable array. Gets and puts become
     * move instructions, and array lengths and fills are handled. Can also
     * identify ArrayIndexOutOfBounds exceptions and throw them if detected.
     *
     * @param use {@code non-null;} move result instruction for array
     * @param prev {@code non-null;} instruction for instantiating new array
     * @param newRegs {@code non-null;} mapping of array indices to new
     * registers
     * @param deletedInsns {@code non-null;} set of instructions marked for
     * deletion
     */
    private void replaceUse(SsaInsn use, SsaInsn prev,
                                ArrayList<RegisterSpec> newRegs,
                                HashSet<SsaInsn> deletedInsns) {
        int index;
        int length = newRegs.size();
        SsaInsn next;
        RegisterSpecList sources;
        RegisterSpec source, result;
        CstLiteralBits indexReg;

        switch (use.getOpcode().getOpcode()) {
            case RegOps.AGET:
                // Replace array gets with moves
                next = getMoveForInsn(use);
                sources = use.getSources();
                indexReg = ((CstLiteralBits) sources.get(1).getTypeBearer());
                index = indexReg.getIntBits();
                if (index < length) {
                    source = newRegs.get(index);
                    result = source.withReg(next.getResult().getReg());
                    insertPlainInsnBefore(next, RegisterSpecList.make(source),
                                              result, RegOps.MOVE, null);
                } else {
                    // Throw an exception if the index is out of bounds
                    insertExceptionThrow(next, sources.get(1), deletedInsns);
                    deletedInsns.add(next.getBlock().getInsns().get(2));
                }
                deletedInsns.add(next);
                break;
            case RegOps.APUT:
                // Replace array puts with moves
                sources = use.getSources();
                indexReg = ((CstLiteralBits) sources.get(2).getTypeBearer());
                index = indexReg.getIntBits();
                if (index < length) {
                    source = sources.get(0);
                    result = source.withReg(newRegs.get(index).getReg());
                    insertPlainInsnBefore(use, RegisterSpecList.make(source),
                                              result, RegOps.MOVE, null);
                    // Update the newReg entry to mark value as unknown now
                    newRegs.set(index, result.withSimpleType());
                } else {
                    // Throw an exception if the index is out of bounds
                    insertExceptionThrow(use, sources.get(2), deletedInsns);
                }
                break;
            case RegOps.ARRAY_LENGTH:
                // Replace array lengths with const instructions
                TypeBearer lengthReg = prev.getSources().get(0).getTypeBearer();
                //CstInteger lengthReg = CstInteger.make(length);
                next = getMoveForInsn(use);
                insertPlainInsnBefore(next, RegisterSpecList.EMPTY,
                                          next.getResult(), RegOps.CONST,
                                          (Constant) lengthReg);
                deletedInsns.add(next);
                break;
            case RegOps.MARK_LOCAL:
                // Remove mark local instructions
                break;
            case RegOps.FILL_ARRAY_DATA:
                // Create const instructions for each fill value
                Insn ropUse = use.getOriginalRopInsn();
                FillArrayDataInsn fill = (FillArrayDataInsn) ropUse;
                ArrayList<Constant> constList = fill.getInitValues();
                for (int i = 0; i < length; i++) {
                    RegisterSpec newFill =
                        RegisterSpec.make(newRegs.get(i).getReg(),
                                              (TypeBearer) constList.get(i));
                    insertPlainInsnBefore(use, RegisterSpecList.EMPTY, newFill,
                                              RegOps.CONST, constList.get(i));
                    // Update the newRegs to hold the new const value
                    newRegs.set(i, newFill);
                }
                break;
            default:
        }
    }

    /**
     * Identifies extra moves added by scalar replacement and propagates the
     * source of the move to any users of the result.
     */
    private void movePropagate() {
        for (int i = 0; i < ssaMeth.getRegCount(); i++) {
            SsaInsn insn = ssaMeth.getDefinitionForRegister(i);

            // Look for move instructions only
            if (insn == null || insn.getOpcode() == null ||
                insn.getOpcode().getOpcode() != RegOps.MOVE) {
                continue;
            }

            final ArrayList<SsaInsn>[] useList = ssaMeth.getUseListCopy();
            final RegisterSpec source = insn.getSources().get(0);
            final RegisterSpec result = insn.getResult();

            // Ignore moves that weren't added due to scalar replacement
            if (source.getReg() < regCount && result.getReg() < regCount) {
                continue;
            }

            // Create a mapping from source to result
            RegisterMapper mapper = new RegisterMapper() {
                @Override
                public int getNewRegisterCount() {
                    return ssaMeth.getRegCount();
                }

                @Override
                public RegisterSpec map(RegisterSpec registerSpec) {
                    if (registerSpec.getReg() == result.getReg()) {
                        return source;
                    }

                    return registerSpec;
                }
            };

            // Modify all uses of the move to use the source of the move instead
            for (SsaInsn use : useList[result.getReg()]) {
                use.mapSourceRegisters(mapper);
            }
        }
    }

    /**
     * Runs escape analysis and scalar replacement of arrays.
     */
    private void run() {
        ssaMeth.forEachBlockDepthFirstDom(new SsaBasicBlock.Visitor() {
            @Override
            public void visitBlock (SsaBasicBlock block,
                    SsaBasicBlock unused) {
                block.forEachInsn(new SsaInsn.Visitor() {
                    @Override
                    public void visitMoveInsn(NormalSsaInsn insn) {
                        // do nothing
                    }

                    @Override
                    public void visitPhiInsn(PhiInsn insn) {
                        // do nothing
                    }

                    @Override
                    public void visitNonMoveInsn(NormalSsaInsn insn) {
                        processInsn(insn);
                    }
                });
            }
        });

        // Go through lattice and promote fieldSets as necessary
        for (EscapeSet e : latticeValues) {
            if (e.escape != EscapeState.NONE) {
                for (EscapeSet field : e.childSets) {
                    if (e.escape.compareTo(field.escape) > 0) {
                        field.escape = e.escape;
                    }
                }
            }
        }

        // Perform scalar replacement for arrays
        scalarReplacement();
    }

    /**
     * Replaces instructions that trigger an ArrayIndexOutofBounds exception
     * with an actual throw of the exception.
     *
     * @param insn {@code non-null;} instruction causing the exception
     * @param index {@code non-null;} index value that is out of bounds
     * @param deletedInsns {@code non-null;} set of instructions marked for
     * deletion
     */
    private void insertExceptionThrow(SsaInsn insn, RegisterSpec index,
                                          HashSet<SsaInsn> deletedInsns) {
        // Create a new ArrayIndexOutOfBoundsException
        CstType exception =
            new CstType(Exceptions.TYPE_ArrayIndexOutOfBoundsException);
        insertThrowingInsnBefore(insn, RegisterSpecList.EMPTY, null,
                                     RegOps.NEW_INSTANCE, exception);

        // Add a successor block with a move result pseudo for the exception
        SsaBasicBlock currBlock = insn.getBlock();
        SsaBasicBlock newBlock =
            currBlock.insertNewSuccessor(currBlock.getPrimarySuccessor());
        SsaInsn newInsn = newBlock.getInsns().get(0);
        RegisterSpec newReg =
            RegisterSpec.make(ssaMeth.makeNewSsaReg(), exception);
        insertPlainInsnBefore(newInsn, RegisterSpecList.EMPTY, newReg,
                                  RegOps.MOVE_RESULT_PSEUDO, null);

        // Add another successor block to initialize the exception
        SsaBasicBlock newBlock2 =
            newBlock.insertNewSuccessor(newBlock.getPrimarySuccessor());
        SsaInsn newInsn2 = newBlock2.getInsns().get(0);
        CstNat newNat = new CstNat(new CstString("<init>"), new CstString("(I)V"));
        CstMethodRef newRef = new CstMethodRef(exception, newNat);
        insertThrowingInsnBefore(newInsn2, RegisterSpecList.make(newReg, index),
                                     null, RegOps.INVOKE_DIRECT, newRef);
        deletedInsns.add(newInsn2);

        // Add another successor block to throw the new exception
        SsaBasicBlock newBlock3 =
            newBlock2.insertNewSuccessor(newBlock2.getPrimarySuccessor());
        SsaInsn newInsn3 = newBlock3.getInsns().get(0);
        insertThrowingInsnBefore(newInsn3, RegisterSpecList.make(newReg), null,
                                     RegOps.THROW, null);
        newBlock3.replaceSuccessor(newBlock3.getPrimarySuccessorIndex(),
                                       ssaMeth.getExitBlock().getIndex());
        deletedInsns.add(newInsn3);
    }

    /**
     * Inserts a new PlainInsn before the given instruction.
     * TODO: move this somewhere more appropriate
     *
     * @param insn {@code non-null;} instruction to insert before
     * @param newSources {@code non-null;} sources of new instruction
     * @param newResult {@code non-null;} result of new instruction
     * @param newOpcode opcode of new instruction
     * @param cst {@code null-ok;} constant for new instruction, if any
     */
    private void insertPlainInsnBefore(SsaInsn insn,
        RegisterSpecList newSources, RegisterSpec newResult, int newOpcode,
        Constant cst) {

        Insn originalRopInsn = insn.getOriginalRopInsn();
        Rop newRop;
        if (newOpcode == RegOps.MOVE_RESULT_PSEUDO) {
            newRop = Rops.opMoveResultPseudo(newResult.getType());
        } else {
            newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        }

        Insn newRopInsn;
        if (cst == null) {
            newRopInsn = new PlainInsn(newRop,
                    originalRopInsn.getPosition(), newResult, newSources);
        } else {
            newRopInsn = new PlainCstInsn(newRop,
                originalRopInsn.getPosition(), newResult, newSources, cst);
        }

        NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        List<SsaInsn> insns = insn.getBlock().getInsns();

        insns.add(insns.lastIndexOf(insn), newInsn);
        ssaMeth.onInsnAdded(newInsn);
    }

    /**
     * Inserts a new ThrowingInsn before the given instruction.
     * TODO: move this somewhere more appropriate
     *
     * @param insn {@code non-null;} instruction to insert before
     * @param newSources {@code non-null;} sources of new instruction
     * @param newResult {@code non-null;} result of new instruction
     * @param newOpcode opcode of new instruction
     * @param cst {@code null-ok;} constant for new instruction, if any
     */
    private void insertThrowingInsnBefore(SsaInsn insn,
        RegisterSpecList newSources, RegisterSpec newResult, int newOpcode,
        Constant cst) {

        Insn origRopInsn = insn.getOriginalRopInsn();
        Rop newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        Insn newRopInsn;
        if (cst == null) {
            newRopInsn = new ThrowingInsn(newRop,
                origRopInsn.getPosition(), newSources, StdTypeList.EMPTY);
        } else {
            newRopInsn = new ThrowingCstInsn(newRop,
                origRopInsn.getPosition(), newSources, StdTypeList.EMPTY, cst);
        }

        NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        List<SsaInsn> insns = insn.getBlock().getInsns();

        insns.add(insns.lastIndexOf(insn), newInsn);
        ssaMeth.onInsnAdded(newInsn);
    }
}
