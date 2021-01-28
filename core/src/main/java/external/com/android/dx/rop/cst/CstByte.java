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
 * Constants of type {@code byte}.
 */
public final class CstByte
        extends CstLiteral32 {
    /** {@code non-null;} the value {@code 0} as an instance of this class */
    public static final CstByte VALUE_0 = make((byte) 0);

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param value the {@code byte} value
     */
    public static CstByte make(byte value) {
        return new CstByte(value);
    }

    /**
     * Makes an instance for the given {@code int} value. This
     * may (but does not necessarily) return an already-allocated
     * instance.
     *
     * @param value the value, which must be in range for a {@code byte}
     * @return {@code non-null;} the appropriate instance
     */
    public static CstByte make(int value) {
        byte cast = (byte) value;

        if (cast != value) {
            throw new IllegalArgumentException("bogus byte value: " +
                    value);
        }

        return make(cast);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param value the {@code byte} value
     */
    private CstByte(byte value) {
        super(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int value = getIntBits();
        return "byte{0x" + Hex.u1(value) + " / " + value + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.BYTE;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "byte";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    /**
     * Gets the {@code byte} value.
     *
     * @return the value
     */
    public byte getValue() {
        return (byte) getIntBits();
    }
}
