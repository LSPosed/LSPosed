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

import external.com.android.dx.rop.type.Type;

/**
 * Constants of type {@code CONSTANT_Fieldref_info}.
 */
public final class CstFieldRef extends CstMemberRef {
    /**
     * Returns an instance of this class that represents the static
     * field which should hold the class corresponding to a given
     * primitive type. For example, if given {@link Type#INT}, this
     * method returns an instance corresponding to the field
     * {@code java.lang.Integer.TYPE}.
     *
     * @param primitiveType {@code non-null;} the primitive type
     * @return {@code non-null;} the corresponding static field
     */
    public static CstFieldRef forPrimitiveType(Type primitiveType) {
        return new CstFieldRef(CstType.forBoxedPrimitiveType(primitiveType),
                CstNat.PRIMITIVE_TYPE_NAT);
    }

    /**
     * Constructs an instance.
     *
     * @param definingClass {@code non-null;} the type of the defining class
     * @param nat {@code non-null;} the name-and-type
     */
    public CstFieldRef(CstType definingClass, CstNat nat) {
        super(definingClass, nat);
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "field";
    }

    /**
     * Returns the type of this field.
     *
     * @return {@code non-null;} the field's type
     */
    @Override
    public Type getType() {
        return getNat().getFieldType();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        int cmp = super.compareTo0(other);

        if (cmp != 0) {
            return cmp;
        }

        CstFieldRef otherField = (CstFieldRef) other;
        CstString thisDescriptor = getNat().getDescriptor();
        CstString otherDescriptor = otherField.getNat().getDescriptor();
        return thisDescriptor.compareTo(otherDescriptor);
    }
}
