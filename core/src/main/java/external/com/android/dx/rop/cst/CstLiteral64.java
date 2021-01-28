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

/**
 * Constants which are literal 64-bit values of some sort.
 */
public abstract class CstLiteral64
        extends CstLiteralBits {
    /** the value as {@code long} bits */
    private final long bits;

    /**
     * Constructs an instance.
     *
     * @param bits the value as {@code long} bits
     */
    /*package*/ CstLiteral64(long bits) {
        this.bits = bits;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(Object other) {
        return (other != null) &&
            (getClass() == other.getClass()) &&
            bits == ((CstLiteral64) other).bits;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return (int) bits ^ (int) (bits >> 32);
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        long otherBits = ((CstLiteral64) other).bits;

        if (bits < otherBits) {
            return -1;
        } else if (bits > otherBits) {
            return 1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isCategory2() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean fitsInInt() {
        return (int) bits == bits;
    }

    /** {@inheritDoc} */
    @Override
    public final int getIntBits() {
        return (int) bits;
    }

    /** {@inheritDoc} */
    @Override
    public final long getLongBits() {
        return bits;
    }
}
