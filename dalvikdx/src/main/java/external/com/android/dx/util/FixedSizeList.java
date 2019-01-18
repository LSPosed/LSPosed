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
 * Simple (mostly) fixed-size list of objects, which may be made immutable.
 */
public class FixedSizeList
        extends MutabilityControl implements ToHuman {
    /** {@code non-null;} array of elements */
    private Object[] arr;

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     */
    public FixedSizeList(int size) {
        super(size != 0);

        try {
            arr = new Object[size];
        } catch (NegativeArraySizeException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("size < 0");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // Easy out.
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            // Another easy out.
            return false;
        }

        FixedSizeList list = (FixedSizeList) other;
        return Arrays.equals(arr, list.arr);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(arr);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String name = getClass().getName();

        return toString0(name.substring(name.lastIndexOf('.') + 1) + '{',
                         ", ",
                         "}",
                         false);
    }

    /**
     * {@inheritDoc}
     *
     * This method will only work if every element of the list
     * implements {@link ToHuman}.
     */
    @Override
    public String toHuman() {
        String name = getClass().getName();

        return toString0(name.substring(name.lastIndexOf('.') + 1) + '{',
                         ", ",
                         "}",
                         true);
    }

    /**
     * Gets a customized string form for this instance.
     *
     * @param prefix {@code null-ok;} prefix for the start of the result
     * @param separator {@code null-ok;} separator to insert between each item
     * @param suffix {@code null-ok;} suffix for the end of the result
     * @return {@code non-null;} the custom string
     */
    public String toString(String prefix, String separator, String suffix) {
        return toString0(prefix, separator, suffix, false);
    }

    /**
     * Gets a customized human string for this instance. This method will
     * only work if every element of the list implements {@link
     * ToHuman}.
     *
     * @param prefix {@code null-ok;} prefix for the start of the result
     * @param separator {@code null-ok;} separator to insert between each item
     * @param suffix {@code null-ok;} suffix for the end of the result
     * @return {@code non-null;} the custom string
     */
    public String toHuman(String prefix, String separator, String suffix) {
        return toString0(prefix, separator, suffix, true);
    }

    /**
     * Gets the number of elements in this list.
     */
    public final int size() {
        return arr.length;
    }

    /**
     * Shrinks this instance to fit, by removing any unset
     * ({@code null}) elements, leaving the remaining elements in
     * their original order.
     */
    public void shrinkToFit() {
        int sz = arr.length;
        int newSz = 0;

        for (int i = 0; i < sz; i++) {
            if (arr[i] != null) {
                newSz++;
            }
        }

        if (sz == newSz) {
            return;
        }

        throwIfImmutable();

        Object[] newa = new Object[newSz];
        int at = 0;

        for (int i = 0; i < sz; i++) {
            Object one = arr[i];
            if (one != null) {
                newa[at] = one;
                at++;
            }
        }

        arr = newa;
        if (newSz == 0) {
            setImmutable();
        }
    }

    /**
     * Gets the indicated element. It is an error to call this with the
     * index for an element which was never set; if you do that, this
     * will throw {@code NullPointerException}. This method is
     * protected so that subclasses may offer a safe type-checked
     * public interface to their clients.
     *
     * @param n {@code >= 0, < size();} which element
     * @return {@code non-null;} the indicated element
     */
    protected final Object get0(int n) {
        try {
            Object result = arr[n];

            if (result == null) {
                throw new NullPointerException("unset: " + n);
            }

            return result;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            return throwIndex(n);
        }
    }

    /**
     * Gets the indicated element, allowing {@code null}s to be
     * returned. This method is protected so that subclasses may
     * (optionally) offer a safe type-checked public interface to
     * their clients.
     *
     * @param n {@code >= 0, < size();} which element
     * @return {@code null-ok;} the indicated element
     */
    protected final Object getOrNull0(int n) {
        return arr[n];
    }

    /**
     * Sets the element at the given index, but without doing any type
     * checks on the element. This method is protected so that
     * subclasses may offer a safe type-checked public interface to
     * their clients.
     *
     * @param n {@code >= 0, < size();} which element
     * @param obj {@code null-ok;} the value to store
     */
    protected final void set0(int n, Object obj) {
        throwIfImmutable();

        try {
            arr[n] = obj;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throwIndex(n);
        }
    }

    /**
     * Throws the appropriate exception for the given index value.
     *
     * @param n the index value
     * @return never
     * @throws IndexOutOfBoundsException always thrown
     */
    private Object throwIndex(int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }

        throw new IndexOutOfBoundsException("n >= size()");
    }

    /**
     * Helper for {@link #toString} and {@link #toHuman}, which both of
     * those call to pretty much do everything.
     *
     * @param prefix {@code null-ok;} prefix for the start of the result
     * @param separator {@code null-ok;} separator to insert between each item
     * @param suffix {@code null-ok;} suffix for the end of the result
     * @param human whether the output is to be human
     * @return {@code non-null;} the custom string
     */
    private String toString0(String prefix, String separator, String suffix,
                             boolean human) {
        int len = arr.length;
        StringBuilder sb = new StringBuilder(len * 10 + 10);

        if (prefix != null) {
            sb.append(prefix);
        }

        for (int i = 0; i < len; i++) {
            if ((i != 0) && (separator != null)) {
                sb.append(separator);
            }

            if (human) {
                sb.append(((ToHuman) arr[i]).toHuman());
            } else {
                sb.append(arr[i]);
            }
        }

        if (suffix != null) {
            sb.append(suffix);
        }

        return sb.toString();
    }

}
