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
 * Constants of type {@code char}.
 */
public final class CstChar
        extends CstLiteral32 {
    /** {@code non-null;} the value {@code 0} as an instance of this class */
    public static final CstChar VALUE_0 = make((char) 0);

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param value the {@code char} value
     */
    public static CstChar make(char value) {
        return new CstChar(value);
    }

    /**
     * Makes an instance for the given {@code int} value. This
     * may (but does not necessarily) return an already-allocated
     * instance.
     *
     * @param value the value, which must be in range for a {@code char}
     * @return {@code non-null;} the appropriate instance
     */
    public static CstChar make(int value) {
        char cast = (char) value;

        if (cast != value) {
            throw new IllegalArgumentException("bogus char value: " +
                    value);
        }

        return make(cast);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param value the {@code char} value
     */
    private CstChar(char value) {
        super(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int value = getIntBits();
        return "char{0x" + Hex.u2(value) + " / " + value + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.CHAR;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "char";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    /**
     * Gets the {@code char} value.
     *
     * @return the value
     */
    public char getValue() {
        return (char) getIntBits();
    }
}
