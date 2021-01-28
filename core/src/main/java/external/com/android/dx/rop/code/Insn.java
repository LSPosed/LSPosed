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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.ToHuman;

/**
 * A register-based instruction. An instruction is the combination of
 * an opcode (which specifies operation and source/result types), a
 * list of actual sources and result registers/values, and additional
 * information.
 */
public abstract class Insn implements ToHuman {
    /** {@code non-null;} opcode */
    private final Rop opcode;

    /** {@code non-null;} source position */
    private final SourcePosition position;

    /** {@code null-ok;} spec for the result of this instruction, if any */
    private final RegisterSpec result;

    /** {@code non-null;} specs for all the sources of this instruction */
    private final RegisterSpecList sources;

    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param result {@code null-ok;} spec for the result, if any
     * @param sources {@code non-null;} specs for all the sources
     */
    public Insn(Rop opcode, SourcePosition position, RegisterSpec result,
                RegisterSpecList sources) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        }

        if (position == null) {
            throw new NullPointerException("position == null");
        }

        if (sources == null) {
            throw new NullPointerException("sources == null");
        }

        this.opcode = opcode;
        this.position = position;
        this.result = result;
        this.sources = sources;
    }

    /**
     * {@inheritDoc}
     *
     * Instances of this class compare by identity. That is,
     * {@code x.equals(y)} is only true if {@code x == y}.
     */
    @Override
    public final boolean equals(Object other) {
        return (this == other);
    }

    /**
     * {@inheritDoc}
     *
     * This implementation returns the identity hashcode of this
     * instance. This is proper, since instances of this class compare
     * by identity (see {@link #equals}).
     */
    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toStringWithInline(getInlineString());
    }

    /**
     * Gets a human-oriented (and slightly lossy) string for this instance.
     *
     * @return {@code non-null;} the human string form
     */
    @Override
    public String toHuman() {
        return toHumanWithInline(getInlineString());
    }

    /**
     * Gets an "inline" string portion for toHuman(), if available. This
     * is the portion that appears after the Rop opcode
     *
     * @return {@code null-ok;} if non-null, the inline text for toHuman()
     */
    public String getInlineString() {
        return null;
    }

    /**
     * Gets the opcode.
     *
     * @return {@code non-null;} the opcode
     */
    public final Rop getOpcode() {
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
     * Gets the result spec, if any. A return value of {@code null}
     * means this instruction returns nothing.
     *
     * @return {@code null-ok;} the result spec, if any
     */
    public final RegisterSpec getResult() {
        return result;
    }

    /**
     * Gets the spec of a local variable assignment that occurs at this
     * instruction, or null if no local variable assignment occurs. This
     * may be the result register, or for {@code mark-local} insns
     * it may be the source.
     *
     * @return {@code null-ok;} a named register spec or null
     */
    public final RegisterSpec getLocalAssignment() {
        RegisterSpec assignment;
        if (opcode.getOpcode() == RegOps.MARK_LOCAL) {
            assignment = sources.get(0);
        } else {
            assignment = result;
        }

        if (assignment == null) {
            return null;
        }

        LocalItem localItem = assignment.getLocalItem();

        if (localItem == null) {
            return null;
        }

        return assignment;
    }

    /**
     * Gets the source specs.
     *
     * @return {@code non-null;} the source specs
     */
    public final RegisterSpecList getSources() {
        return sources;
    }

    /**
     * Gets whether this instruction can possibly throw an exception. This
     * is just a convenient wrapper for {@code getOpcode().canThrow()}.
     *
     * @return {@code true} iff this instruction can possibly throw
     */
    public final boolean canThrow() {
        return opcode.canThrow();
    }

    /**
     * Gets the list of possibly-caught exceptions. This returns {@link
     * StdTypeList#EMPTY} if this instruction has no handlers,
     * which can be <i>either</i> if this instruction can't possibly
     * throw or if it merely doesn't handle any of its possible
     * exceptions. To determine whether this instruction can throw,
     * use {@link #canThrow}.
     *
     * @return {@code non-null;} the catches list
     */
    public abstract TypeList getCatches();

    /**
     * Calls the appropriate method on the given visitor, depending on the
     * class of this instance. Subclasses must override this.
     *
     * @param visitor {@code non-null;} the visitor to call on
     */
    public abstract void accept(Visitor visitor);

    /**
     * Returns an instance that is just like this one, except that it
     * has a catch list with the given item appended to the end. This
     * method throws an exception if this instance can't possibly
     * throw. To determine whether this instruction can throw, use
     * {@link #canThrow}.
     *
     * @param type {@code non-null;} type to append to the catch list
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract Insn withAddedCatch(Type type);

    /**
     * Returns an instance that is just like this one, except that all
     * register references have been offset by the given delta.
     *
     * @param delta the amount to offset register references by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract Insn withRegisterOffset(int delta);

    /**
     * Returns an instance that is just like this one, except that, if
     * possible, the insn is converted into a version in which a source
     * (if it is a constant) is represented directly rather than as a
     * register reference. {@code this} is returned in cases where the
     * translation is not possible.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public Insn withSourceLiteral() {
        return this;
    }

    /**
     * Returns an exact copy of this Insn
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public Insn copy() {
        return withRegisterOffset(0);
    }


    /**
     * Compares, handling nulls safely
     *
     * @param a first object
     * @param b second object
     * @return true if they're equal or both null.
     */
    private static boolean equalsHandleNulls (Object a, Object b) {
        return (a == b) || ((a != null) && a.equals(b));
    }

    /**
     * Compares Insn contents, since {@code Insn.equals()} is defined
     * to be an identity compare. Insn's are {@code contentEquals()}
     * if they have the same opcode, registers, source position, and other
     * metadata.
     *
     * @return true in the case described above
     */
    public boolean contentEquals(Insn b) {
        return opcode == b.getOpcode()
                && position.equals(b.getPosition())
                && (getClass() == b.getClass())
                && equalsHandleNulls(result, b.getResult())
                && equalsHandleNulls(sources, b.getSources())
                && StdTypeList.equalContents(getCatches(), b.getCatches());
    }

    /**
     * Returns an instance that is just like this one, except
     * with new result and source registers.
     *
     * @param result {@code null-ok;} new result register
     * @param sources {@code non-null;} new sources registers
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public abstract Insn withNewRegisters(RegisterSpec result,
            RegisterSpecList sources);

    /**
     * Returns the string form of this instance, with the given bit added in
     * the standard location for an inline argument.
     *
     * @param extra {@code null-ok;} the inline argument string
     * @return {@code non-null;} the string form
     */
    protected final String toStringWithInline(String extra) {
        StringBuilder sb = new StringBuilder(80);

        sb.append("Insn{");
        sb.append(position);
        sb.append(' ');
        sb.append(opcode);

        if (extra != null) {
            sb.append(' ');
            sb.append(extra);
        }

        sb.append(" :: ");

        if (result != null) {
            sb.append(result);
            sb.append(" <- ");
        }

        sb.append(sources);
        sb.append('}');

        return sb.toString();
    }

    /**
     * Returns the human string form of this instance, with the given
     * bit added in the standard location for an inline argument.
     *
     * @param extra {@code null-ok;} the inline argument string
     * @return {@code non-null;} the human string form
     */
    protected final String toHumanWithInline(String extra) {
        StringBuilder sb = new StringBuilder(80);

        sb.append(position);
        sb.append(": ");
        sb.append(opcode.getNickname());

        if (extra != null) {
            sb.append("(");
            sb.append(extra);
            sb.append(")");
        }

        if (result == null) {
            sb.append(" .");
        } else {
            sb.append(" ");
            sb.append(result.toHuman());
        }

        sb.append(" <-");

        int sz = sources.size();
        if (sz == 0) {
            sb.append(" .");
        } else {
            for (int i = 0; i < sz; i++) {
                sb.append(" ");
                sb.append(sources.get(i).toHuman());
            }
        }

        return sb.toString();
    }


    /**
     * Visitor interface for this (outer) class.
     */
    public static interface Visitor {
        /**
         * Visits a {@link PlainInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitPlainInsn(PlainInsn insn);

        /**
         * Visits a {@link PlainCstInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitPlainCstInsn(PlainCstInsn insn);

        /**
         * Visits a {@link SwitchInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitSwitchInsn(SwitchInsn insn);

        /**
         * Visits a {@link ThrowingCstInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitThrowingCstInsn(ThrowingCstInsn insn);

        /**
         * Visits a {@link ThrowingInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitThrowingInsn(ThrowingInsn insn);

        /**
         * Visits a {@link FillArrayDataInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitFillArrayDataInsn(FillArrayDataInsn insn);

        /**
         * Visits a {@link InvokePolymorphicInsn}.
         *
         * @param insn {@code non-null;} the instruction to visit
         */
        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn);
    }

    /**
     * Base implementation of {@link Visitor}, which has empty method
     * bodies for all methods.
     */
    public static class BaseVisitor implements Visitor {
        /** {@inheritDoc} */
        @Override
        public void visitPlainInsn(PlainInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainCstInsn(PlainCstInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitSwitchInsn(SwitchInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingInsn(ThrowingInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
            // This space intentionally left blank.
        }

        /** {@inheritDoc} */
        @Override
        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
            // This space intentionally left blank.
        }
    }
}
