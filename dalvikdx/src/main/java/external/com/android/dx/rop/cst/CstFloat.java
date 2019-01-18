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
import external.com.android.dx.util.Hex;

/**
 * Constants of type {@code CONSTANT_Float_info}.
 */
public final class CstFloat
        extends CstLiteral32 {
    /** {@code non-null;} instance representing {@code 0} */
    public static final CstFloat VALUE_0 = make(Float.floatToIntBits(0.0f));

    /** {@code non-null;} instance representing {@code 1} */
    public static final CstFloat VALUE_1 = make(Float.floatToIntBits(1.0f));

    /** {@code non-null;} instance representing {@code 2} */
    public static final CstFloat VALUE_2 = make(Float.floatToIntBits(2.0f));

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param bits the {@code float} value as {@code int} bits
     */
    public static CstFloat make(int bits) {
        /*
         * Note: Javadoc notwithstanding, this implementation always
         * allocates.
         */
        return new CstFloat(bits);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param bits the {@code float} value as {@code int} bits
     */
    private CstFloat(int bits) {
        super(bits);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int bits = getIntBits();
        return "float{0x" + Hex.u4(bits) + " / " +
            Float.intBitsToFloat(bits) + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.FLOAT;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "float";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return Float.toString(Float.intBitsToFloat(getIntBits()));
    }

    /**
     * Gets the {@code float} value.
     *
     * @return the value
     */
    public float getValue() {
        return Float.intBitsToFloat(getIntBits());
    }
}
