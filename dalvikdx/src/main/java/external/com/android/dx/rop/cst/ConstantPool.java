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
 * Interface for constant pools, which are, more or less, just lists of
 * {@link Constant} objects.
 */
public interface ConstantPool {
    /**
     * Get the "size" of the constant pool. This corresponds to the
     * class file field {@code constant_pool_count}, and is in fact
     * always at least one more than the actual size of the constant pool,
     * as element {@code 0} is always invalid.
     *
     * @return {@code >= 1;} the size
     */
    public int size();

    /**
     * Get the {@code n}th entry in the constant pool, which must
     * be valid.
     *
     * @param n {@code n >= 0, n < size();} the constant pool index
     * @return {@code non-null;} the corresponding entry
     * @throws IllegalArgumentException thrown if {@code n} is
     * in-range but invalid
     */
    public Constant get(int n);

    /**
     * Get the {@code n}th entry in the constant pool, which must
     * be valid unless {@code n == 0}, in which case {@code null}
     * is returned.
     *
     * @param n {@code n >= 0, n < size();} the constant pool index
     * @return {@code null-ok;} the corresponding entry, if {@code n != 0}
     * @throws IllegalArgumentException thrown if {@code n} is
     * in-range and non-zero but invalid
     */
    public Constant get0Ok(int n);

    /**
     * Get the {@code n}th entry in the constant pool, or
     * {@code null} if the index is in-range but invalid. In
     * particular, {@code null} is returned for index {@code 0}
     * as well as the index after any entry which is defined to take up
     * two slots (that is, {@code Long} and {@code Double}
     * entries).
     *
     * @param n {@code n >= 0, n < size();} the constant pool index
     * @return {@code null-ok;} the corresponding entry, or {@code null} if
     * the index is in-range but invalid
     */
    public Constant getOrNull(int n);

    /**
     * Get all entries in this constant pool.
     *
     * @return the returned array may contain null entries.
     */
    public Constant[] getEntries();
}
