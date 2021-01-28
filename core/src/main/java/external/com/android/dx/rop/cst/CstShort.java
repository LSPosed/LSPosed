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
 * Constants of type {@code short}.
 */
public final class CstShort
        extends CstLiteral32 {
    /** {@code non-null;} the value {@code 0} as an instance of this class */
    public static final CstShort VALUE_0 = make((short) 0);

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param value the {@code short} value
     * @return {@code non-null;} the appropriate instance
     */
    public static CstShort make(short value) {
        return new CstShort(value);
    }

    /**
     * Makes an instance for the given {@code int} value. This
     * may (but does not necessarily) return an already-allocated
     * instance.
     *
     * @param value the value, which must be in range for a {@code short}
     * @return {@code non-null;} the appropriate instance
     */
    public static CstShort make(int value) {
        short cast = (short) value;

        if (cast != value) {
            throw new IllegalArgumentException("bogus short value: " +
                    value);
        }

        return make(cast);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param value the {@code short} value
     */
    private CstShort(short value) {
        super(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int value = getIntBits();
        return "short{0x" + Hex.u2(value) + " / " + value + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.SHORT;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "short";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    /**
     * Gets the {@code short} value.
     *
     * @return the value
     */
    public short getValue() {
        return (short) getIntBits();
    }
}
