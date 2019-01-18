/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * Constant type to represent a reference to a particular constant
 * value of an enumerated type.
 */
public final class CstEnumRef extends CstMemberRef {
    /** {@code null-ok;} the corresponding field ref, lazily initialized */
    private CstFieldRef fieldRef;

    /**
     * Constructs an instance.
     *
     * @param nat {@code non-null;} the name-and-type; the defining class is derived
     * from this
     */
    public CstEnumRef(CstNat nat) {
        super(new CstType(nat.getFieldType()), nat);

        fieldRef = null;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "enum";
    }

    /**
     * {@inheritDoc}
     *
     * <b>Note:</b> This returns the enumerated type.
     */
    @Override
    public Type getType() {
        return getDefiningClass().getClassType();
    }

    /**
     * Get a {@link CstFieldRef} that corresponds with this instance.
     *
     * @return {@code non-null;} the corresponding field reference
     */
    public CstFieldRef getFieldRef() {
        if (fieldRef == null) {
            fieldRef = new CstFieldRef(getDefiningClass(), getNat());
        }

        return fieldRef;
    }
}
