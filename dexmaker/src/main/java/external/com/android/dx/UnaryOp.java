/*
 * Copyright (C) 2012 The Android Open Source Project
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

/**
 * An operation on one value.
 */
public enum UnaryOp {

    /** {@code ~a}. Supports int and long. */
    NOT() {
        @Override
        Rop rop(TypeId<?> type) {
            return Rops.opNot(type.ropType);
        }
    },

    /** {@code -a}. Supports int, long, float and double. */
    NEGATE() {
        @Override
        Rop rop(TypeId<?> type) {
            return Rops.opNeg(type.ropType);
        }
    };

    abstract Rop rop(TypeId<?> type);
}
