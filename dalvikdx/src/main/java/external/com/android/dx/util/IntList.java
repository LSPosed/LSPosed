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

package external.com.android.dx.util;

import java.util.Arrays;

/**
 * Simple list of {@code int}s.
 */
public final class IntList extends MutabilityControl {
    /** {@code non-null;} immutable, no-element instance */
    public static final IntList EMPTY = new IntList(0);

    /** {@code non-null;} array of elements */
    private int[] values;

    /** {@code >= 0;} current size of the list */
    private int size;

    /** whether the values are currently sorted */
    private boolean sorted;

    static {
        EMPTY.setImmutable();
    }

    /**
     * Constructs a new immutable instance with the given element.
     *
     * @param value the sole value in the list
     */
    public static IntList makeImmutable(int value) {
        IntList result = new IntList(1);

        result.add(value);
        result.setImmutable();

        return result;
    }

    /**
     * Constructs a new immutable instance with the given elements.
     *
     * @param value0 the first value in the list
     * @param value1 the second value in the list
     */
    public static IntList makeImmutable(int value0, int value1) {
        IntList result = new IntList(2);

        result.add(value0);
        result.add(value1);
        result.setImmutable();

        return result;
    }

    /**
     * Constructs an empty instance with a default initial capacity.
     */
    public IntList() {
        this(4);
    }

    /**
     * Constructs an empty instance.
     *
     * @param initialCapacity {@code >= 0;} initial capacity of the list
     */
    public IntList(int initialCapacity) {
        super(true);

        try {
            values = new int[initialCapacity];
        } catch (NegativeArraySizeException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("size < 0");
        }

        size = 0;
        sorted = true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = 0;

        for (int i = 0; i < size; i++) {
            result = (result * 31) + values[i];
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (! (other instanceof IntList)) {
            return false;
        }

        IntList otherList = (IntList) other;

        if (sorted != otherList.sorted) {
            return false;
        }

        if (size != otherList.size) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (values[i] != otherList.values[i]) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size * 5 + 10);

        sb.append('{');

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }

        sb.append('}');

        return sb.toString();
    }

    /**
     * Gets the number of elements in this list.
     */
    public int size() {
        return size;
    }

    /**
     * Gets the indicated value.
     *
     * @param n {@code >= 0, < size();} which element
     * @return the indicated element's value
     */
    public int get(int n) {
        if (n >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }

        try {
            return values[n];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate exception.
            throw new IndexOutOfBoundsException("n < 0");
        }
    }

    /**
     * Sets the value at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param value value to store
     */
    public void set(int n, int value) {
        throwIfImmutable();

        if (n >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }

        try {
            values[n] = value;
            sorted = false;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            if (n < 0) {
                throw new IllegalArgumentException("n < 0");
            }
        }
    }

    /**
     * Adds an element to the end of the list. This will increase the
     * list's capacity if necessary.
     *
     * @param value the value to add
     */
    public void add(int value) {
        throwIfImmutable();

        growIfNeeded();

        values[size++] = value;

        if (sorted && (size > 1)) {
            sorted = (value >= values[size - 2]);
        }
    }

    /**
     * Inserts element into specified index, moving elements at and above
     * that index up one. May not be used to insert at an index beyond the
     * current size (that is, insertion as a last element is legal but
     * no further).
     *
     * @param n {@code >= 0, <=size();} index of where to insert
     * @param value value to insert
     */
    public void insert(int n, int value) {
        if (n > size) {
            throw new IndexOutOfBoundsException("n > size()");
        }

        growIfNeeded();

        System.arraycopy (values, n, values, n+1, size - n);
        values[n] = value;
        size++;

        sorted = sorted
                && (n == 0 || value > values[n-1])
                && (n == (size - 1) || value < values[n+1]);
    }

    /**
     * Removes an element at a given index, shifting elements at greater
     * indicies down one.
     *
     * @param n  {@code >=0, < size();} index of element to remove
     */
    public void removeIndex(int n) {
        if (n >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }

        System.arraycopy (values, n + 1, values, n, size - n - 1);
        size--;

        // sort status is unchanged
    }

    /**
     * Increases size of array if needed
     */
    private void growIfNeeded() {
        if (size == values.length) {
            // Resize.
            int[] newv = new int[size * 3 / 2 + 10];
            System.arraycopy(values, 0, newv, 0, size);
            values = newv;
        }
    }

    /**
     * Returns the last element in the array without modifying the array
     *
     * @return last value in the array
     * @throws IndexOutOfBoundsException if stack is empty
     */
    public int top() {
        return get(size - 1);
    }

    /**
     * Pops an element off the end of the list and decreasing the size by one.
     *
     * @return value from what was the last element
     * @throws IndexOutOfBoundsException if stack is empty
     */
    public int pop() {
        throwIfImmutable();

        int result;

        result = get(size-1);
        size--;

        return result;
    }

    /**
     * Pops N elements off the end of the list and decreasing the size by N.
     *
     * @param n {@code >= 0;} number of elements to remove from end
     * @throws IndexOutOfBoundsException if stack is smaller than N
     */
    public void pop(int n) {
        throwIfImmutable();

        size -= n;
    }

    /**
     * Shrinks the size of the list.
     *
     * @param newSize {@code >= 0;} the new size
     */
    public void shrink(int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize < 0");
        }

        if (newSize > size) {
            throw new IllegalArgumentException("newSize > size");
        }

        throwIfImmutable();

        size = newSize;
    }

    /**
     * Makes and returns a mutable copy of the list.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public IntList mutableCopy() {
        int sz = size;
        IntList result = new IntList(sz);

        for (int i = 0; i < sz; i++) {
            result.add(values[i]);
        }

        return result;
    }

    /**
     * Sorts the elements in the list in-place.
     */
    public void sort() {
        throwIfImmutable();

        if (!sorted) {
            Arrays.sort(values, 0, size);
            sorted = true;
        }
    }

    /**
     * Returns the index of the given value, or -1 if the value does not
     * appear in the list.  This will do a binary search if the list is
     * sorted or a linear search if not.
     *
     * @param value value to find
     * @return index of value or -1
     */
    public int indexOf(int value) {
        int ret = binarysearch(value);

        return ret >= 0 ? ret : -1;

    }

    /**
     * Performs a binary search on a sorted list, returning the index of
     * the given value if it is present or
     * {@code (-(insertion point) - 1)} if the value is not present.
     * If the list is not sorted, then reverts to linear search and returns
     * {@code -size()} if the element is not found.
     *
     * @param value value to find
     * @return index of value or {@code (-(insertion point) - 1)} if the
     * value is not present
     */
    public int binarysearch(int value) {
        int sz = size;

        if (!sorted) {
            // Linear search.
            for (int i = 0; i < sz; i++) {
                if (values[i] == value) {
                    return i;
                }
            }

            return -sz;
        }

        /*
         * Binary search. This variant does only one value comparison
         * per iteration but does one more iteration on average than
         * the variant that includes a value equality check per
         * iteration.
         */

        int min = -1;
        int max = sz;

        while (max > (min + 1)) {
            /*
             * The guessIdx calculation is equivalent to ((min + max)
             * / 2) but won't go wonky when min and max are close to
             * Integer.MAX_VALUE.
             */
            int guessIdx = min + ((max - min) >> 1);
            int guess = values[guessIdx];

            if (value <= guess) {
                max = guessIdx;
            } else {
                min = guessIdx;
            }
        }

        if ((max != sz)) {
            return (value == values[max]) ? max : (-max - 1);
        } else {
            return -sz - 1;
        }
    }


    /**
     * Returns whether or not the given value appears in the list.
     * This will do a binary search if the list is sorted or a linear
     * search if not.
     *
     * @see #sort
     *
     * @param value value to look for
     * @return whether the list contains the given value
     */
    public boolean contains(int value) {
        return indexOf(value) >= 0;
    }
}
