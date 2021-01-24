/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.type.TypeList;

/**
 * An operation on two values of the same type.
 *
 * <p>Math operations ({@link #ADD}, {@link #SUBTRACT}, {@link #MULTIPLY},
 * {@link #DIVIDE}, and {@link #REMAINDER}) support ints, longs, floats and
 * doubles.
 *
 * <p>Bit operations ({@link #AND}, {@link #OR}, {@link #XOR}, {@link
 * #SHIFT_LEFT}, {@link #SHIFT_RIGHT}, {@link #UNSIGNED_SHIFT_RIGHT}) support
 * ints and longs.
 *
 * <p>Division by zero behaves differently depending on the operand type.
 * For int and long operands, {@link #DIVIDE} and {@link #REMAINDER} throw
 * {@link ArithmeticException} if {@code b == 0}. For float and double operands,
 * the operations return {@code NaN}.
 */
public enum BinaryOp {
    /** {@code a + b} */
    ADD() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opAdd(types);
        }
    },

    /** {@code a - b} */
    SUBTRACT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opSub(types);
        }
    },

    /** {@code a * b} */
    MULTIPLY() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opMul(types);
        }
    },

    /** {@code a / b} */
    DIVIDE() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opDiv(types);
        }
    },

    /** {@code a % b} */
    REMAINDER() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opRem(types);
        }
    },

    /** {@code a & b} */
    AND() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opAnd(types);
        }
    },

    /** {@code a | b} */
    OR() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opOr(types);
        }
    },

    /** {@code a ^ b} */
    XOR() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opXor(types);
        }
    },

    /** {@code a << b} */
    SHIFT_LEFT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opShl(types);
        }
    },

    /** {@code a >> b} */
    SHIFT_RIGHT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opShr(types);
        }
    },

    /** {@code a >>> b} */
    UNSIGNED_SHIFT_RIGHT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opUshr(types);
        }
    };

    abstract Rop rop(external.com.android.dx.rop.type.TypeList types);
}
