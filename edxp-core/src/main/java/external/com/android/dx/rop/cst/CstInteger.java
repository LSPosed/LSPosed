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
 * Constants of type {@code CONSTANT_Integer_info}.
 */
public final class CstInteger
        extends CstLiteral32 {
    /** {@code non-null;} array of cached instances */
    private static final CstInteger[] cache = new CstInteger[511];

    /** {@code non-null;} instance representing {@code -1} */
    public static final CstInteger VALUE_M1 = make(-1);

    /** {@code non-null;} instance representing {@code 0} */
    public static final CstInteger VALUE_0 = make(0);

    /** {@code non-null;} instance representing {@code 1} */
    public static final CstInteger VALUE_1 = make(1);

    /** {@code non-null;} instance representing {@code 2} */
    public static final CstInteger VALUE_2 = make(2);

    /** {@code non-null;} instance representing {@code 3} */
    public static final CstInteger VALUE_3 = make(3);

    /** {@code non-null;} instance representing {@code 4} */
    public static final CstInteger VALUE_4 = make(4);

    /** {@code non-null;} instance representing {@code 5} */
    public static final CstInteger VALUE_5 = make(5);

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param value the {@code int} value
     * @return {@code non-null;} the appropriate instance
     */
    public static CstInteger make(int value) {
        /*
         * Note: No need to synchronize, since we don't make any sort
         * of guarantee about ==, and it's okay to overwrite existing
         * entries too.
         */
        int idx = (value & 0x7fffffff) % cache.length;
        CstInteger obj = cache[idx];

        if ((obj != null) && (obj.getValue() == value)) {
            return obj;
        }

        obj = new CstInteger(value);
        cache[idx] = obj;
        return obj;
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param value the {@code int} value
     */
    private CstInteger(int value) {
        super(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int value = getIntBits();
        return "int{0x" + Hex.u4(value) + " / " + value + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.INT;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "int";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    /**
     * Gets the {@code int} value.
     *
     * @return the value
     */
    public int getValue() {
        return getIntBits();
    }
}
