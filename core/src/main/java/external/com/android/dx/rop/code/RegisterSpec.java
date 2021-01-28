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

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.ToHuman;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combination of a register number and a type, used as the sources and
 * destinations of register-based operations.
 */
public final class RegisterSpec
        implements TypeBearer, ToHuman, Comparable<RegisterSpec> {
    /** {@code non-null;} string to prefix register numbers with */
    public static final String PREFIX = "v";

    /** {@code non-null;} intern table for instances */
    private static final ConcurrentHashMap<Object, RegisterSpec> theInterns =
        new ConcurrentHashMap<Object, RegisterSpec>(10_000, 0.75f);

    /** {@code non-null;} common comparison instance used while interning */
    private static final ThreadLocal<ForComparison> theInterningItem =
            new ThreadLocal<ForComparison>() {
                @Override
                protected ForComparison initialValue() {
                    return new ForComparison();
                }
            };

    /** {@code >= 0;} register number */
    private final int reg;

    /** {@code non-null;} type loaded or stored */
    private final TypeBearer type;

    /**
     * {@code null-ok;} local variable info associated with this register,
     * if any
     */
    private final LocalItem local;

    /**
     * Intern the given triple as an instance of this class.
     *
     * @param reg {@code >= 0;} the register number
     * @param type {@code non-null;} the type (or possibly actual value) which
     * is loaded from or stored to the indicated register
     * @param local {@code null-ok;} the associated local variable, if any
     * @return {@code non-null;} an appropriately-constructed instance
     */
    private static RegisterSpec intern(int reg, TypeBearer type,
            LocalItem local) {
        ForComparison interningItem = theInterningItem.get();
        interningItem.set(reg, type, local);
        RegisterSpec found = theInterns.get(interningItem);
        if (found == null) {
            found = interningItem.toRegisterSpec();
            RegisterSpec existing = theInterns.putIfAbsent(found, found);
            if (existing != null) {
                return existing;
            }
        }
        return found;
    }

    /**
     * Returns an instance for the given register number and type, with
     * no variable info. This method is allowed to return shared
     * instances (but doesn't necessarily do so).
     *
     * @param reg {@code >= 0;} the register number
     * @param type {@code non-null;} the type (or possibly actual value) which
     * is loaded from or stored to the indicated register
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpec make(int reg, TypeBearer type) {
        return intern(reg, type, null);
    }

    /**
     * Returns an instance for the given register number, type, and
     * variable info. This method is allowed to return shared
     * instances (but doesn't necessarily do so).
     *
     * @param reg {@code >= 0;} the register number
     * @param type {@code non-null;} the type (or possibly actual value) which
     * is loaded from or stored to the indicated register
     * @param local {@code non-null;} the associated local variable
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpec make(int reg, TypeBearer type,
            LocalItem local) {
        if (local == null) {
            throw new NullPointerException("local  == null");
        }

        return intern(reg, type, local);
    }

    /**
     * Returns an instance for the given register number, type, and
     * variable info. This method is allowed to return shared
     * instances (but doesn't necessarily do so).
     *
     * @param reg {@code >= 0;} the register number
     * @param type {@code non-null;} the type (or possibly actual value) which
     * is loaded from or stored to the indicated register
     * @param local {@code null-ok;} the associated variable info or null for
     * none
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpec makeLocalOptional(
            int reg, TypeBearer type, LocalItem local) {

        return intern(reg, type, local);
    }

    /**
     * Gets the string form for the given register number.
     *
     * @param reg {@code >= 0;} the register number
     * @return {@code non-null;} the string form
     */
    public static String regString(int reg) {
        return PREFIX + reg;
    }

    /**
     * Constructs an instance. This constructor is private. Use
     * {@link #make}.
     *
     * @param reg {@code >= 0;} the register number
     * @param type {@code non-null;} the type (or possibly actual value) which
     * is loaded from or stored to the indicated register
     * @param local {@code null-ok;} the associated local variable, if any
     */
    private RegisterSpec(int reg, TypeBearer type, LocalItem local) {
        if (reg < 0) {
            throw new IllegalArgumentException("reg < 0");
        }

        if (type == null) {
            throw new NullPointerException("type == null");
        }

        this.reg = reg;
        this.type = type;
        this.local = local;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RegisterSpec)) {
            if (other instanceof ForComparison) {
                ForComparison fc = (ForComparison) other;
                return equals(fc.reg, fc.type, fc.local);
            }
            return false;
        }

        RegisterSpec spec = (RegisterSpec) other;
        return equals(spec.reg, spec.type, spec.local);
    }

    /**
     * Like {@code equals}, but only consider the simple types of the
     * registers. That is, this compares {@code getType()} on the types
     * to ignore whatever arbitrary extra stuff might be carried around
     * by an outer {@link TypeBearer}.
     *
     * @param other {@code null-ok;} spec to compare to
     * @return {@code true} iff {@code this} and {@code other} are equal
     * in the stated way
     */
    public boolean equalsUsingSimpleType(RegisterSpec other) {
        if (!matchesVariable(other)) {
            return false;
        }

        return (reg == other.reg);
    }

    /**
     * Like {@link #equalsUsingSimpleType} but ignoring the register number.
     * This is useful to determine if two instances refer to the "same"
     * local variable.
     *
     * @param other {@code null-ok;} spec to compare to
     * @return {@code true} iff {@code this} and {@code other} are equal
     * in the stated way
     */
    public boolean matchesVariable(RegisterSpec other) {
        if (other == null) {
            return false;
        }

        return type.getType().equals(other.type.getType())
            && ((local == other.local)
                    || ((local != null) && local.equals(other.local)));
    }

    /**
     * Helper for {@link #equals} and
     * {@link external.com.android.dx.rop.code.RegisterSpec.ForComparison#equals},
     * which actually does the test.
     *
     * @param reg value of the instance variable, for another instance
     * @param type value of the instance variable, for another instance
     * @param local value of the instance variable, for another instance
     * @return whether this instance is equal to one with the given
     * values
     */
    private boolean equals(int reg, TypeBearer type, LocalItem local) {
        return (this.reg == reg)
            && this.type.equals(type)
            && ((this.local == local)
                    || ((this.local != null) && this.local.equals(local)));
    }

    /**
     * Compares by (in priority order) register number, unwrapped type
     * (that is types not {@link TypeBearer}s, and local info.
     *
     * @param other {@code non-null;} spec to compare to
     * @return {@code -1..1;} standard result of comparison
     */
    @Override
    public int compareTo(RegisterSpec other) {
        if (this.reg < other.reg) {
            return -1;
        } else if (this.reg > other.reg) {
            return 1;
        } else if (this == other) {
            return 0;
        }

        int compare = type.getType().compareTo(other.type.getType());

        if (compare != 0) {
            return compare;
        }

        if (this.local == null) {
            return (other.local == null) ? 0 : -1;
        } else if (other.local == null) {
            return 1;
        }

        return this.local.compareTo(other.local);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return hashCodeOf(reg, type, local);
    }

    /**
     * Helper for {@link #hashCode} and
     * {@link external.com.android.dx.rop.code.RegisterSpec.ForComparison#hashCode},
     * which actually does the calculation.
     *
     * @param reg value of the instance variable
     * @param type value of the instance variable
     * @param local value of the instance variable
     * @return the hash code
     */
    private static int hashCodeOf(int reg, TypeBearer type, LocalItem local) {
        int hash = (local != null) ? local.hashCode() : 0;

        hash = (hash * 31 + type.hashCode()) * 31 + reg;
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toString0(false);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return toString0(true);
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return type.getType();
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer getFrameType() {
        return type.getFrameType();
    }

    /** {@inheritDoc} */
    @Override
    public final int getBasicType() {
        return type.getBasicType();
    }

    /** {@inheritDoc} */
    @Override
    public final int getBasicFrameType() {
        return type.getBasicFrameType();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isConstant() {
        return false;
    }

    /**
     * Gets the register number.
     *
     * @return {@code >= 0;} the register number
     */
    public int getReg() {
        return reg;
    }

    /**
     * Gets the type (or actual value) which is loaded from or stored
     * to the register associated with this instance.
     *
     * @return {@code non-null;} the type
     */
    public TypeBearer getTypeBearer() {
        return type;
    }

    /**
     * Gets the variable info associated with this instance, if any.
     *
     * @return {@code null-ok;} the variable info, or {@code null} if this
     * instance has none
     */
    public LocalItem getLocalItem() {
        return local;
    }

    /**
     * Gets the next available register number after the one in this
     * instance. This is equal to the register number plus the width
     * (category) of the type used. Among other things, this may also
     * be used to determine the minimum required register count
     * implied by this instance.
     *
     * @return {@code >= 0;} the required registers size
     */
    public int getNextReg() {
        return reg + getCategory();
    }

    /**
     * Gets the category of this instance's type. This is just a convenient
     * shorthand for {@code getType().getCategory()}.
     *
     * @see #isCategory1
     * @see #isCategory2
     * @return {@code 1..2;} the category of this instance's type
     */
    public int getCategory() {
        return type.getType().getCategory();
    }

    /**
     * Gets whether this instance's type is category 1. This is just a
     * convenient shorthand for {@code getType().isCategory1()}.
     *
     * @see #getCategory
     * @see #isCategory2
     * @return whether or not this instance's type is of category 1
     */
    public boolean isCategory1() {
        return type.getType().isCategory1();
    }

    /**
     * Gets whether this instance's type is category 2. This is just a
     * convenient shorthand for {@code getType().isCategory2()}.
     *
     * @see #getCategory
     * @see #isCategory1
     * @return whether or not this instance's type is of category 2
     */
    public boolean isCategory2() {
        return type.getType().isCategory2();
    }

    /**
     * Gets the string form for just the register number of this instance.
     *
     * @return {@code non-null;} the register string form
     */
    public String regString() {
        return regString(reg);
    }

    /**
     * Returns an instance that is the intersection between this instance
     * and the given one, if any. The intersection is defined as follows:
     *
     * <ul>
     *   <li>If {@code other} is {@code null}, then the result
     *     is {@code null}.
     *   <li>If the register numbers don't match, then the intersection
     *     is {@code null}. Otherwise, the register number of the
     *     intersection is the same as the one in the two instances.</li>
     *   <li>If the types returned by {@code getType()} are not
     *     {@code equals()}, then the intersection is null.</li>
     *   <li>If the type bearers returned by {@code getTypeBearer()}
     *     are {@code equals()}, then the intersection's type bearer
     *     is the one from this instance. Otherwise, the intersection's
     *     type bearer is the {@code getType()} of this instance.</li>
     *   <li>If the locals are {@code equals()}, then the local info
     *     of the intersection is the local info of this instance. Otherwise,
     *     the local info of the intersection is {@code null}.</li>
     * </ul>
     *
     * @param other {@code null-ok;} instance to intersect with (or {@code null})
     * @param localPrimary whether local variables are primary to the
     * intersection; if {@code true}, then the only non-null
     * results occur when registers being intersected have equal local
     * infos (or both have {@code null} local infos)
     * @return {@code null-ok;} the intersection
     */
    public RegisterSpec intersect(RegisterSpec other, boolean localPrimary) {
        if (this == other) {
            // Easy out.
            return this;
        }

        if ((other == null) || (reg != other.getReg())) {
            return null;
        }

        LocalItem resultLocal =
            ((local == null) || !local.equals(other.getLocalItem()))
            ? null : local;
        boolean sameName = (resultLocal == local);

        if (localPrimary && !sameName) {
            return null;
        }

        Type thisType = getType();
        Type otherType = other.getType();

        // Note: Types are always interned.
        if (thisType != otherType) {
            return null;
        }

        TypeBearer resultTypeBearer =
            type.equals(other.getTypeBearer()) ? type : thisType;

        if ((resultTypeBearer == type) && sameName) {
            // It turns out that the intersection is "this" after all.
            return this;
        }

        return (resultLocal == null) ? make(reg, resultTypeBearer) :
            make(reg, resultTypeBearer, resultLocal);
    }

    /**
     * Returns an instance that is identical to this one, except that the
     * register number is replaced by the given one.
     *
     * @param newReg {@code >= 0;} the new register number
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpec withReg(int newReg) {
        if (reg == newReg) {
            return this;
        }

        return makeLocalOptional(newReg, type, local);
    }

    /**
     * Returns an instance that is identical to this one, except that
     * the type is replaced by the given one.
     *
     * @param newType {@code non-null;} the new type
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpec withType(TypeBearer newType) {
        return makeLocalOptional(reg, newType, local);
    }

    /**
     * Returns an instance that is identical to this one, except that the
     * register number is offset by the given amount.
     *
     * @param delta the amount to offset the register number by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpec withOffset(int delta) {
        if (delta == 0) {
            return this;
        }

        return withReg(reg + delta);
    }

    /**
     * Returns an instance that is identical to this one, except that
     * the type bearer is replaced by the actual underlying type
     * (thereby stripping off non-type information) with any
     * initialization information stripped away as well.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpec withSimpleType() {
        TypeBearer orig = type;
        Type newType;

        if (orig instanceof Type) {
            newType = (Type) orig;
        } else {
            newType = orig.getType();
        }

        if (newType.isUninitialized()) {
            newType = newType.getInitializedType();
        }

        if (newType == orig) {
            return this;
        }

        return makeLocalOptional(reg, newType, local);
    }

    /**
     * Returns an instance that is identical to this one except that the
     * local variable is as specified in the parameter.
     *
     * @param local {@code null-ok;} the local item or null for none
     * @return an appropriate instance
     */
    public RegisterSpec withLocalItem(LocalItem local) {
        if ((this.local== local)
                    || ((this.local != null) && this.local.equals(local))) {

            return this;
        }

        return makeLocalOptional(reg, type, local);
    }

    /**
     * @return boolean specifying if this instance is an even register or not.
     */
    public boolean isEvenRegister() {
      return ((getReg() & 1) == 0);
    }

    /**
     * Helper for {@link #toString} and {@link #toHuman}.
     *
     * @param human whether to be human-oriented
     * @return {@code non-null;} the string form
     */
    private String toString0(boolean human) {
        StringBuilder sb = new StringBuilder(40);

        sb.append(regString());
        sb.append(":");

        if (local != null) {
            sb.append(local.toString());
        }

        Type justType = type.getType();
        sb.append(justType);

        if (justType != type) {
            sb.append("=");
            if (human && (type instanceof CstString)) {
                sb.append(((CstString) type).toQuoted());
            } else if (human && (type instanceof Constant)) {
                sb.append(type.toHuman());
            } else {
                sb.append(type);
            }
        }

        return sb.toString();
    }

    public static void clearInternTable() {
        theInterns.clear();
    }

    /**
     * Holder of register spec data for the purposes of comparison (so that
     * {@code RegisterSpec} itself can still keep {@code final}
     * instance variables.
     */
    private static class ForComparison {
        /** {@code >= 0;} register number */
        private int reg;

        /** {@code non-null;} type loaded or stored */
        private TypeBearer type;

        /**
         * {@code null-ok;} local variable associated with this
         * register, if any
         */
        private LocalItem local;

        /**
         * Set all the instance variables.
         *
         * @param reg {@code >= 0;} the register number
         * @param type {@code non-null;} the type (or possibly actual
         * value) which is loaded from or stored to the indicated
         * register
         * @param local {@code null-ok;} the associated local variable, if any
         * @return {@code non-null;} an appropriately-constructed instance
         */
        public void set(int reg, TypeBearer type, LocalItem local) {
            this.reg = reg;
            this.type = type;
            this.local = local;
        }

        /**
         * Construct a {@code RegisterSpec} of this instance's
         * contents.
         *
         * @return {@code non-null;} an appropriately-constructed instance
         */
        public RegisterSpec toRegisterSpec() {
            return new RegisterSpec(reg, type, local);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RegisterSpec)) {
                return false;
            }

            RegisterSpec spec = (RegisterSpec) other;
            return spec.equals(reg, type, local);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return hashCodeOf(reg, type, local);
        }
    }
}
