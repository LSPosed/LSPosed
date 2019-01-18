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

/**
 * Constant type to represent a known-{@code null} value.
 */
public final class CstKnownNull extends CstLiteralBits {
    /** {@code non-null;} unique instance of this class */
    public static final CstKnownNull THE_ONE = new CstKnownNull();

    /**
     * Constructs an instance. This class is not publicly instantiable. Use
     * {@link #THE_ONE}.
     */
    private CstKnownNull() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        return (other instanceof CstKnownNull);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 0x4466757a;
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "known-null";
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.KNOWN_NULL;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "known-null";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return "null";
    }

    /** {@inheritDoc} */
    @Override
    public boolean fitsInInt() {
        // See comment in getIntBits().
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * As "literal bits," a known-null is always represented as the
     * number zero.
     */
    @Override
    public int getIntBits() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * As "literal bits," a known-null is always represented as the
     * number zero.
     */
    @Override
    public long getLongBits() {
        return 0;
    }
}
