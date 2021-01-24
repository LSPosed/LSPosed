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

package external.com.android.dx.rop.cst;

/**
 * Constants of type {@code CONSTANT_*ref_info}.
 */
public abstract class CstMemberRef extends TypedConstant {
    /** {@code non-null;} the type of the defining class */
    private final CstType definingClass;

    /** {@code non-null;} the name-and-type */
    private final CstNat nat;

    /**
     * Constructs an instance.
     *
     * @param definingClass {@code non-null;} the type of the defining class
     * @param nat {@code non-null;} the name-and-type
     */
    /*package*/ CstMemberRef(CstType definingClass, CstNat nat) {
        if (definingClass == null) {
            throw new NullPointerException("definingClass == null");
        }

        if (nat == null) {
            throw new NullPointerException("nat == null");
        }

        this.definingClass = definingClass;
        this.nat = nat;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(Object other) {
        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        CstMemberRef otherRef = (CstMemberRef) other;
        return definingClass.equals(otherRef.definingClass) &&
            nat.equals(otherRef.nat);
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return (definingClass.hashCode() * 31) ^ nat.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This implementation just compares the defining
     * class and name, and it is up to subclasses to compare the rest
     * after calling {@code super.compareTo0()}.</p>
     */
    @Override
    protected int compareTo0(Constant other) {
        CstMemberRef otherMember = (CstMemberRef) other;
        int cmp = definingClass.compareTo(otherMember.definingClass);

        if (cmp != 0) {
            return cmp;
        }

        CstString thisName = nat.getName();
        CstString otherName = otherMember.nat.getName();

        return thisName.compareTo(otherName);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return typeName() + '{' + toHuman() + '}';
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final String toHuman() {
        return definingClass.toHuman() + '.' + nat.toHuman();
    }

    /**
     * Gets the type of the defining class.
     *
     * @return {@code non-null;} the type of defining class
     */
    public final CstType getDefiningClass() {
        return definingClass;
    }

    /**
     * Gets the defining name-and-type.
     *
     * @return {@code non-null;} the name-and-type
     */
    public final CstNat getNat() {
        return nat;
    }
}
