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

import external.com.android.dx.util.FixedSizeList;

/**
 * Constant type to represent a fixed array of other constants.
 */
public class CstArray extends Constant {
    /** {@code non-null;} the actual list of contents */
    private final List list;

    /**
     * Constructs an instance.
     *
     * @param list {@code non-null;} the actual list of contents
     */
    public CstArray(List list) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }

        list.throwIfMutable();

        this.list = list;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof CstArray)) {
            return false;
        }

        return list.equals(((CstArray) other).list);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return list.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        return list.compareTo(((CstArray) other).list);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return list.toString("array{", ", ", "}");
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "array";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return list.toHuman("{", ", ", "}");
    }

    /**
     * Get the underlying list.
     *
     * @return {@code non-null;} the list
     */
    public List getList() {
        return list;
    }

    /**
     * List of {@link Constant} instances.
     */
    public static final class List
            extends FixedSizeList implements Comparable<List> {
        /**
         * Constructs an instance. All indices initially contain
         * {@code null}.
         *
         * @param size the size of the list
         */
        public List(int size) {
            super(size);
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(List other) {
            int thisSize = size();
            int otherSize = other.size();
            int compareSize = (thisSize < otherSize) ? thisSize : otherSize;

            for (int i = 0; i < compareSize; i++) {
                Constant thisItem = (Constant) get0(i);
                Constant otherItem = (Constant) other.get0(i);
                int compare = thisItem.compareTo(otherItem);
                if (compare != 0) {
                    return compare;
                }
            }

            if (thisSize < otherSize) {
                return -1;
            } else if (thisSize > otherSize) {
                return 1;
            }

            return 0;
        }

        /**
         * Gets the element at the given index. It is an error to call
         * this with the index for an element which was never set; if you
         * do that, this will throw {@code NullPointerException}.
         *
         * @param n {@code >= 0, < size();} which index
         * @return {@code non-null;} element at that index
         */
        public Constant get(int n) {
            return (Constant) get0(n);
        }

        /**
         * Sets the element at the given index.
         *
         * @param n {@code >= 0, < size();} which index
         * @param a {@code null-ok;} the element to set at {@code n}
         */
        public void set(int n, Constant a) {
            set0(n, a);
        }
    }
}
