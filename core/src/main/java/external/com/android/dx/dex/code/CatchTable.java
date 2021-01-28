/*
 * Copyright (C) 2008 The Android Open Source Project
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

package external.com.android.dx.dex.code;

import external.com.android.dx.util.FixedSizeList;

/**
 * Table of catch entries. Each entry includes a range of code
 * addresses for which it is valid and an associated {@link
 * CatchHandlerList}.
 */
public final class CatchTable extends FixedSizeList
        implements Comparable<CatchTable> {
    /** {@code non-null;} empty instance */
    public static final CatchTable EMPTY = new CatchTable(0);

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size {@code >= 0;} the size of the table
     */
    public CatchTable(int size) {
        super(size);
    }

    /**
     * Gets the element at the given index. It is an error to call
     * this with the index for an element which was never set; if you
     * do that, this will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which index
     * @return {@code non-null;} element at that index
     */
    public Entry get(int n) {
        return (Entry) get0(n);
    }

    /**
     * Sets the entry at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param entry {@code non-null;} the entry to set at {@code n}
     */
    public void set(int n, Entry entry) {
        set0(n, entry);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(CatchTable other) {
        if (this == other) {
            // Easy out.
            return 0;
        }

        int thisSize = size();
        int otherSize = other.size();
        int checkSize = Math.min(thisSize, otherSize);

        for (int i = 0; i < checkSize; i++) {
            Entry thisEntry = get(i);
            Entry otherEntry = other.get(i);
            int compare = thisEntry.compareTo(otherEntry);
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
     * Entry in a catch list.
     */
    public static class Entry implements Comparable<Entry> {
        /** {@code >= 0;} start address */
        private final int start;

        /** {@code > start;} end address (exclusive) */
        private final int end;

        /** {@code non-null;} list of catch handlers */
        private final CatchHandlerList handlers;

        /**
         * Constructs an instance.
         *
         * @param start {@code >= 0;} start address
         * @param end {@code > start;} end address (exclusive)
         * @param handlers {@code non-null;} list of catch handlers
         */
        public Entry(int start, int end, CatchHandlerList handlers) {
            if (start < 0) {
                throw new IllegalArgumentException("start < 0");
            }

            if (end <= start) {
                throw new IllegalArgumentException("end <= start");
            }

            if (handlers.isMutable()) {
                throw new IllegalArgumentException("handlers.isMutable()");
            }

            this.start = start;
            this.end = end;
            this.handlers = handlers;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hash = (start * 31) + end;
            hash = (hash * 31) + handlers.hashCode();
            return hash;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object other) {
            if (other instanceof Entry) {
                return (compareTo((Entry) other) == 0);
            }

            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(Entry other) {
            if (start < other.start) {
                return -1;
            } else if (start > other.start) {
                return 1;
            }

            if (end < other.end) {
                return -1;
            } else if (end > other.end) {
                return 1;
            }

            return handlers.compareTo(other.handlers);
        }

        /**
         * Gets the start address.
         *
         * @return {@code >= 0;} the start address
         */
        public int getStart() {
            return start;
        }

        /**
         * Gets the end address (exclusive).
         *
         * @return {@code > start;} the end address (exclusive)
         */
        public int getEnd() {
            return end;
        }

        /**
         * Gets the handlers.
         *
         * @return {@code non-null;} the handlers
         */
        public CatchHandlerList getHandlers() {
            return handlers;
        }
    }
}
