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

package external.com.android.dx.cf.attrib;

import external.com.android.dx.rop.cst.CstDouble;
import external.com.android.dx.rop.cst.CstFloat;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstLong;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.TypedConstant;

/**
 * Attribute class for standard {@code ConstantValue} attributes.
 */
public final class AttConstantValue extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "ConstantValue";

    /** {@code non-null;} the constant value */
    private final TypedConstant constantValue;

    /**
     * Constructs an instance.
     *
     * @param constantValue {@code non-null;} the constant value, which must
     * be an instance of one of: {@code CstString},
     * {@code CstInteger}, {@code CstLong},
     * {@code CstFloat}, or {@code CstDouble}
     */
    public AttConstantValue(TypedConstant constantValue) {
        super(ATTRIBUTE_NAME);

        if (!((constantValue instanceof CstString) ||
               (constantValue instanceof CstInteger) ||
               (constantValue instanceof CstLong) ||
               (constantValue instanceof CstFloat) ||
               (constantValue instanceof CstDouble))) {
            if (constantValue == null) {
                throw new NullPointerException("constantValue == null");
            }
            throw new IllegalArgumentException("bad type for constantValue");
        }

        this.constantValue = constantValue;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8;
    }

    /**
     * Gets the constant value of this instance. The returned value
     * is an instance of one of: {@code CstString},
     * {@code CstInteger}, {@code CstLong},
     * {@code CstFloat}, or {@code CstDouble}.
     *
     * @return {@code non-null;} the constant value
     */
    public TypedConstant getConstantValue() {
        return constantValue;
    }
}
