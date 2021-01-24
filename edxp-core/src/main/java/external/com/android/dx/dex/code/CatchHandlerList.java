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

import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.FixedSizeList;
import external.com.android.dx.util.Hex;

/**
 * Ordered list of (exception type, handler address) entries.
 */
public final class CatchHandlerList extends FixedSizeList
        implements Comparable<CatchHandlerList> {
    /** {@code non-null;} empty instance */
    public static final CatchHandlerList EMPTY = new CatchHandlerList(0);

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size {@code >= 0;} the size of the list
     */
    public CatchHandlerList(int size) {
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

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return toHuman("", "");
    }

    /**
     * Get the human form of this instance, prefixed on each line
     * with the string.
     *
     * @param prefix {@code non-null;} the prefix for every line
     * @param header {@code non-null;} the header for the first line (after the
     * first prefix)
     * @return {@code non-null;} the human form
     */
    public String toHuman(String prefix, String header) {
        StringBuilder sb = new StringBuilder(100);
        int size = size();

        sb.append(prefix);
        sb.append(header);
        sb.append("catch ");

        for (int i = 0; i < size; i++) {
            Entry entry = get(i);

            if (i != 0) {
                sb.append(",\n");
                sb.append(prefix);
                sb.append("  ");
            }

            if ((i == (size - 1)) && catchesAll()) {
                sb.append("<any>");
            } else {
                sb.append(entry.getExceptionType().toHuman());
            }

            sb.append(" -> ");
            sb.append(Hex.u2or4(entry.getHandler()));
        }

        return sb.toString();
    }

    /**
     * Returns whether or not this instance ends with a "catch-all"
     * handler.
     *
     * @return {@code true} if this instance ends with a "catch-all"
     * handler or {@code false} if not
     */
    public boolean catchesAll() {
        int size = size();

        if (size == 0) {
            return false;
        }

        Entry last = get(size - 1);
        return last.getExceptionType().equals(CstType.OBJECT);
    }

    /**
     * Sets the entry at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param exceptionType {@code non-null;} type of exception handled
     * @param handler {@code >= 0;} exception handler address
     */
    public void set(int n, CstType exceptionType, int handler) {
        set0(n, new Entry(exceptionType, handler));
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
    public int compareTo(CatchHandlerList other) {
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
     * Entry in the list.
     */
    public static class Entry implements Comparable<Entry> {
        /** {@code non-null;} type of exception handled */
        private final CstType exceptionType;

        /** {@code >= 0;} exception handler address */
        private final int handler;

        /**
         * Constructs an instance.
         *
         * @param exceptionType {@code non-null;} type of exception handled
         * @param handler {@code >= 0;} exception handler address
         */
        public Entry(CstType exceptionType, int handler) {
            if (handler < 0) {
                throw new IllegalArgumentException("handler < 0");
            }

            if (exceptionType == null) {
                throw new NullPointerException("exceptionType == null");
            }

            this.handler = handler;
            this.exceptionType = exceptionType;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return (handler * 31) + exceptionType.hashCode();
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
            if (handler < other.handler) {
                return -1;
            } else if (handler > other.handler) {
                return 1;
            }

            return exceptionType.compareTo(other.exceptionType);
        }

        /**
         * Gets the exception type handled.
         *
         * @return {@code non-null;} the exception type
         */
        public CstType getExceptionType() {
            return exceptionType;
        }

        /**
         * Gets the handler address.
         *
         * @return {@code >= 0;} the handler address
         */
        public int getHandler() {
            return handler;
        }
    }
}
