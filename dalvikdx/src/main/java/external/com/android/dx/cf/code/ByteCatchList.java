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

import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.FixedSizeList;
import external.com.android.dx.util.IntList;

/**
 * List of catch entries, that is, the elements of an "exception table,"
 * which is part of a standard {@code Code} attribute.
 */
public final class ByteCatchList extends FixedSizeList {
    /** {@code non-null;} convenient zero-entry instance */
    public static final ByteCatchList EMPTY = new ByteCatchList(0);

    /**
     * Constructs an instance.
     *
     * @param count the number of elements to be in the table
     */
    public ByteCatchList(int count) {
        super(count);
    }

    /**
     * Gets the total length of this structure in bytes, when included in
     * a {@code Code} attribute. The returned value includes the
     * two bytes for {@code exception_table_length}.
     *
     * @return {@code >= 2;} the total length, in bytes
     */
    public int byteLength() {
        return 2 + size() * 8;
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
     * @param n {@code >= 0, < size();} which entry to set
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
     * @param n {@code >= 0, < size();} which entry to set
     * @param startPc {@code >= 0;} the start pc (inclusive) of the handler's range
     * @param endPc {@code >= startPc;} the end pc (exclusive) of the
     * handler's range
     * @param handlerPc {@code >= 0;} the pc of the exception handler
     * @param exceptionClass {@code null-ok;} the exception class or
     * {@code null} to catch all exceptions with this handler
     */
    public void set(int n, int startPc, int endPc, int handlerPc,
            CstType exceptionClass) {
        set0(n, new Item(startPc, endPc, handlerPc, exceptionClass));
    }

    /**
     * Gets the list of items active at the given address. The result is
     * automatically made immutable.
     *
     * @param pc which address
     * @return {@code non-null;} list of exception handlers active at
     * {@code pc}
     */
    public ByteCatchList listFor(int pc) {
        int sz = size();
        Item[] resultArr = new Item[sz];
        int resultSz = 0;

        for (int i = 0; i < sz; i++) {
            Item one = get(i);
            if (one.covers(pc) && typeNotFound(one, resultArr, resultSz)) {
                resultArr[resultSz] = one;
                resultSz++;
            }
        }

        if (resultSz == 0) {
            return EMPTY;
        }

        ByteCatchList result = new ByteCatchList(resultSz);
        for (int i = 0; i < resultSz; i++) {
            result.set(i, resultArr[i]);
        }

        result.setImmutable();
        return result;
    }

    /**
     * Helper method for {@link #listFor}, which tells whether a match
     * is <i>not</i> found for the exception type of the given item in
     * the given array. A match is considered to be either an exact type
     * match or the class {@code Object} which represents a catch-all.
     *
     * @param item {@code non-null;} item with the exception type to look for
     * @param arr {@code non-null;} array to search in
     * @param count {@code non-null;} maximum number of elements in the array to check
     * @return {@code true} iff the exception type is <i>not</i> found
     */
    private static boolean typeNotFound(Item item, Item[] arr, int count) {
        CstType type = item.getExceptionClass();

        for (int i = 0; i < count; i++) {
            CstType one = arr[i].getExceptionClass();
            if ((one == type) || (one == CstType.OBJECT)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a target list corresponding to this instance. The result
     * is a list of all the exception handler addresses, with the given
     * {@code noException} address appended if appropriate. The
     * result is automatically made immutable.
     *
     * @param noException {@code >= -1;} the no-exception address to append, or
     * {@code -1} not to append anything
     * @return {@code non-null;} list of exception targets, with
     * {@code noException} appended if necessary
     */
    public IntList toTargetList(int noException) {
        if (noException < -1) {
            throw new IllegalArgumentException("noException < -1");
        }

        boolean hasDefault = (noException >= 0);
        int sz = size();

        if (sz == 0) {
            if (hasDefault) {
                /*
                 * The list is empty, but there is a no-exception
                 * address; so, the result is just that address.
                 */
                return IntList.makeImmutable(noException);
            }
            /*
             * The list is empty and there isn't even a no-exception
             * address.
             */
            return IntList.EMPTY;
        }

        IntList result = new IntList(sz + (hasDefault ? 1 : 0));

        for (int i = 0; i < sz; i++) {
            result.add(get(i).getHandlerPc());
        }

        if (hasDefault) {
            result.add(noException);
        }

        result.setImmutable();
        return result;
    }

    /**
     * Returns a rop-style catches list equivalent to this one.
     *
     * @return {@code non-null;} the converted instance
     */
    public TypeList toRopCatchList() {
        int sz = size();
        if (sz == 0) {
            return StdTypeList.EMPTY;
        }

        StdTypeList result = new StdTypeList(sz);

        for (int i = 0; i < sz; i++) {
            result.set(i, get(i).getExceptionClass().getClassType());
        }

        result.setImmutable();
        return result;
    }

    /**
     * Item in an exception handler list.
     */
    public static class Item {
        /** {@code >= 0;} the start pc (inclusive) of the handler's range */
        private final int startPc;

        /** {@code >= startPc;} the end pc (exclusive) of the handler's range */
        private final int endPc;

        /** {@code >= 0;} the pc of the exception handler */
        private final int handlerPc;

        /** {@code null-ok;} the exception class or {@code null} to catch all
         * exceptions with this handler */
        private final CstType exceptionClass;

        /**
         * Constructs an instance.
         *
         * @param startPc {@code >= 0;} the start pc (inclusive) of the
         * handler's range
         * @param endPc {@code >= startPc;} the end pc (exclusive) of the
         * handler's range
         * @param handlerPc {@code >= 0;} the pc of the exception handler
         * @param exceptionClass {@code null-ok;} the exception class or
         * {@code null} to catch all exceptions with this handler
         */
        public Item(int startPc, int endPc, int handlerPc,
                CstType exceptionClass) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            }

            if (endPc < startPc) {
                throw new IllegalArgumentException("endPc < startPc");
            }

            if (handlerPc < 0) {
                throw new IllegalArgumentException("handlerPc < 0");
            }

            this.startPc = startPc;
            this.endPc = endPc;
            this.handlerPc = handlerPc;
            this.exceptionClass = exceptionClass;
        }

        /**
         * Gets the start pc (inclusive) of the handler's range.
         *
         * @return {@code >= 0;} the start pc (inclusive) of the handler's range.
         */
        public int getStartPc() {
            return startPc;
        }

        /**
         * Gets the end pc (exclusive) of the handler's range.
         *
         * @return {@code >= startPc;} the end pc (exclusive) of the
         * handler's range.
         */
        public int getEndPc() {
            return endPc;
        }

        /**
         * Gets the pc of the exception handler.
         *
         * @return {@code >= 0;} the pc of the exception handler
         */
        public int getHandlerPc() {
            return handlerPc;
        }

        /**
         * Gets the class of exception handled.
         *
         * @return {@code non-null;} the exception class; {@link CstType#OBJECT}
         * if this entry handles all possible exceptions
         */
        public CstType getExceptionClass() {
            return (exceptionClass != null) ?
                exceptionClass : CstType.OBJECT;
        }

        /**
         * Returns whether the given address is in the range of this item.
         *
         * @param pc the address
         * @return {@code true} iff this item covers {@code pc}
         */
        public boolean covers(int pc) {
            return (pc >= startPc) && (pc < endPc);
        }
    }
}
