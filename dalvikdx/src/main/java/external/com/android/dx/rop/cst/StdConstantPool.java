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

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.MutabilityControl;

/**
 * Standard implementation of {@link ConstantPool}, which directly stores
 * an array of {@link Constant} objects and can be made immutable.
 */
public final class StdConstantPool
        extends MutabilityControl implements ConstantPool {
    /** {@code non-null;} array of entries */
    private final Constant[] entries;

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the pool; this corresponds to the
     * class file field {@code constant_pool_count}, and is in fact
     * always at least one more than the actual size of the constant pool,
     * as element {@code 0} is always invalid.
     */
    public StdConstantPool(int size) {
        super(size > 1);

        if (size < 1) {
            throw new IllegalArgumentException("size < 1");
        }

        entries = new Constant[size];
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return entries.length;
    }

    /** {@inheritDoc} */
    @Override
    public Constant getOrNull(int n) {
        try {
            return entries[n];
        } catch (IndexOutOfBoundsException ex) {
            // Translate the exception.
            return throwInvalid(n);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Constant get0Ok(int n) {
        if (n == 0) {
            return null;
        }

        return get(n);
    }

    /** {@inheritDoc} */
    @Override
    public Constant get(int n) {
        try {
            Constant result = entries[n];

            if (result == null) {
                throwInvalid(n);
            }

            return result;
        } catch (IndexOutOfBoundsException ex) {
            // Translate the exception.
            return throwInvalid(n);
        }
    }

    /**
     * Get all entries in this constant pool.
     *
     * @return the returned array may contain null entries.
     */
    @Override
    public Constant[] getEntries() {
        return entries;
    }

    /**
     * Sets the entry at the given index.
     *
     * @param n {@code >= 1, < size();} which entry
     * @param cst {@code null-ok;} the constant to store
     */
    public void set(int n, Constant cst) {
        throwIfImmutable();

        boolean cat2 = (cst != null) && cst.isCategory2();

        if (n < 1) {
            throw new IllegalArgumentException("n < 1");
        }

        if (cat2) {
            // Storing a category-2 entry nulls out the next index.
            if (n == (entries.length - 1)) {
                throw new IllegalArgumentException("(n == size - 1) && " +
                                                   "cst.isCategory2()");
            }
            entries[n + 1] = null;
        }

        if ((cst != null) && (entries[n] == null)) {
            /*
             * Overwriting the second half of a category-2 entry nulls out
             * the first half.
             */
            Constant prev = entries[n - 1];
            if ((prev != null) && prev.isCategory2()) {
                entries[n - 1] = null;
            }
        }

        entries[n] = cst;
    }

    /**
     * Throws the right exception for an invalid cpi.
     *
     * @param idx the bad cpi
     * @return never
     * @throws ExceptionWithContext always thrown
     */
    private static Constant throwInvalid(int idx) {
        throw new ExceptionWithContext("invalid constant pool index " +
                                       Hex.u2(idx));
    }
}
