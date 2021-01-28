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

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.ssa.RegisterMapper;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.TwoColumnOutput;
import java.util.BitSet;

/**
 * Base class for Dalvik instructions.
 */
public abstract class DalvInsn {
    /**
     * the actual output address of this instance, if known, or
     * {@code -1} if not
     */
    private int address;

    /** the opcode; one of the constants from {@link Dops} */
    private final Dop opcode;

    /** {@code non-null;} source position */
    private final SourcePosition position;

    /** {@code non-null;} list of register arguments */
    private final RegisterSpecList registers;

    /**
     * Makes a move instruction, appropriate and ideal for the given arguments.
     *
     * @param position {@code non-null;} source position information
     * @param dest {@code non-null;} destination register
     * @param src {@code non-null;} source register
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static SimpleInsn makeMove(SourcePosition position,
            RegisterSpec dest, RegisterSpec src) {
        boolean category1 = dest.getCategory() == 1;
        boolean reference = dest.getType().isReference();
        int destReg = dest.getReg();
        int srcReg = src.getReg();
        Dop opcode;

        if ((srcReg | destReg) < 16) {
            opcode = reference ? Dops.MOVE_OBJECT :
                (category1 ? Dops.MOVE : Dops.MOVE_WIDE);
        } else if (destReg < 256) {
            opcode = reference ? Dops.MOVE_OBJECT_FROM16 :
                (category1 ? Dops.MOVE_FROM16 : Dops.MOVE_WIDE_FROM16);
        } else {
            opcode = reference ? Dops.MOVE_OBJECT_16 :
                (category1 ? Dops.MOVE_16 : Dops.MOVE_WIDE_16);
        }

        return new SimpleInsn(opcode, position,
                              RegisterSpecList.make(dest, src));
    }

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * <p><b>Note:</b> In the unlikely event that an instruction takes
     * absolutely no registers (e.g., a {@code nop} or a
     * no-argument no-result static method call), then the given
     * register list may be passed as {@link
     * RegisterSpecList#EMPTY}.</p>
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins and outs)
     */
    public DalvInsn(Dop opcode, SourcePosition position,
                    RegisterSpecList registers) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        }

        if (position == null) {
            throw new NullPointerException("position == null");
        }

        if (registers == null) {
            throw new NullPointerException("registers == null");
        }

        this.address = -1;
        this.opcode = opcode;
        this.position = position;
        this.registers = registers;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(100);

        sb.append(identifierString());
        sb.append(' ');
        sb.append(position);

        sb.append(": ");
        sb.append(opcode.getName());

        boolean needComma = false;
        if (registers.size() != 0) {
            sb.append(registers.toHuman(" ", ", ", null));
            needComma = true;
        }

        String extra = argString();
        if (extra != null) {
            if (needComma) {
                sb.append(',');
            }
            sb.append(' ');
            sb.append(extra);
        }

        return sb.toString();
    }

    /**
     * Gets whether the address of this instruction is known.
     *
     * @see #getAddress
     * @see #setAddress
     */
    public final boolean hasAddress() {
        return (address >= 0);
    }

    /**
     * Gets the output address of this instruction, if it is known. This throws
     * a {@code RuntimeException} if it has not yet been set.
     *
     * @see #setAddress
     *
     * @return {@code >= 0;} the output address
     */
    public final int getAddress() {
        if (address < 0) {
            throw new RuntimeException("address not yet known");
        }

        return address;
    }

    /**
     * Gets the opcode.
     *
     * @return {@code non-null;} the opcode
     */
    public final Dop getOpcode() {
        return opcode;
    }

    /**
     * Gets the source position.
     *
     * @return {@code non-null;} the source position
     */
    public final SourcePosition getPosition() {
        return position;
    }

    /**
     * Gets the register list for this instruction.
     *
     * @return {@code non-null;} the registers
     */
    public final RegisterSpecList getRegisters() {
        return registers;
    }

    /**
     * Returns whether this instance's opcode uses a result register.
     * This method is a convenient shorthand for
     * {@code getOpcode().hasResult()}.
     *
     * @return {@code true} iff this opcode uses a result register
     */
    public final boolean hasResult() {
        return opcode.hasResult();
    }

    /**
     * Gets the minimum distinct registers required for this instruction.
     * Uses the given BitSet to determine which registers require
     * replacement, and ignores registers that are already compatible.
     * This assumes that the result (if any) can share registers with the
     * sources (if any), that each source register is unique, and that
     * (to be explicit here) category-2 values take up two consecutive
     * registers.
     *
     * @param compatRegs {@code non-null;} set of compatible registers
     * @return {@code >= 0;} the minimum distinct register requirement
     */
    public final int getMinimumRegisterRequirement(BitSet compatRegs) {
        boolean hasResult = hasResult();
        int regSz = registers.size();
        int resultRequirement = 0;
        int sourceRequirement = 0;

        if (hasResult && !compatRegs.get(0)) {
            resultRequirement = registers.get(0).getCategory();
        }

        for (int i = hasResult ? 1 : 0; i < regSz; i++) {
            if (!compatRegs.get(i)) {
                sourceRequirement += registers.get(i).getCategory();
            }
        }

        return Math.max(sourceRequirement, resultRequirement);
    }

    /**
     * Gets the instruction that is equivalent to this one, except that
     * it uses sequential registers starting at {@code 0} (storing
     * the result, if any, in register {@code 0} as well).
     *
     * @return {@code non-null;} the replacement
     */
    public DalvInsn getLowRegVersion() {
        RegisterSpecList regs =
            registers.withExpandedRegisters(0, hasResult(), null);
        return withRegisters(regs);
    }

    /**
     * Gets the instruction prefix required, if any, to use in an expanded
     * version of this instance. Will not generate moves for registers
     * marked compatible to the format by the given BitSet.
     *
     * @see #expandedVersion
     *
     * @param compatRegs {@code non-null;} set of compatible registers
     * @return {@code null-ok;} the prefix, if any
     */
    public DalvInsn expandedPrefix(BitSet compatRegs) {
        RegisterSpecList regs = registers;
        boolean firstBit = compatRegs.get(0);

        if (hasResult()) compatRegs.set(0);

        regs = regs.subset(compatRegs);

        if (hasResult()) compatRegs.set(0, firstBit);

        if (regs.size() == 0) return null;

        return new HighRegisterPrefix(position, regs);
    }

    /**
     * Gets the instruction suffix required, if any, to use in an expanded
     * version of this instance. Will not generate a move for a register
     * marked compatible to the format by the given BitSet.
     *
     * @see #expandedVersion
     *
     * @param compatRegs {@code non-null;} set of compatible registers
     * @return {@code null-ok;} the suffix, if any
     */
    public DalvInsn expandedSuffix(BitSet compatRegs) {
        if (hasResult() && !compatRegs.get(0)) {
            RegisterSpec r = registers.get(0);
            return makeMove(position, r, r.withReg(0));
        } else {
            return null;
        }
    }

    /**
     * Gets the instruction that is equivalent to this one, except that
     * it replaces incompatible registers with sequential registers
     * starting at {@code 0} (storing the result, if any, in register
     * {@code 0} as well). The sequence of instructions from
     * {@link #expandedPrefix} and {@link #expandedSuffix} (if non-null)
     * surrounding the result of a call to this method are the expanded
     * transformation of this instance, and it is guaranteed that the
     * number of low registers used will be the number returned by
     * {@link #getMinimumRegisterRequirement}.
     *
     * @param compatRegs {@code non-null;} set of compatible registers
     * @return {@code non-null;} the replacement
     */
    public DalvInsn expandedVersion(BitSet compatRegs) {
        RegisterSpecList regs =
            registers.withExpandedRegisters(0, hasResult(), compatRegs);
        return withRegisters(regs);
    }

    /**
     * Gets the short identifier for this instruction. This is its
     * address, if assigned, or its identity hashcode if not.
     *
     * @return {@code non-null;} the identifier
     */
    public final String identifierString() {
        if (address != -1) {
            return String.format("%04x", address);
        }

        return Hex.u4(System.identityHashCode(this));
    }

    /**
     * Returns the string form of this instance suitable for inclusion in
     * a human-oriented listing dump. This method will return {@code null}
     * if this instance should not appear in a listing.
     *
     * @param prefix {@code non-null;} prefix before the address; each follow-on
     * line will be indented to match as well
     * @param width {@code width >= 0;} the width of the output or {@code 0} for
     * unlimited width
     * @param noteIndices whether to include an explicit notation of
     * constant pool indices
     * @return {@code null-ok;} the string form or {@code null} if this
     * instance should not appear in a listing
     */
    public final String listingString(String prefix, int width,
            boolean noteIndices) {
        String insnPerSe = listingString0(noteIndices);

        if (insnPerSe == null) {
            return null;
        }

        String addr = prefix + identifierString() + ": ";
        int w1 = addr.length();
        int w2 = (width == 0) ? insnPerSe.length() : (width - w1);

        return TwoColumnOutput.toString(addr, w1, "", insnPerSe, w2);
    }

    /**
     * Sets the output address.
     *
     * @param address {@code address >= 0;} the output address
     */
    public final void setAddress(int address) {
        if (address < 0) {
            throw new IllegalArgumentException("address < 0");
        }

        this.address = address;
    }

    /**
     * Gets the address immediately after this instance. This is only
     * calculable if this instance's address is known, and it is equal
     * to the address plus the length of the instruction format of this
     * instance's opcode.
     *
     * @return {@code >= 0;} the next address
     */
    public final int getNextAddress() {
        return getAddress() + codeSize();
    }

    /**
     * Returns an instance that is just like this one, except that the
     * register list is mapped by using {@code mapper}.
     *
     * @param mapper {@code non-null;} used to map registers
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public DalvInsn withMapper(RegisterMapper mapper) {
      return withRegisters(mapper.map(getRegisters()));
    }

    /**
     * Gets the size of this instruction, in 16-bit code units.
     *
     * @return {@code >= 0;} the code size of this instruction
     */
    public abstract int codeSize();

    /**
     * Writes this instance to the given output. This method should
     * never annotate the output.
     *
     * @param out {@code non-null;} where to write to
     */
    public abstract void writeTo(AnnotatedOutput out);

    /**
     * Returns an instance that is just like this one, except that its
     * opcode is replaced by the one given, and its address is reset.
     *
     * @param opcode {@code non-null;} the new opcode
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract DalvInsn withOpcode(Dop opcode);

    /**
     * Returns an instance that is just like this one, except that all
     * register references have been offset by the given delta, and its
     * address is reset.
     *
     * @param delta the amount to offset register references by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract DalvInsn withRegisterOffset(int delta);

    /**
     * Returns an instance that is just like this one, except that the
     * register list is replaced by the given one, and its address is
     * reset.
     *
     * @param registers {@code non-null;} new register list
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract DalvInsn withRegisters(RegisterSpecList registers);

    /**
     * Gets the string form for any arguments to this instance. Subclasses
     * must override this.
     *
     * @return {@code null-ok;} the string version of any arguments or
     * {@code null} if there are none
     */
    protected abstract String argString();

    /**
     * Helper for {@link #listingString}, which returns the string
     * form of this instance suitable for inclusion in a
     * human-oriented listing dump, not including the instruction
     * address and without respect for any output formatting. This
     * method should return {@code null} if this instance should
     * not appear in a listing.
     *
     * @param noteIndices whether to include an explicit notation of
     * constant pool indices
     * @return {@code null-ok;} the listing string
     */
    protected abstract String listingString0(boolean noteIndices);

    /**
     * Helper which returns the string form of the associated constants
     * for inclusion in a human oriented listing dump.
     *
     * This method is only implemented for instructions with one or more
     * constants.
     *
     * @return the constant as a string.
     */
    public String cstString() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Helper which returns the comment form of the associated constants
     * for inclusion in a human oriented listing dump.
     *
     * This method is only implemented for instructions with one or more
     * constants.
     *
     * @return the comment as a string.
     */
    public String cstComment() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
