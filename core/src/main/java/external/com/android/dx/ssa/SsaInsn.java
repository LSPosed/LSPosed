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

import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.util.ToHuman;

/**
 * An instruction in SSA form
 */
public abstract class SsaInsn implements ToHuman, Cloneable {
    /** {@code non-null;} the block that contains this instance */
    private final SsaBasicBlock block;

    /** {@code null-ok;} result register */
    private RegisterSpec result;

    /**
     * Constructs an instance.
     *
     * @param result {@code null-ok;} initial result register. May be changed.
     * @param block {@code non-null;} block containing this insn. Can
     * never change.
     */
    protected SsaInsn(RegisterSpec result, SsaBasicBlock block) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }

        this.block = block;
        this.result = result;
    }

    /**
     * Makes a new SSA insn form a rop insn.
     *
     * @param insn {@code non-null;} rop insn
     * @param block {@code non-null;} owning block
     * @return {@code non-null;} an appropriately constructed instance
     */
    public static SsaInsn makeFromRop(Insn insn, SsaBasicBlock block) {
        return new NormalSsaInsn(insn, block);
    }

    /** {@inheritDoc} */
    @Override
    public SsaInsn clone() {
        try {
            return (SsaInsn)super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException ("unexpected", ex);
        }
    }

    /**
     * Like {@link external.com.android.dx.rop.code.Insn getResult()}.
     *
     * @return result register
     */
    public RegisterSpec getResult() {
        return result;
    }

    /**
     * Set the result register.
     *
     * @param result {@code non-null;} the new result register
     */
    protected void setResult(RegisterSpec result) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }

        this.result = result;
    }

    /**
     * Like {@link external.com.android.dx.rop.code.Insn getSources()}.
     *
     * @return {@code non-null;} sources list
     */
    abstract public RegisterSpecList getSources();

    /**
     * Gets the block to which this insn instance belongs.
     *
     * @return owning block
     */
    public SsaBasicBlock getBlock() {
        return block;
    }

    /**
     * Returns whether or not the specified reg is the result reg.
     *
     * @param reg register to test
     * @return true if there is a result and it is stored in the specified
     * register
     */
    public boolean isResultReg(int reg) {
        return result != null && result.getReg() == reg;
    }


    /**
     * Changes the result register if this insn has a result. This is used
     * during renaming.
     *
     * @param reg new result register
     */
    public void changeResultReg(int reg) {
        if (result != null) {
            result = result.withReg(reg);
        }
    }

    /**
     * Sets the local association for the result of this insn. This is
     * sometimes updated during the SsaRenamer process.
     *
     * @param local {@code null-ok;} new debug/local variable info
     */
    public final void setResultLocal(LocalItem local) {
        LocalItem oldItem = result.getLocalItem();

        if (local != oldItem && (local == null
                || !local.equals(result.getLocalItem()))) {
            result = RegisterSpec.makeLocalOptional(
                    result.getReg(), result.getType(), local);
        }
    }

    /**
     * Map registers after register allocation.
     *
     * @param mapper {@code non-null;} mapping from old to new registers
     */
    public final void mapRegisters(RegisterMapper mapper) {
        RegisterSpec oldResult = result;

        result = mapper.map(result);
        block.getParent().updateOneDefinition(this, oldResult);
        mapSourceRegisters(mapper);
    }

    /**
     * Maps only source registers.
     *
     * @param mapper new mapping
     */
    abstract public void mapSourceRegisters(RegisterMapper mapper);

    /**
     * Returns the Rop opcode for this insn, or null if this is a phi insn.
     *
     * TODO: Move this up into NormalSsaInsn.
     *
     * @return {@code null-ok;} Rop opcode if there is one.
     */
    abstract public Rop getOpcode();

    /**
     * Returns the original Rop insn for this insn, or null if this is
     * a phi insn.
     *
     * TODO: Move this up into NormalSsaInsn.
     *
     * @return {@code null-ok;} Rop insn if there is one.
     */
    abstract public Insn getOriginalRopInsn();

    /**
     * Gets the spec of a local variable assignment that occurs at this
     * instruction, or null if no local variable assignment occurs. This
     * may be the result register, or for {@code mark-local} insns
     * it may be the source.
     *
     * @see external.com.android.dx.rop.code.Insn#getLocalAssignment()
     *
     * @return {@code null-ok;} a local-associated register spec or null
     */
    public RegisterSpec getLocalAssignment() {
        if (result != null && result.getLocalItem() != null) {
            return result;
        }

        return null;
    }

    /**
     * Indicates whether the specified register is amongst the registers
     * used as sources for this instruction.
     *
     * @param reg the register in question
     * @return true if the reg is a source
     */
    public boolean isRegASource(int reg) {
        return null != getSources().specForRegister(reg);
    }

    /**
     * Transform back to ROP form.
     *
     * TODO: Move this up into NormalSsaInsn.
     *
     * @return {@code non-null;} a ROP representation of this instruction, with
     * updated registers.
     */
    public abstract Insn toRopInsn();

    /**
     * @return true if this is a PhiInsn or a normal move insn
     */
    public abstract boolean isPhiOrMove();

    /**
     * Returns true if this insn is considered to have a side effect beyond
     * that of assigning to the result reg.
     *
     * @return true if this insn is considered to have a side effect beyond
     * that of assigning to the result reg.
     */
    public abstract boolean hasSideEffect();

    /**
     * @return true if this is a move (but not a move-operand or
     * move-exception) instruction
     */
    public boolean isNormalMoveInsn() {
        return false;
    }

    /**
     * @return true if this is a move-exception instruction.
     * These instructions must immediately follow a preceeding invoke*
     */
    public boolean isMoveException() {
        return false;
    }

    /**
     * @return true if this instruction can throw.
     */
    abstract public boolean canThrow();

    /**
     * Accepts a visitor.
     *
     * @param v {@code non-null} the visitor
     */
    public abstract void accept(Visitor v);

    /**
     * Visitor interface for this class.
     */
    public static interface Visitor {
        /**
         * Any non-phi move instruction
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitMoveInsn(NormalSsaInsn insn);

        /**
         * Any phi insn
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitPhiInsn(PhiInsn insn);

        /**
         * Any insn that isn't a move or a phi (which is also a move).
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitNonMoveInsn(NormalSsaInsn insn);
    }
}
