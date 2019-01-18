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

import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.rop.code.CstInsn;
import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.ssa.InterferenceRegisterMapper;
import external.com.android.dx.ssa.NormalSsaInsn;
import external.com.android.dx.ssa.Optimizer;
import external.com.android.dx.ssa.PhiInsn;
import external.com.android.dx.ssa.RegisterMapper;
import external.com.android.dx.ssa.SsaBasicBlock;
import external.com.android.dx.ssa.SsaInsn;
import external.com.android.dx.ssa.SsaMethod;
import external.com.android.dx.util.IntIterator;
import external.com.android.dx.util.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Allocates registers in a first-fit fashion, with the bottom reserved for
 * method parameters and all SSAregisters representing the same local variable
 * kept together if possible.
 */
public class FirstFitLocalCombiningAllocator extends RegisterAllocator {

    /**
     * Alignment constraint that can be used during search of free registers.
     */
    private enum Alignment {
      EVEN {
        @Override
        int nextClearBit(BitSet bitSet, int startIdx) {
          int bitNumber = bitSet.nextClearBit(startIdx);
          while (!isEven(bitNumber)) {
            bitNumber = bitSet.nextClearBit(bitNumber + 1);
          }
          return bitNumber;
        }
      },
      ODD {
        @Override
        int nextClearBit(BitSet bitSet, int startIdx) {
          int bitNumber = bitSet.nextClearBit(startIdx);
          while (isEven(bitNumber)) {
            bitNumber = bitSet.nextClearBit(bitNumber + 1);
          }
          return bitNumber;
        }
      },
      UNSPECIFIED {
        @Override
        int nextClearBit(BitSet bitSet, int startIdx) {
          return bitSet.nextClearBit(startIdx);
        }
      };

      /**
       * Returns the index of the first bit that is set to {@code false} that occurs on or after the
       * specified starting index and that respect {@link Alignment}.
       *
       * @param bitSet bitSet working on.
       * @param startIdx {@code >= 0;} the index to start checking from (inclusive).
       * @return the index of the next clear bit respecting alignment.
       */
      abstract int nextClearBit(BitSet bitSet, int startIdx);
    }

    /** local debug flag */
    private static final boolean DEBUG = false;

    /** maps local variable to a list of associated SSA registers */
    private final Map<LocalItem, ArrayList<RegisterSpec>> localVariables;

    /** list of move-result-pesudo instructions seen in this method */
    private final ArrayList<NormalSsaInsn> moveResultPseudoInsns;

    /** list of invoke-range instructions seen in this method */
    private final ArrayList<NormalSsaInsn> invokeRangeInsns;

    /** list of phi instructions seen in this method */
    private final ArrayList<PhiInsn> phiInsns;

    /** indexed by SSA reg; the set of SSA regs we've mapped */
    private final BitSet ssaRegsMapped;

    /** Register mapper which will be our result */
    private final InterferenceRegisterMapper mapper;

    /** end of rop registers range (starting at 0) reserved for parameters */
    private final int paramRangeEnd;

    /** set of rop registers reserved for parameters or local variables */
    private final BitSet reservedRopRegs;

    /** set of rop registers that have been used by anything */
    private final BitSet usedRopRegs;

    /** true if converter should take steps to minimize rop-form registers */
    private final boolean minimizeRegisters;

    /**
     * Constructs instance.
     *
     * @param ssaMeth {@code non-null;} method to process
     * @param interference non-null interference graph for SSA registers
     * @param minimizeRegisters true if converter should take steps to
     * minimize rop-form registers
     */
    public FirstFitLocalCombiningAllocator(
            SsaMethod ssaMeth, InterferenceGraph interference,
            boolean minimizeRegisters) {
        super(ssaMeth, interference);

        ssaRegsMapped = new BitSet(ssaMeth.getRegCount());

        mapper = new InterferenceRegisterMapper(
                interference, ssaMeth.getRegCount());

        this.minimizeRegisters = minimizeRegisters;

        /*
         * Reserve space for the params at the bottom of the register
         * space. Later, we'll flip the params to the end of the register
         * space.
         */

        paramRangeEnd = ssaMeth.getParamWidth();

        reservedRopRegs = new BitSet(paramRangeEnd * 2);
        reservedRopRegs.set(0, paramRangeEnd);
        usedRopRegs = new BitSet(paramRangeEnd * 2);
        localVariables = new TreeMap<LocalItem, ArrayList<RegisterSpec>>();
        moveResultPseudoInsns = new ArrayList<NormalSsaInsn>();
        invokeRangeInsns = new ArrayList<NormalSsaInsn>();
        phiInsns = new ArrayList<PhiInsn>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean wantsParamsMovedHigh() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public RegisterMapper allocateRegisters() {

        analyzeInstructions();

        if (DEBUG) {
            printLocalVars();
        }

        if (DEBUG) System.out.println("--->Mapping local-associated params");
        handleLocalAssociatedParams();

        if (DEBUG) System.out.println("--->Mapping other params");
        handleUnassociatedParameters();

        if (DEBUG) System.out.println("--->Mapping invoke-range");
        handleInvokeRangeInsns();

        if (DEBUG) {
            System.out.println("--->Mapping local-associated non-params");
        }
        handleLocalAssociatedOther();

        if (DEBUG) System.out.println("--->Mapping check-cast results");
        handleCheckCastResults();

        if (DEBUG) System.out.println("--->Mapping phis");
        handlePhiInsns();

        if (DEBUG) System.out.println("--->Mapping others");
        handleNormalUnassociated();

        return mapper;
    }

    /**
     * Dumps local variable table to stdout for debugging.
     */
    private void printLocalVars() {
        System.out.println("Printing local vars");
        for (Map.Entry<LocalItem, ArrayList<RegisterSpec>> e :
                localVariables.entrySet()) {
            StringBuilder regs = new StringBuilder();

            regs.append('{');
            regs.append(' ');
            for (RegisterSpec reg : e.getValue()) {
                regs.append('v');
                regs.append(reg.getReg());
                regs.append(' ');
            }
            regs.append('}');
            System.out.printf("Local: %s Registers: %s\n", e.getKey(), regs);
        }
    }

    /**
     * Maps all local-associated parameters to rop registers.
     */
    private void handleLocalAssociatedParams() {
        for (ArrayList<RegisterSpec> ssaRegs : localVariables.values()) {
            int sz = ssaRegs.size();
            int paramIndex = -1;
            int paramCategory = 0;

            // First, find out if this local variable is a parameter.
            for (int i = 0; i < sz; i++) {
                RegisterSpec ssaSpec = ssaRegs.get(i);
                int ssaReg = ssaSpec.getReg();

                paramIndex = getParameterIndexForReg(ssaReg);

                if (paramIndex >= 0) {
                    paramCategory = ssaSpec.getCategory();
                    addMapping(ssaSpec, paramIndex);
                    break;
                }
            }

            if (paramIndex < 0) {
                // This local wasn't a parameter.
                continue;
            }

            // Any remaining local-associated registers will be mapped later.
            tryMapRegs(ssaRegs, paramIndex, paramCategory, true);
        }
    }

    /**
     * Gets the parameter index for SSA registers that are method parameters.
     * {@code -1} is returned for non-parameter registers.
     *
     * @param ssaReg {@code >=0;} SSA register to look up
     * @return parameter index or {@code -1} if not a parameter
     */
    private int getParameterIndexForReg(int ssaReg) {
        SsaInsn defInsn = ssaMeth.getDefinitionForRegister(ssaReg);
        if (defInsn == null) {
            return -1;
        }

        Rop opcode = defInsn.getOpcode();

        // opcode == null for phi insns.
        if (opcode != null && opcode.getOpcode() == RegOps.MOVE_PARAM) {
            CstInsn origInsn = (CstInsn) defInsn.getOriginalRopInsn();
            return  ((CstInteger) origInsn.getConstant()).getValue();
        }

        return -1;
    }

    /**
     * Maps all local-associated registers that are not parameters.
     * Tries to find an unreserved range that's wide enough for all of
     * the SSA registers, and then tries to map them all to that
     * range. If not all fit, a new range is tried until all registers
     * have been fit.
     */
    private void handleLocalAssociatedOther() {
        for (ArrayList<RegisterSpec> specs : localVariables.values()) {
            int ropReg = paramRangeEnd;

            boolean done = false;
            do {
                int maxCategory = 1;

                // Compute max category for remaining unmapped registers.
                int sz = specs.size();
                for (int i = 0; i < sz; i++) {
                    RegisterSpec ssaSpec = specs.get(i);
                    int category = ssaSpec.getCategory();
                    if (!ssaRegsMapped.get(ssaSpec.getReg())
                            && category > maxCategory) {
                        maxCategory = category;
                    }
                }

                ropReg = findRopRegForLocal(ropReg, maxCategory);
                if (canMapRegs(specs, ropReg)) {
                    done = tryMapRegs(specs, ropReg, maxCategory, true);
                }

                // Increment for next call to findRopRegForLocal.
                ropReg++;
            } while (!done);
        }
    }

    /**
     * Tries to map a list of SSA registers into the a rop reg, marking
     * used rop space as reserved. SSA registers that don't fit are left
     * unmapped.
     *
     * @param specs {@code non-null;} SSA registers to attempt to map
     * @param ropReg {@code >=0;} rop register to map to
     * @param maxAllowedCategory {@code 1..2;} maximum category
     * allowed in mapping.
     * @param markReserved do so if {@code true}
     * @return {@code true} if all registers were mapped, {@code false}
     * if some remain unmapped
     */
    private boolean tryMapRegs(
            ArrayList<RegisterSpec> specs, int ropReg,
            int maxAllowedCategory, boolean markReserved) {
        boolean remaining = false;
        for (RegisterSpec spec : specs) {
            if (ssaRegsMapped.get(spec.getReg())) {
                continue;
            }

            boolean succeeded;
            succeeded = tryMapReg(spec, ropReg, maxAllowedCategory);
            remaining = !succeeded || remaining;
            if (succeeded && markReserved) {
                // This only needs to be called once really with
                // the widest category used, but <shrug>
                markReserved(ropReg, spec.getCategory());
            }
        }
        return !remaining;
    }

    /**
     * Tries to map an SSA register to a rop register.
     *
     * @param ssaSpec {@code non-null;} SSA register
     * @param ropReg {@code >=0;} rop register
     * @param maxAllowedCategory {@code 1..2;} the maximum category
     * that the SSA register is allowed to be
     * @return {@code true} if map succeeded, {@code false} if not
     */
    private boolean tryMapReg(RegisterSpec ssaSpec, int ropReg,
            int maxAllowedCategory) {
        if (ssaSpec.getCategory() <= maxAllowedCategory
                && !ssaRegsMapped.get(ssaSpec.getReg())
                && canMapReg(ssaSpec, ropReg)) {
            addMapping(ssaSpec, ropReg);
            return true;
        }

        return false;
    }

    /**
     * Marks a range of rop registers as "reserved for a local variable."
     *
     * @param ropReg {@code >= 0;} rop register to reserve
     * @param category {@code > 0;} width to reserve
     */
    private void markReserved(int ropReg, int category) {
        reservedRopRegs.set(ropReg, ropReg + category, true);
    }

    /**
     * Checks to see if any rop registers in the specified range are reserved
     * for local variables or parameters.
     *
     * @param ropRangeStart {@code >= 0;} lowest rop register
     * @param width {@code > 0;} number of rop registers in range.
     * @return {@code true} if any register in range is marked reserved
     */
    private boolean rangeContainsReserved(int ropRangeStart, int width) {
        for (int i = ropRangeStart; i < (ropRangeStart + width); i++) {
            if (reservedRopRegs.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if given rop register represents the {@code this} pointer
     * for a non-static method.
     *
     * @param startReg rop register
     * @return true if the "this" pointer is located here.
     */
    private boolean isThisPointerReg(int startReg) {
        // "this" is always the first parameter.
        return startReg == 0 && !ssaMeth.isStatic();
    }

    /**
     * Return the register alignment constraint to have 64-bits registers that will be align on even
     * dalvik registers after that parameter registers are move up to the top of the frame to match
     * the calling convention.
     *
     * @param regCategory category of the register that will be aligned.
     * @return the register alignment constraint.
     */
    private Alignment getAlignment(int regCategory) {
      Alignment alignment = Alignment.UNSPECIFIED;

      if (DexOptions.ALIGN_64BIT_REGS_SUPPORT && regCategory == 2) {
        if (isEven(paramRangeEnd)) {
          alignment = Alignment.EVEN;
        } else {
          alignment = Alignment.ODD;
        }
      }

      return alignment;
    }

    /**
     * Finds unreserved rop registers with a specific category.
     *
     * @param startReg {@code >= 0;} a rop register to start the search at
     * @param regCategory {@code > 0;} category of the searched registers.
     * @return {@code >= 0;} start of available registers.
     */
    private int findNextUnreservedRopReg(int startReg, int regCategory) {
      return findNextUnreservedRopReg(startReg, regCategory, getAlignment(regCategory));
    }

    /**
     * Finds a range of unreserved rop registers.
     *
     * @param startReg {@code >= 0;} a rop register to start the search at
     * @param width {@code > 0;} the width, in registers, required.
     * @param alignment the alignment constraint.
     * @return {@code >= 0;} start of available register range.
     */
    private int findNextUnreservedRopReg(int startReg, int width, Alignment alignment) {
      int reg = alignment.nextClearBit(reservedRopRegs, startReg);

      while (true) {
        int i = 1;

        while (i < width && !reservedRopRegs.get(reg + i)) {
          i++;
        }

        if (i == width) {
          return reg;
        }

        reg = alignment.nextClearBit(reservedRopRegs, reg + i);
      }
    }

    /**
     * Finds rop registers that can be used for local variables.
     * If {@code MIX_LOCALS_AND_OTHER} is {@code false}, this means any
     * rop register that has not yet been used.
     *
     * @param startReg {@code >= 0;} a rop register to start the search at
     * @param category {@code > 0;} the register category required.
     * @return {@code >= 0;} start of available registers.
     */
    private int findRopRegForLocal(int startReg, int category) {
      Alignment alignment = getAlignment(category);
      int reg = alignment.nextClearBit(usedRopRegs, startReg);

      while (true) {
        int i = 1;

        while (i < category && !usedRopRegs.get(reg + i)) {
          i++;
        }

        if (i == category) {
          return reg;
        }

        reg = alignment.nextClearBit(usedRopRegs, reg + i);
      }
    }

    /**
     * Maps any parameter that isn't local-associated, which can happen
     * in the case where there is no java debug info.
     */
    private void handleUnassociatedParameters() {
        int szSsaRegs = ssaMeth.getRegCount();

        for (int ssaReg = 0; ssaReg < szSsaRegs; ssaReg++) {
            if (ssaRegsMapped.get(ssaReg)) {
                // We already did this one above
                continue;
            }

            int paramIndex = getParameterIndexForReg(ssaReg);

            RegisterSpec ssaSpec = getDefinitionSpecForSsaReg(ssaReg);
            if (paramIndex >= 0) {
                addMapping(ssaSpec, paramIndex);
            }
        }
    }

    /**
     * Handles all insns that want a register range for their sources.
     */
    private void handleInvokeRangeInsns() {
        for (NormalSsaInsn insn : invokeRangeInsns) {
            adjustAndMapSourceRangeRange(insn);
        }
    }

    /**
     * Handles check cast results to reuse the same source register.
     * Inserts a move if it can't map the same register to both and the
     * check cast is not caught.
     */
    private void handleCheckCastResults() {
        for (NormalSsaInsn insn : moveResultPseudoInsns) {
            RegisterSpec moveRegSpec = insn.getResult();
            int moveReg = moveRegSpec.getReg();
            BitSet predBlocks = insn.getBlock().getPredecessors();

            // Expect one predecessor block only
            if (predBlocks.cardinality() != 1) {
                continue;
            }

            SsaBasicBlock predBlock =
                    ssaMeth.getBlocks().get(predBlocks.nextSetBit(0));
            ArrayList<SsaInsn> insnList = predBlock.getInsns();

            /**
             * If the predecessor block has a check-cast, it will be the last
             * instruction
             */
            SsaInsn checkCastInsn = insnList.get(insnList.size() - 1);
            if (checkCastInsn.getOpcode().getOpcode() != RegOps.CHECK_CAST) {
                continue;
            }

            RegisterSpec checkRegSpec = checkCastInsn.getSources().get(0);
            int checkReg = checkRegSpec.getReg();

            /**
             * See if either register is already mapped. Most likely the move
             * result will be mapped already since the cast result is stored
             * in a local variable.
             */
            int category = checkRegSpec.getCategory();
            boolean moveMapped = ssaRegsMapped.get(moveReg);
            boolean checkMapped = ssaRegsMapped.get(checkReg);
            if (moveMapped & !checkMapped) {
                int moveRopReg = mapper.oldToNew(moveReg);
                checkMapped = tryMapReg(checkRegSpec, moveRopReg, category);
            }
            if (checkMapped & !moveMapped) {
                int checkRopReg = mapper.oldToNew(checkReg);
                moveMapped = tryMapReg(moveRegSpec, checkRopReg, category);
            }

            // Map any unmapped registers to anything available
            if (!moveMapped || !checkMapped) {
                int ropReg = findNextUnreservedRopReg(paramRangeEnd, category);
                ArrayList<RegisterSpec> ssaRegs =
                    new ArrayList<RegisterSpec>(2);
                ssaRegs.add(moveRegSpec);
                ssaRegs.add(checkRegSpec);

                while (!tryMapRegs(ssaRegs, ropReg, category, false)) {
                    ropReg = findNextUnreservedRopReg(ropReg + 1, category);
                }
            }

            /*
             * If source and result have a different mapping, insert a move so
             * they can have the same mapping. Don't do this if the check cast
             * is caught, since it will overwrite a potentially live value.
             */
            boolean hasExceptionHandlers =
                checkCastInsn.getOriginalRopInsn().getCatches().size() != 0;
            int moveRopReg = mapper.oldToNew(moveReg);
            int checkRopReg = mapper.oldToNew(checkReg);
            if (moveRopReg != checkRopReg && !hasExceptionHandlers) {
                ((NormalSsaInsn) checkCastInsn).changeOneSource(0,
                        insertMoveBefore(checkCastInsn, checkRegSpec));
                addMapping(checkCastInsn.getSources().get(0), moveRopReg);
            }
        }
    }

    /**
    * Handles all phi instructions, trying to map them to a common register.
    */
    private void handlePhiInsns() {
        for (PhiInsn insn : phiInsns) {
            processPhiInsn(insn);
        }
    }

    /**
     * Maps all non-parameter, non-local variable registers.
     */
    private void handleNormalUnassociated() {
        int szSsaRegs = ssaMeth.getRegCount();

        for (int ssaReg = 0; ssaReg < szSsaRegs; ssaReg++) {
            if (ssaRegsMapped.get(ssaReg)) {
                // We already did this one
                continue;
            }

            RegisterSpec ssaSpec = getDefinitionSpecForSsaReg(ssaReg);

            if (ssaSpec == null) continue;

            int category = ssaSpec.getCategory();
            // Find a rop reg that does not interfere
            int ropReg = findNextUnreservedRopReg(paramRangeEnd, category);
            while (!canMapReg(ssaSpec, ropReg)) {
                ropReg = findNextUnreservedRopReg(ropReg + 1, category);
            }

            addMapping(ssaSpec, ropReg);
        }
    }

    /**
     * Checks to see if a list of SSA registers can all be mapped into
     * the same rop reg. Ignores registers that have already been mapped,
     * and checks the interference graph and ensures the range does not
     * cross the parameter range.
     *
     * @param specs {@code non-null;} SSA registers to check
     * @param ropReg {@code >=0;} rop register to check mapping to
     * @return {@code true} if all unmapped registers can be mapped
     */
    private boolean canMapRegs(ArrayList<RegisterSpec> specs, int ropReg) {
        for (RegisterSpec spec : specs) {
            if (ssaRegsMapped.get(spec.getReg())) continue;
            if (!canMapReg(spec, ropReg)) return false;
        }
        return true;
    }

    /**
     * Checks to see if {@code ssaSpec} can be mapped to
     * {@code ropReg}. Checks interference graph and ensures
     * the range does not cross the parameter range.
     *
     * @param ssaSpec {@code non-null;} SSA spec
     * @param ropReg prosepctive new-namespace reg
     * @return {@code true} if mapping is possible
     */
    private boolean canMapReg(RegisterSpec ssaSpec, int ropReg) {
        int category = ssaSpec.getCategory();
        return !(spansParamRange(ropReg, category)
                || mapper.interferes(ssaSpec, ropReg));
    }

    /**
     * Returns true if the specified rop register + category
     * will cross the boundry between the lower {@code paramWidth}
     * registers reserved for method params and the upper registers. We cannot
     * allocate a register that spans the param block and the normal block,
     * because we will be moving the param block to high registers later.
     *
     * @param ssaReg register in new namespace
     * @param category width that the register will have
     * @return {@code true} in the case noted above
     */
    private boolean spansParamRange(int ssaReg, int category) {
        return ((ssaReg < paramRangeEnd)
                && ((ssaReg + category) > paramRangeEnd));
    }

    /**
     * Analyze each instruction and find out all the local variable assignments
     * and move-result-pseudo/invoke-range instrucitons.
     */
    private void analyzeInstructions() {
        ssaMeth.forEachInsn(new SsaInsn.Visitor() {
            /** {@inheritDoc} */
            @Override
            public void visitMoveInsn(NormalSsaInsn insn) {
                processInsn(insn);
            }

            /** {@inheritDoc} */
            @Override
            public void visitPhiInsn(PhiInsn insn) {
                processInsn(insn);
            }

            /** {@inheritDoc} */
            @Override
            public void visitNonMoveInsn(NormalSsaInsn insn) {
                processInsn(insn);
            }

            /**
             * This method collects three types of instructions:
             *
             * 1) Adds a local variable assignment to the
             *    {@code localVariables} map.
             * 2) Add move-result-pseudo to the
             *    {@code moveResultPseudoInsns} list.
             * 3) Add invoke-range to the
             *    {@code invokeRangeInsns} list.
             *
             * @param insn {@code non-null;} insn that may represent a
             * local variable assignment
             */
            private void processInsn(SsaInsn insn) {
                RegisterSpec assignment;
                assignment = insn.getLocalAssignment();

                if (assignment != null) {
                    LocalItem local = assignment.getLocalItem();

                    ArrayList<RegisterSpec> regList
                        = localVariables.get(local);

                    if (regList == null) {
                        regList = new ArrayList<RegisterSpec>();
                        localVariables.put(local, regList);
                    }

                    regList.add(assignment);
                }

                if (insn instanceof NormalSsaInsn) {
                    if (insn.getOpcode().getOpcode() ==
                            RegOps.MOVE_RESULT_PSEUDO) {
                        moveResultPseudoInsns.add((NormalSsaInsn) insn);
                    } else if (Optimizer.getAdvice().requiresSourcesInOrder(
                            insn.getOriginalRopInsn().getOpcode(),
                            insn.getSources())) {
                        invokeRangeInsns.add((NormalSsaInsn) insn);
                    }
                } else if (insn instanceof PhiInsn) {
                    phiInsns.add((PhiInsn) insn);
                }

            }
        });
    }

    /**
     * Adds a mapping from an SSA register to a rop register.
     * {@link #canMapReg} should have already been called.
     *
     * @param ssaSpec {@code non-null;} SSA register to map from
     * @param ropReg {@code >=0;} rop register to map to
     */
    private void addMapping(RegisterSpec ssaSpec, int ropReg) {
        int ssaReg = ssaSpec.getReg();

        // An assertion.
        if (ssaRegsMapped.get(ssaReg) || !canMapReg(ssaSpec, ropReg)) {
            throw new RuntimeException(
                    "attempt to add invalid register mapping");
        }

        if (DEBUG) {
            System.out.printf("Add mapping s%d -> v%d c:%d\n",
                    ssaSpec.getReg(), ropReg, ssaSpec.getCategory());
        }

        int category = ssaSpec.getCategory();
        mapper.addMapping(ssaSpec.getReg(), ropReg, category);
        ssaRegsMapped.set(ssaReg);
        usedRopRegs.set(ropReg, ropReg + category);
    }


    /**
     * Maps the source registers of the specified instruction such that they
     * will fall in a contiguous range in rop form. Moves are inserted as
     * necessary to allow the range to be allocated.
     *
     * @param insn {@code non-null;} insn whos sources to process
     */
    private void adjustAndMapSourceRangeRange(NormalSsaInsn insn) {
        int newRegStart = findRangeAndAdjust(insn);

        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        int nextRopReg = newRegStart;

        for (int i = 0; i < szSources; i++) {
            RegisterSpec source = sources.get(i);
            int sourceReg = source.getReg();
            int category = source.getCategory();
            int curRopReg = nextRopReg;
            nextRopReg += category;

            if (ssaRegsMapped.get(sourceReg)) {
                continue;
            }

            LocalItem localItem = getLocalItemForReg(sourceReg);
            addMapping(source, curRopReg);

            if (localItem != null) {
                markReserved(curRopReg, category);
                ArrayList<RegisterSpec> similarRegisters
                        = localVariables.get(localItem);

                int szSimilar = similarRegisters.size();

                /*
                 * Try to map all SSA registers also associated with
                 * this local.
                 */
                for (int j = 0; j < szSimilar; j++) {
                    RegisterSpec similarSpec = similarRegisters.get(j);
                    int similarReg = similarSpec.getReg();

                    // Don't map anything that's also a source.
                    if (-1 != sources.indexOfRegister(similarReg)) {
                        continue;
                    }

                    // Registers left unmapped will get handled later.
                    tryMapReg(similarSpec, curRopReg, category);
                }
            }
        }
    }

    /**
     * Find a contiguous rop register range that fits the specified
     * instruction's sources. First, try to center the range around
     * sources that have already been mapped to rop registers. If that fails,
     * just find a new contiguous range that doesn't interfere.
     *
     * @param insn {@code non-null;} the insn whose sources need to
     * fit. Must be last insn in basic block.
     * @return {@code >= 0;} rop register of start of range
     */
    private int findRangeAndAdjust(NormalSsaInsn insn) {
        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        // the category for each source index
        int categoriesForIndex[] = new int[szSources];
        int rangeLength = 0;

        // Compute rangeLength and categoriesForIndex
        for (int i = 0; i < szSources; i++) {
            int category = sources.get(i).getCategory();
            categoriesForIndex[i] = category;
            rangeLength += categoriesForIndex[i];
        }

        // the highest score of fits tried so far
        int maxScore = Integer.MIN_VALUE;
        // the high scoring range's start
        int resultRangeStart = -1;
        // by source index: set of sources needing moves in high scoring plan
        BitSet resultMovesRequired = null;

        /*
         * First, go through each source that's already been mapped. Try
         * to center the range around the rop register this source is mapped
         * to.
         */
        int rangeStartOffset = 0;
        for (int i = 0; i < szSources; i++) {
            int ssaCenterReg = sources.get(i).getReg();

            if (i != 0) {
                rangeStartOffset -= categoriesForIndex[i - 1];
            }
            if (!ssaRegsMapped.get(ssaCenterReg)) {
                continue;
            }

            int rangeStart = mapper.oldToNew(ssaCenterReg) + rangeStartOffset;

            if (rangeStart < 0 || spansParamRange(rangeStart, rangeLength)) {
                continue;
            }

            BitSet curMovesRequired = new BitSet(szSources);

            int fitWidth
                    = fitPlanForRange(rangeStart, insn, categoriesForIndex,
                    curMovesRequired);

            if (fitWidth < 0) {
                continue;
            }

            int score = fitWidth - curMovesRequired.cardinality();

            if (score > maxScore) {
                maxScore = score;
                resultRangeStart = rangeStart;
                resultMovesRequired = curMovesRequired;
            }

            if (fitWidth == rangeLength) {
                // We can't do any better than this, so stop here
                break;
            }
        }

        /*
         * If we were unable to find a plan for a fit centered around
         * an already-mapped source, just try to find a range of
         * registers we can move the range into.
         */

        if (resultRangeStart == -1) {
            resultMovesRequired = new BitSet(szSources);

            resultRangeStart = findAnyFittingRange(insn, rangeLength,
                    categoriesForIndex, resultMovesRequired);
        }

        /*
         * Now, insert any moves required.
         */

        for (int i = resultMovesRequired.nextSetBit(0); i >= 0;
             i = resultMovesRequired.nextSetBit(i+1)) {
            insn.changeOneSource(i, insertMoveBefore(insn, sources.get(i)));
        }

        return resultRangeStart;
    }

    /**
     * Finds an unreserved range that will fit the sources of the
     * specified instruction. Does not bother trying to center the range
     * around an already-mapped source register;
     *
     * @param insn {@code non-null;} insn to build range for
     * @param rangeLength {@code >=0;} length required in register units
     * @param categoriesForIndex {@code non-null;} indexed by source index;
     * the category for each source
     * @param outMovesRequired {@code non-null;} an output parameter indexed by
     * source index that will contain the set of sources which need
     * moves inserted
     * @return the rop register that starts the fitting range
     */
    private int findAnyFittingRange(NormalSsaInsn insn, int rangeLength,
            int[] categoriesForIndex, BitSet outMovesRequired) {
        Alignment alignment = Alignment.UNSPECIFIED;

        if (DexOptions.ALIGN_64BIT_REGS_SUPPORT) {
          int regNumber = 0;
          int p64bitsAligned = 0;
          int p64bitsNotAligned = 0;
          for (int category : categoriesForIndex) {
            if (category == 2) {
              if (isEven(regNumber)) {
                p64bitsAligned++;
              } else {
                p64bitsNotAligned++;
              }
              regNumber += 2;
            } else {
              regNumber += 1;
            }
          }

          if (p64bitsNotAligned > p64bitsAligned) {
            if (isEven(paramRangeEnd)) {
              alignment = Alignment.ODD;
            } else {
              alignment = Alignment.EVEN;
            }
          } else if (p64bitsAligned > 0) {
            if (isEven(paramRangeEnd)) {
              alignment = Alignment.EVEN;
            } else {
              alignment = Alignment.ODD;
            }
          }
        }

        int rangeStart = paramRangeEnd;
        while (true) {
          rangeStart = findNextUnreservedRopReg(rangeStart, rangeLength, alignment);

          int fitWidth = fitPlanForRange(rangeStart, insn, categoriesForIndex, outMovesRequired);

          if (fitWidth >= 0) {
            break;
          }
          rangeStart++;
          outMovesRequired.clear();
        }

        return rangeStart;
    }

    /**
     * Attempts to build a plan for fitting a range of sources into rop
     * registers.
     *
     * @param ropReg {@code >= 0;} rop reg that begins range
     * @param insn {@code non-null;} insn to plan range for
     * @param categoriesForIndex {@code non-null;} indexed by source index;
     * the category for each source
     * @param outMovesRequired {@code non-null;} an output parameter indexed by
     * source index that will contain the set of sources which need
     * moves inserted
     * @return the width of the fit that that does not involve added moves or
     * {@code -1} if "no fit possible"
     */
    private int fitPlanForRange(int ropReg, NormalSsaInsn insn,
            int[] categoriesForIndex, BitSet outMovesRequired) {
        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        int fitWidth = 0;
        IntSet liveOut = insn.getBlock().getLiveOutRegs();
        RegisterSpecList liveOutSpecs = ssaSetToSpecs(liveOut);

        // An SSA reg may only be mapped into a range once.
        BitSet seen = new BitSet(ssaMeth.getRegCount());

        for (int i = 0; i < szSources ; i++) {
            RegisterSpec ssaSpec = sources.get(i);
            int ssaReg = ssaSpec.getReg();
            int category = categoriesForIndex[i];

            if (i != 0) {
                ropReg += categoriesForIndex[i-1];
            }

            if (ssaRegsMapped.get(ssaReg)
                    && mapper.oldToNew(ssaReg) == ropReg) {
                // This is a register that is already mapped appropriately.
                fitWidth += category;
            } else if (rangeContainsReserved(ropReg, category)) {
                fitWidth = -1;
                break;
            } else if (!ssaRegsMapped.get(ssaReg)
                    && canMapReg(ssaSpec, ropReg)
                    && !seen.get(ssaReg)) {
                // This is a register that can be mapped appropriately.
                fitWidth += category;
            } else if (!mapper.areAnyPinned(liveOutSpecs, ropReg, category)
                    && !mapper.areAnyPinned(sources, ropReg, category)) {
                /*
                 * This is a source that can be moved. We can insert a
                 * move as long as:
                 *
                 *   * no SSA register pinned to the desired rop reg
                 *     is live out on the block
                 *
                 *   * no SSA register pinned to desired rop reg is
                 *     a source of this insn (since this may require
                 *     overlapping moves, which we can't presently handle)
                 */

                outMovesRequired.set(i);
            } else {
                fitWidth = -1;
                break;
            }

            seen.set(ssaReg);
        }
        return fitWidth;
    }

    /**
     * Converts a bit set of SSA registers into a RegisterSpecList containing
     * the definition specs of all the registers.
     *
     * @param ssaSet {@code non-null;} set of SSA registers
     * @return list of RegisterSpecs as noted above
     */
    RegisterSpecList ssaSetToSpecs(IntSet ssaSet) {
        RegisterSpecList result = new RegisterSpecList(ssaSet.elements());

        IntIterator iter = ssaSet.iterator();

        int i = 0;
        while (iter.hasNext()) {
            result.set(i++, getDefinitionSpecForSsaReg(iter.next()));
        }

        return result;
    }

    /**
     * Gets a local item associated with an ssa register, if one exists.
     *
     * @param ssaReg {@code >= 0;} SSA register
     * @return {@code null-ok;} associated local item or null
     */
    private LocalItem getLocalItemForReg(int ssaReg) {
        for (Map.Entry<LocalItem, ArrayList<RegisterSpec>> entry :
                 localVariables.entrySet()) {
            for (RegisterSpec spec : entry.getValue()) {
                if (spec.getReg() == ssaReg) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    /**
     * Attempts to map the sources and result of a phi to a common register.
     * Will try existing mappings first, from most to least common. If none
     * of the registers have mappings yet, a new mapping is created.
     */
    private void processPhiInsn(PhiInsn insn) {
        RegisterSpec result = insn.getResult();
        int resultReg = result.getReg();
        int category = result.getCategory();

        RegisterSpecList sources = insn.getSources();
        int sourcesSize = sources.size();

        // List of phi sources / result that need mapping
        ArrayList<RegisterSpec> ssaRegs = new ArrayList<RegisterSpec>();

        // Track how many times a particular mapping is found
        Multiset mapSet = new Multiset(sourcesSize + 1);

        /*
         * If the result of the phi has an existing mapping, get it.
         * Otherwise, add it to the list of regs that need mapping.
         */
        if (ssaRegsMapped.get(resultReg)) {
            mapSet.add(mapper.oldToNew(resultReg));
        } else {
            ssaRegs.add(result);
        }

        for (int i = 0; i < sourcesSize; i++) {
            RegisterSpec source = sources.get(i);
            SsaInsn def = ssaMeth.getDefinitionForRegister(source.getReg());
            RegisterSpec sourceDef = def.getResult();
            int sourceReg = sourceDef.getReg();

            /*
             * If a source of the phi has an existing mapping, get it.
             * Otherwise, add it to the list of regs that need mapping.
             */
            if (ssaRegsMapped.get(sourceReg)) {
                mapSet.add(mapper.oldToNew(sourceReg));
            } else {
                ssaRegs.add(sourceDef);
            }
        }

        // Try all existing mappings, with the most common ones first
        for (int i = 0; i < mapSet.getSize(); i++) {
            int maxReg = mapSet.getAndRemoveHighestCount();
            tryMapRegs(ssaRegs, maxReg, category, false);
        }

        // Map any remaining unmapped regs with whatever fits
        int mapReg = findNextUnreservedRopReg(paramRangeEnd, category);
        while (!tryMapRegs(ssaRegs, mapReg, category, false)) {
            mapReg = findNextUnreservedRopReg(mapReg + 1, category);
        }
    }

    private static boolean isEven(int regNumger) {
      return ((regNumger & 1) == 0);
    }

    // A set that tracks how often elements are added to it.
    private static class Multiset {
        private final int[] reg;
        private final int[] count;
        private int size;

        /**
         * Constructs an instance.
         *
         * @param maxSize the maximum distinct elements the set may have
         */
        public Multiset(int maxSize) {
            reg = new int[maxSize];
            count = new int[maxSize];
            size = 0;
        }

        /**
         * Adds an element to the set.
         *
         * @param element element to add
         */
        public void add(int element) {
            for (int i = 0; i < size; i++) {
                if (reg[i] == element) {
                    count[i]++;
                    return;
                }
            }

            reg[size] = element;
            count[size] = 1;
            size++;
        }

        /**
         * Searches the set for the element that has been added the most.
         * In the case of a tie, the element that was added first is returned.
         * Then, it clears the count on that element. The size of the set
         * remains unchanged.
         *
         * @return element with the highest count
         */
        public int getAndRemoveHighestCount() {
            int maxIndex = -1;
            int maxReg = -1;
            int maxCount = 0;

            for (int i = 0; i < size; i++) {
                if (maxCount < count[i]) {
                    maxIndex = i;
                    maxReg = reg[i];
                    maxCount = count[i];
                }
            }

            count[maxIndex] = 0;
            return maxReg;
        }

        /**
         * Gets the number of distinct elements in the set.
         *
         * @return size of the set
         */
        public int getSize() {
            return size;
        }
    }
}
