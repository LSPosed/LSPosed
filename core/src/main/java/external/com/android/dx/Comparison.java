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
 * A comparison between two values of the same type.
 */
public enum Comparison {

    /** {@code a < b}. Supports int only. */
    LT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfLt(types);
        }
    },

    /** {@code a <= b}. Supports int only. */
    LE() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfLe(types);
        }
    },

    /** {@code a == b}. Supports int and reference types. */
    EQ() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfEq(types);
        }
    },

    /** {@code a >= b}. Supports int only. */
    GE() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfGe(types);
        }
    },

    /** {@code a > b}. Supports int only. */
    GT() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfGt(types);
        }
    },

    /** {@code a != b}. Supports int and reference types. */
    NE() {
        @Override
        Rop rop(TypeList types) {
            return Rops.opIfNe(types);
        }
    };

    abstract Rop rop(TypeList types);
}
