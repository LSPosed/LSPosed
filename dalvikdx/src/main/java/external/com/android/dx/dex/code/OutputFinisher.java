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

package external.com.android.dx.dex.code;

import external.com.android.dex.DexException;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.RegisterSpecSet;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstMemberRef;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.ssa.BasicRegisterMapper;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/**
 * Processor for instruction lists, which takes a "first cut" of
 * instruction selection as a basis and produces a "final cut" in the
 * form of a {@link DalvInsnList} instance.
 */
public final class OutputFinisher {
    /** {@code non-null;} options for dex output */
    private final DexOptions dexOptions;

    /**
     * {@code >= 0;} register count for the method, not including any extra
     * "reserved" registers needed to translate "difficult" instructions
     */
    private final int unreservedRegCount;

    /** {@code non-null;} the list of instructions, per se */
    private ArrayList<DalvInsn> insns;

    /** whether any instruction has position info */
    private boolean hasAnyPositionInfo;

    /** whether any instruction has local variable info */
    private boolean hasAnyLocalInfo;

    /**
     * {@code >= 0;} the count of reserved registers (low-numbered
     * registers used when expanding instructions that can't be
     * represented simply); becomes valid after a call to {@link
     * #massageInstructions}
     */
    private int reservedCount;

    /**
     * {@code >= 0;} the count of reserved registers just before parameters in order to align them.
     */
    private int reservedParameterCount;

    /**
     * Size, in register units, of all the parameters to this method
     */
    private final int paramSize;

    /**
     * Constructs an instance. It initially contains no instructions.
     *
     * @param dexOptions {@code non-null;} options for dex output
     * @param initialCapacity {@code >= 0;} initial capacity of the
     * instructions list
     * @param regCount {@code >= 0;} register count for the method
     * @param paramSize size, in register units, of all the parameters for this method
     */
    public OutputFinisher(DexOptions dexOptions, int initialCapacity, int regCount, int paramSize) {
        this.dexOptions = dexOptions;
        this.unreservedRegCount = regCount;
        this.insns = new ArrayList<DalvInsn>(initialCapacity);
        this.reservedCount = -1;
        this.hasAnyPositionInfo = false;
        this.hasAnyLocalInfo = false;
        this.paramSize = paramSize;
    }

    /**
     * Returns whether any of the instructions added to this instance
     * come with position info.
     *
     * @return whether any of the instructions added to this instance
     * come with position info
     */
    public boolean hasAnyPositionInfo() {
        return hasAnyPositionInfo;
    }

    /**
     * Returns whether this instance has any local variable information.
     *
     * @return whether this instance has any local variable information
     */
    public boolean hasAnyLocalInfo() {
        return hasAnyLocalInfo;
    }

    /**
     * Helper for {@link #add} which scrutinizes a single
     * instruction for local variable information.
     *
     * @param insn {@code non-null;} instruction to scrutinize
     * @return {@code true} iff the instruction refers to any
     * named locals
     */
    private static boolean hasLocalInfo(DalvInsn insn) {
        if (insn instanceof LocalSnapshot) {
            RegisterSpecSet specs = ((LocalSnapshot) insn).getLocals();
            int size = specs.size();
            for (int i = 0; i < size; i++) {
                if (hasLocalInfo(specs.get(i))) {
                    return true;
                }
            }
        } else if (insn instanceof LocalStart) {
            RegisterSpec spec = ((LocalStart) insn).getLocal();
            if (hasLocalInfo(spec)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper for {@link #hasAnyLocalInfo} which scrutinizes a single
     * register spec.
     *
     * @param spec {@code non-null;} spec to scrutinize
     * @return {@code true} iff the spec refers to any
     * named locals
     */
    private static boolean hasLocalInfo(RegisterSpec spec) {
        return (spec != null)
            && (spec.getLocalItem().getName() != null);
    }

    /**
     * Returns the set of all constants referred to by instructions added
     * to this instance.
     *
     * @return {@code non-null;} the set of constants
     */
    public HashSet<Constant> getAllConstants() {
        HashSet<Constant> result = new HashSet<Constant>(20);

        for (DalvInsn insn : insns) {
            addConstants(result, insn);
        }

        return result;
    }

    /**
     * Helper for {@link #getAllConstants} which adds all the info for
     * a single instruction.
     *
     * @param result {@code non-null;} result set to add to
     * @param insn {@code non-null;} instruction to scrutinize
     */
    private static void addConstants(HashSet<Constant> result,
            DalvInsn insn) {
        if (insn instanceof CstInsn) {
            Constant cst = ((CstInsn) insn).getConstant();
            result.add(cst);
        } else if (insn instanceof MultiCstInsn) {
            MultiCstInsn m = (MultiCstInsn) insn;
            for (int i = 0; i < m.getNumberOfConstants(); i++) {
                result.add(m.getConstant(i));
            }
        } else if (insn instanceof LocalSnapshot) {
            RegisterSpecSet specs = ((LocalSnapshot) insn).getLocals();
            int size = specs.size();
            for (int i = 0; i < size; i++) {
                addConstants(result, specs.get(i));
            }
        } else if (insn instanceof LocalStart) {
            RegisterSpec spec = ((LocalStart) insn).getLocal();
            addConstants(result, spec);
        }
    }

    /**
     * Helper for {@link #getAllConstants} which adds all the info for
     * a single {@code RegisterSpec}.
     *
     * @param result {@code non-null;} result set to add to
     * @param spec {@code null-ok;} register spec to add
     */
    private static void addConstants(HashSet<Constant> result,
            RegisterSpec spec) {
        if (spec == null) {
            return;
        }

        LocalItem local = spec.getLocalItem();
        CstString name = local.getName();
        CstString signature = local.getSignature();
        Type type = spec.getType();

        if (type != Type.KNOWN_NULL) {
            result.add(CstType.intern(type));
        } else {
            /* If this a "known null", let's use "Object" because that's going to be the
             * resulting type in {@link LocalList.MakeState#filterSpec} */
            result.add(CstType.intern(Type.OBJECT));
        }

        if (name != null) {
            result.add(name);
        }

        if (signature != null) {
            result.add(signature);
        }
    }

    /**
     * Adds an instruction to the output.
     *
     * @param insn {@code non-null;} the instruction to add
     */
    public void add(DalvInsn insn) {
        insns.add(insn);
        updateInfo(insn);
    }

    /**
     * Inserts an instruction in the output at the given offset.
     *
     * @param at {@code at >= 0;} what index to insert at
     * @param insn {@code non-null;} the instruction to insert
     */
    public void insert(int at, DalvInsn insn) {
        insns.add(at, insn);
        updateInfo(insn);
    }

    /**
     * Helper for {@link #add} and {@link #insert},
     * which updates the position and local info flags.
     *
     * @param insn {@code non-null;} an instruction that was just introduced
     */
    private void updateInfo(DalvInsn insn) {
        if (! hasAnyPositionInfo) {
            SourcePosition pos = insn.getPosition();
            if (pos.getLine() >= 0) {
                hasAnyPositionInfo = true;
            }
        }

        if (! hasAnyLocalInfo) {
            if (hasLocalInfo(insn)) {
                hasAnyLocalInfo = true;
            }
        }
    }

    /**
     * Reverses a branch which is buried a given number of instructions
     * backward in the output. It is illegal to call this unless the
     * indicated instruction really is a reversible branch.
     *
     * @param which how many instructions back to find the branch;
     * {@code 0} is the most recently added instruction,
     * {@code 1} is the instruction before that, etc.
     * @param newTarget {@code non-null;} the new target for the
     * reversed branch
     */
    public void reverseBranch(int which, CodeAddress newTarget) {
        int size = insns.size();
        int index = size - which - 1;
        TargetInsn targetInsn;

        try {
            targetInsn = (TargetInsn) insns.get(index);
        } catch (IndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("too few instructions");
        } catch (ClassCastException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("non-reversible instruction");
        }

        /*
         * No need to call this.set(), since the format and other info
         * are the same.
         */
        insns.set(index, targetInsn.withNewTargetAndReversed(newTarget));
    }

    /**
     * Assigns indices in all instructions that need them, using the
     * given callback to perform lookups. This should be called before
     * calling {@link #finishProcessingAndGetList}.
     *
     * @param callback {@code non-null;} callback object
     */
    public void assignIndices(DalvCode.AssignIndicesCallback callback) {
        for (DalvInsn insn : insns) {
            if (insn instanceof CstInsn) {
                assignIndices((CstInsn) insn, callback);
            } else if (insn instanceof MultiCstInsn) {
                assignIndices((MultiCstInsn) insn, callback);
            }
        }
    }

    /**
     * Helper for {@link #assignIndices} which does assignment for one
     * instruction.
     *
     * @param insn {@code non-null;} the instruction
     * @param callback {@code non-null;} the callback
     */
    private static void assignIndices(CstInsn insn,
            DalvCode.AssignIndicesCallback callback) {
        Constant cst = insn.getConstant();
        int index = callback.getIndex(cst);

        if (index >= 0) {
            insn.setIndex(index);
        }

        if (cst instanceof CstMemberRef) {
            CstMemberRef member = (CstMemberRef) cst;
            CstType definer = member.getDefiningClass();
            index = callback.getIndex(definer);
            // TODO(oth): what scenarios is this guard valid under? Is it not just an error?
            if (index >= 0) {
                insn.setClassIndex(index);
            }
        }
    }

    /**
     * Helper for {@link #assignIndices} which does assignment for one
     * instruction.
     *
     * @param insn {@code non-null;} the instruction
     * @param callback {@code non-null;} the callback
     */
    private static void assignIndices(MultiCstInsn insn, DalvCode.AssignIndicesCallback callback) {
        for (int i = 0; i < insn.getNumberOfConstants(); ++i) {
            Constant cst = insn.getConstant(i);
            int index = callback.getIndex(cst);
            insn.setIndex(i, index);

            if (cst instanceof CstMemberRef) {
                CstMemberRef member = (CstMemberRef) cst;
                CstType definer = member.getDefiningClass();
                index = callback.getIndex(definer);
                insn.setClassIndex(index);
            }
        }
    }

    /**
     * Does final processing on this instance and gets the output as
     * a {@link DalvInsnList}. Final processing consists of:
     *
     * <ul>
     *   <li>optionally renumbering registers (to make room as needed for
     *   expanded instructions)</li>
     *   <li>picking a final opcode for each instruction</li>
     *   <li>rewriting instructions, because of register number,
     *   constant pool index, or branch target size issues</li>
     *   <li>assigning final addresses</li>
     * </ul>
     *
     * <p><b>Note:</b> This method may only be called once per instance
     * of this class.</p>
     *
     * @return {@code non-null;} the output list
     * @throws UnsupportedOperationException if this method has
     * already been called
     */
    public DalvInsnList finishProcessingAndGetList() {
        if (reservedCount >= 0) {
            throw new UnsupportedOperationException("already processed");
        }

        Dop[] opcodes = makeOpcodesArray();
        reserveRegisters(opcodes);
        if (dexOptions.ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER) {
          align64bits(opcodes);
        }
        massageInstructions(opcodes);
        assignAddressesAndFixBranches();

        return DalvInsnList.makeImmutable(insns, reservedCount + unreservedRegCount
            + reservedParameterCount);
    }

    /**
     * Helper for {@link #finishProcessingAndGetList}, which extracts
     * the opcode out of each instruction into a separate array, to be
     * further manipulated as things progress.
     *
     * @return {@code non-null;} the array of opcodes
     */
    private Dop[] makeOpcodesArray() {
        int size = insns.size();
        Dop[] result = new Dop[size];

        for (int i = 0; i < size; i++) {
            DalvInsn insn = insns.get(i);
            result[i] = insn.getOpcode();
        }

        return result;
    }

    /**
     * Helper for {@link #finishProcessingAndGetList}, which figures
     * out how many reserved registers are required and then reserving
     * them. It also updates the given {@code opcodes} array so
     * as to avoid extra work when constructing the massaged
     * instruction list.
     *
     * @param opcodes {@code non-null;} array of per-instruction
     * opcode selections
     * @return true if reservedCount is expanded, false otherwise
     */
    private boolean reserveRegisters(Dop[] opcodes) {
        boolean reservedCountExpanded = false;
        int oldReservedCount = (reservedCount < 0) ? 0 : reservedCount;

        /*
         * Call calculateReservedCount() and then perform register
         * reservation, repeatedly until no new reservations happen.
         */
        for (;;) {
            int newReservedCount = calculateReservedCount(opcodes);
            if (oldReservedCount >= newReservedCount) {
                break;
            }

            reservedCountExpanded = true;

            int reservedDifference = newReservedCount - oldReservedCount;
            int size = insns.size();

            for (int i = 0; i < size; i++) {
                /*
                 * CodeAddress instance identity is used to link
                 * TargetInsns to their targets, so it is
                 * inappropriate to make replacements, and they don't
                 * have registers in any case. Hence, the instanceof
                 * test below.
                 */
                DalvInsn insn = insns.get(i);
                if (!(insn instanceof CodeAddress)) {
                    /*
                     * No need to call this.set() since the format and
                     * other info are the same.
                     */
                    insns.set(i, insn.withRegisterOffset(reservedDifference));
                }
            }

            oldReservedCount = newReservedCount;
        }

        reservedCount = oldReservedCount;

        return reservedCountExpanded;
    }

    /**
     * Helper for {@link #reserveRegisters}, which does one
     * pass over the instructions, calculating the number of
     * registers that need to be reserved. It also updates the
     * {@code opcodes} list to help avoid extra work in future
     * register reservation passes.
     *
     * @param opcodes {@code non-null;} array of per-instruction
     * opcode selections
     * @return {@code >= 0;} the count of reserved registers
     */
    private int calculateReservedCount(Dop[] opcodes) {
        int size = insns.size();

        /*
         * Potential new value of reservedCount, which gets updated in the
         * following loop. It starts out with the existing reservedCount
         * and gets increased if it turns out that additional registers
         * need to be reserved.
         */
        int newReservedCount = reservedCount;

        for (int i = 0; i < size; i++) {
            DalvInsn insn = insns.get(i);
            Dop originalOpcode = opcodes[i];
            Dop newOpcode = findOpcodeForInsn(insn, originalOpcode);

            if (newOpcode == null) {
                /*
                 * The instruction will need to be expanded, so find the
                 * expanded opcode and reserve registers for it.
                 */
                Dop expandedOp = findExpandedOpcodeForInsn(insn);
                BitSet compatRegs = expandedOp.getFormat().compatibleRegs(insn);
                int reserve = insn.getMinimumRegisterRequirement(compatRegs);
                if (reserve > newReservedCount) {
                    newReservedCount = reserve;
                }
            } else if (originalOpcode == newOpcode) {
                continue;
            }

            opcodes[i] = newOpcode;
        }

        return newReservedCount;
    }

    /**
     * Attempts to fit the given instruction into a specific opcode,
     * returning the opcode whose format that the instruction fits
     * into or {@code null} to indicate that the instruction will need
     * to be expanded. This fitting process starts with the given
     * opcode as a first "best guess" and then pessimizes from there
     * if necessary.
     *
     * @param insn {@code non-null;} the instruction in question
     * @param guess {@code null-ok;} the current guess as to the best
     * opcode; {@code null} means that no simple opcode fits
     * @return {@code null-ok;} a possibly-different opcode; either a
     * {@code non-null} good fit or {@code null} to indicate that no
     * simple opcode fits
     */
    private Dop findOpcodeForInsn(DalvInsn insn, Dop guess) {
        /*
         * Note: The initial guess might be null, meaning that an
         * earlier call to this method already determined that there
         * was no possible simple opcode fit.
         */

        while (guess != null) {
            if (guess.getFormat().isCompatible(insn)) {
                /*
                 * Don't break out for const_string to generate jumbo version
                 * when option is enabled.
                 */
                if (!dexOptions.forceJumbo ||
                    guess.getOpcode() != Opcodes.CONST_STRING) {
                    break;
                }
            }

            guess = Dops.getNextOrNull(guess, dexOptions);
        }

        return guess;
    }

    /**
     * Finds the proper opcode for the given instruction, ignoring
     * register constraints.
     *
     * @param insn {@code non-null;} the instruction in question
     * @return {@code non-null;} the opcode that fits
     */
    private Dop findExpandedOpcodeForInsn(DalvInsn insn) {
        Dop result = findOpcodeForInsn(insn.getLowRegVersion(), insn.getOpcode());
        if (result == null) {
            throw new DexException("No expanded opcode for " + insn);
        }
        return result;
    }

    /**
     * Helper for {@link #finishProcessingAndGetList}, which goes
     * through each instruction in the output, making sure its opcode
     * can accomodate its arguments. In cases where the opcode is
     * unable to do so, this replaces the instruction with a larger
     * instruction with identical semantics that <i>will</i> work.
     *
     * <p>This method may also reserve a number of low-numbered
     * registers, renumbering the instructions' original registers, in
     * order to have register space available in which to move
     * very-high registers when expanding instructions into
     * multi-instruction sequences. This expansion is done when no
     * simple instruction format can be found for a given instruction that
     * is able to accomodate that instruction's registers.</p>
     *
     * <p>This method ignores issues of branch target size, since
     * final addresses aren't known at the point that this method is
     * called.</p>
     *
     * @param opcodes {@code non-null;} array of per-instruction
     * opcode selections
     */
    private void massageInstructions(Dop[] opcodes) {
        if (reservedCount == 0) {
            /*
             * The easy common case: No registers were reserved, so we
             * merely need to replace any instructions whose format
             * (and hence whose opcode) changed during the reservation
             * pass, but all instructions will stay at their original
             * indices, and the instruction list doesn't grow.
             */
            int size = insns.size();

            for (int i = 0; i < size; i++) {
                DalvInsn insn = insns.get(i);
                Dop originalOpcode = insn.getOpcode();
                Dop currentOpcode = opcodes[i];

                if (originalOpcode != currentOpcode) {
                    insns.set(i, insn.withOpcode(currentOpcode));
                }
            }
        } else {
            /*
             * The difficult uncommon case: Some instructions have to be
             * expanded to deal with high registers.
             */
            insns = performExpansion(opcodes);
        }
    }

    /**
     * Helper for {@link #massageInstructions}, which constructs a
     * replacement list, where each {link DalvInsn} instance that
     * couldn't be represented simply (due to register representation
     * problems) is expanded into a series of instances that together
     * perform the proper function.
     *
     * @param opcodes {@code non-null;} array of per-instruction
     * opcode selections
     * @return {@code non-null;} the replacement list
     */
    private ArrayList<DalvInsn> performExpansion(Dop[] opcodes) {
        int size = insns.size();
        ArrayList<DalvInsn> result = new ArrayList<DalvInsn>(size * 2);

        ArrayList<CodeAddress> closelyBoundAddresses = new ArrayList<CodeAddress>();

        for (int i = 0; i < size; i++) {
            DalvInsn insn = insns.get(i);
            Dop originalOpcode = insn.getOpcode();
            Dop currentOpcode = opcodes[i];
            DalvInsn prefix;
            DalvInsn suffix;

            if (currentOpcode != null) {
                // No expansion is necessary.
                prefix = null;
                suffix = null;
            } else {
                // Expansion is required.
                currentOpcode = findExpandedOpcodeForInsn(insn);
                BitSet compatRegs =
                    currentOpcode.getFormat().compatibleRegs(insn);
                prefix = insn.expandedPrefix(compatRegs);
                suffix = insn.expandedSuffix(compatRegs);

                // Expand necessary registers to fit the new format
                insn = insn.expandedVersion(compatRegs);
            }

            if (insn instanceof CodeAddress) {
                // If we have a closely bound address, don't add it yet,
                // because we need to add it after the prefix for the
                // instruction it is bound to.
                if (((CodeAddress) insn).getBindsClosely()) {
                    closelyBoundAddresses.add((CodeAddress)insn);
                    continue;
                }
            }

            if (prefix != null) {
                result.add(prefix);
            }

            // Add any pending closely bound addresses
            if (!(insn instanceof ZeroSizeInsn) && closelyBoundAddresses.size() > 0) {
                for (CodeAddress codeAddress: closelyBoundAddresses) {
                    result.add(codeAddress);
                }
                closelyBoundAddresses.clear();
            }

            if (currentOpcode != originalOpcode) {
                insn = insn.withOpcode(currentOpcode);
            }
            result.add(insn);

            if (suffix != null) {
                result.add(suffix);
            }
        }

        return result;
    }

    /**
     * Helper for {@link #finishProcessingAndGetList}, which assigns
     * addresses to each instruction, possibly rewriting branches to
     * fix ones that wouldn't otherwise be able to reach their
     * targets.
     */
    private void assignAddressesAndFixBranches() {
        for (;;) {
            assignAddresses();
            if (!fixBranches()) {
                break;
            }
        }
    }

    /**
     * Helper for {@link #assignAddressesAndFixBranches}, which
     * assigns an address to each instruction, in order.
     */
    private void assignAddresses() {
        int address = 0;
        int size = insns.size();

        for (int i = 0; i < size; i++) {
            DalvInsn insn = insns.get(i);
            insn.setAddress(address);
            address += insn.codeSize();
        }
    }

    /**
     * Helper for {@link #assignAddressesAndFixBranches}, which checks
     * the branch target size requirement of each branch instruction
     * to make sure it fits. For instructions that don't fit, this
     * rewrites them to use a {@code goto} of some sort. In the
     * case of a conditional branch that doesn't fit, the sense of the
     * test is reversed in order to branch around a {@code goto}
     * to the original target.
     *
     * @return whether any branches had to be fixed
     */
    private boolean fixBranches() {
        int size = insns.size();
        boolean anyFixed = false;

        for (int i = 0; i < size; i++) {
            DalvInsn insn = insns.get(i);
            if (!(insn instanceof TargetInsn)) {
                // This loop only needs to inspect TargetInsns.
                continue;
            }

            Dop opcode = insn.getOpcode();
            TargetInsn target = (TargetInsn) insn;

            if (opcode.getFormat().branchFits(target)) {
                continue;
            }

            if (opcode.getFamily() == Opcodes.GOTO) {
                // It is a goto; widen it if possible.
                opcode = findOpcodeForInsn(insn, opcode);
                if (opcode == null) {
                    /*
                     * The branch is already maximally large. This should
                     * only be possible if a method somehow manages to have
                     * more than 2^31 code units.
                     */
                    throw new UnsupportedOperationException("method too long");
                }
                insns.set(i, insn.withOpcode(opcode));
            } else {
                /*
                 * It is a conditional: Reverse its sense, and arrange for
                 * it to branch around an absolute goto to the original
                 * branch target.
                 *
                 * Note: An invariant of the list being processed is
                 * that every TargetInsn is followed by a CodeAddress.
                 * Hence, it is always safe to get the next element
                 * after a TargetInsn and cast it to CodeAddress, as
                 * is happening a few lines down.
                 *
                 * Also note: Size gets incremented by one here, as we
                 * have -- in the net -- added one additional element
                 * to the list, so we increment i to match. The added
                 * and changed elements will be inspected by a repeat
                 * call to this method after this invocation returns.
                 */
                CodeAddress newTarget;
                try {
                    newTarget = (CodeAddress) insns.get(i + 1);
                } catch (IndexOutOfBoundsException ex) {
                    // The TargetInsn / CodeAddress invariant was violated.
                    throw new IllegalStateException(
                            "unpaired TargetInsn (dangling)");
                } catch (ClassCastException ex) {
                    // The TargetInsn / CodeAddress invariant was violated.
                    throw new IllegalStateException("unpaired TargetInsn");
                }
                TargetInsn gotoInsn =
                    new TargetInsn(Dops.GOTO, target.getPosition(),
                            RegisterSpecList.EMPTY, target.getTarget());
                insns.set(i, gotoInsn);
                insns.add(i, target.withNewTargetAndReversed(newTarget));
                size++;
                i++;
            }

            anyFixed = true;
        }

        return anyFixed;
    }

    private void align64bits(Dop[] opcodes) {
      while (true) {
        int notAligned64bitRegAccess = 0;
        int aligned64bitRegAccess = 0;
        int notAligned64bitParamAccess = 0;
        int aligned64bitParamAccess = 0;
        int lastParameter = unreservedRegCount + reservedCount + reservedParameterCount;
        int firstParameter = lastParameter - paramSize;

        // Collects the number of time that 64-bit registers are accessed aligned or not.
        for (DalvInsn insn : insns) {
          RegisterSpecList regs = insn.getRegisters();
          for (int usedRegIdx = 0; usedRegIdx < regs.size(); usedRegIdx++) {
            RegisterSpec reg = regs.get(usedRegIdx);
            if (reg.isCategory2()) {
              boolean isParameter = reg.getReg() >= firstParameter;
              if (reg.isEvenRegister()) {
                if (isParameter) {
                  aligned64bitParamAccess++;
                } else {
                  aligned64bitRegAccess++;
                }
              } else {
                if (isParameter) {
                  notAligned64bitParamAccess++;
                } else {
                  notAligned64bitRegAccess++;
                }
              }
            }
          }
        }

        if (notAligned64bitParamAccess > aligned64bitParamAccess
            && notAligned64bitRegAccess > aligned64bitRegAccess) {
          addReservedRegisters(1);
        } else if (notAligned64bitParamAccess > aligned64bitParamAccess) {
          addReservedParameters(1);
        } else if (notAligned64bitRegAccess > aligned64bitRegAccess) {
          addReservedRegisters(1);

          // Need to shift parameters if they exist and if number of unaligned is greater than
          // aligned. We test the opposite because we previously shift all registers by one,
          // so the number of aligned become the number of unaligned.
          if (paramSize != 0 && aligned64bitParamAccess > notAligned64bitParamAccess) {
            addReservedParameters(1);
          }
        } else {
          break;
        }

        if (!reserveRegisters(opcodes)) {
          break;
        }
      }
    }

    private void addReservedParameters(int delta) {
      shiftParameters(delta);
      reservedParameterCount += delta;
    }

    private void addReservedRegisters(int delta) {
      shiftAllRegisters(delta);
      reservedCount += delta;
    }

    private void shiftAllRegisters(int delta) {
      int insnSize = insns.size();

      for (int i = 0; i < insnSize; i++) {
        DalvInsn insn = insns.get(i);
        // Since there is no need to replace CodeAddress since it does not use registers, skips it to
        // avoid to update all TargetInsn that contain a reference to CodeAddress
        if (!(insn instanceof CodeAddress)) {
          insns.set(i, insn.withRegisterOffset(delta));
        }
      }
    }

    private void shiftParameters(int delta) {
      int insnSize = insns.size();
      int lastParameter = unreservedRegCount + reservedCount + reservedParameterCount;
      int firstParameter = lastParameter - paramSize;

      BasicRegisterMapper mapper = new BasicRegisterMapper(lastParameter);
      for (int i = 0; i < lastParameter; i++) {
        if (i >= firstParameter) {
          mapper.addMapping(i, i + delta, 1);
        } else {
          mapper.addMapping(i, i, 1);
        }
      }

      for (int i = 0; i < insnSize; i++) {
        DalvInsn insn = insns.get(i);
        // Since there is no need to replace CodeAddress since it does not use registers, skips it to
        // avoid to update all TargetInsn that contain a reference to CodeAddress
        if (!(insn instanceof CodeAddress)) {
          insns.set(i, insn.withMapper(mapper));
        }
      }
    }
}
