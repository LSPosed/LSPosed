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

package external.com.android.dx.cf.code;

import external.com.android.dx.util.FixedSizeList;

/**
 * List of "line number" entries, which are the contents of
 * {@code LineNumberTable} attributes.
 */
public final class LineNumberList extends FixedSizeList {
    /** {@code non-null;} zero-size instance */
    public static final LineNumberList EMPTY = new LineNumberList(0);

    /**
     * Returns an instance which is the concatenation of the two given
     * instances.
     *
     * @param list1 {@code non-null;} first instance
     * @param list2 {@code non-null;} second instance
     * @return {@code non-null;} combined instance
     */
    public static LineNumberList concat(LineNumberList list1,
                                        LineNumberList list2) {
        if (list1 == EMPTY) {
            // easy case
            return list2;
        }

        int sz1 = list1.size();
        int sz2 = list2.size();
        LineNumberList result = new LineNumberList(sz1 + sz2);

        for (int i = 0; i < sz1; i++) {
            result.set(i, list1.get(i));
        }

        for (int i = 0; i < sz2; i++) {
            result.set(sz1 + i, list2.get(i));
        }

        return result;
    }

    /**
     * Constructs an instance.
     *
     * @param count the number of elements to be in the list
     */
    public LineNumberList(int count) {
        super(count);
    }

    /**
     * Gets the indicated item.
     *
     * @param n {@code >= 0;} which item
     * @return {@code null-ok;} the indicated item
     */
    public Item get(int n) {
        return (Item) get0(n);
    }

    /**
     * Sets the item at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param item {@code non-null;} the item
     */
    public void set(int n, Item item) {
        if (item == null) {
            throw new NullPointerException("item == null");
        }

        set0(n, item);
    }

    /**
     * Sets the item at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param startPc {@code >= 0;} start pc of this item
     * @param lineNumber {@code >= 0;} corresponding line number
     */
    public void set(int n, int startPc, int lineNumber) {
        set0(n, new Item(startPc, lineNumber));
    }

    /**
     * Gets the line number associated with the given address.
     *
     * @param pc {@code >= 0;} the address to look up
     * @return {@code >= -1;} the associated line number, or {@code -1} if
     * none is known
     */
    public int pcToLine(int pc) {
        /*
         * Line number entries don't have to appear in any particular
         * order, so we have to do a linear search. TODO: If
         * this turns out to be a bottleneck, consider sorting the
         * list prior to use.
         */
        int sz = size();
        int bestPc = -1;
        int bestLine = -1;

        for (int i = 0; i < sz; i++) {
            Item one = get(i);
            int onePc = one.getStartPc();
            if ((onePc <= pc) && (onePc > bestPc)) {
                bestPc = onePc;
                bestLine = one.getLineNumber();
                if (bestPc == pc) {
                    // We can't do better than this
                    break;
                }
            }
        }

        return bestLine;
    }

    /**
     * Item in a line number table.
     */
    public static class Item {
        /** {@code >= 0;} start pc of this item */
        private final int startPc;

        /** {@code >= 0;} corresponding line number */
        private final int lineNumber;

        /**
         * Constructs an instance.
         *
         * @param startPc {@code >= 0;} start pc of this item
         * @param lineNumber {@code >= 0;} corresponding line number
         */
        public Item(int startPc, int lineNumber) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            }

            if (lineNumber < 0) {
                throw new IllegalArgumentException("lineNumber < 0");
            }

            this.startPc = startPc;
            this.lineNumber = lineNumber;
        }

        /**
         * Gets the start pc of this item.
         *
         * @return the start pc
         */
        public int getStartPc() {
            return startPc;
        }

        /**
         * Gets the line number of this item.
         *
         * @return the line number
         */
        public int getLineNumber() {
            return lineNumber;
        }
    }
}
