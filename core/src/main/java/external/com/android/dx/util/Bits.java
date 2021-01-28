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

/**
 * Utilities for treating {@code int[]}s as bit sets.
 */
public final class Bits {
    /**
     * This class is uninstantiable.
     */
    private Bits() {
        // This space intentionally left blank.
    }

    /**
     * Constructs a bit set to contain bits up to the given index (exclusive).
     *
     * @param max {@code >= 0;} the maximum bit index (exclusive)
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static int[] makeBitSet(int max) {
        int size = (max + 0x1f) >> 5;
        return new int[size];
    }

    /**
     * Gets the maximum index (exclusive) for the given bit set.
     *
     * @param bits {@code non-null;} bit set in question
     * @return {@code >= 0;} the maximum index (exclusive) that may be set
     */
    public static int getMax(int[] bits) {
        return bits.length * 0x20;
    }

    /**
     * Gets the value of the bit at the given index.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param idx {@code >= 0, < getMax(set);} which bit
     * @return the value of the indicated bit
     */
    public static boolean get(int[] bits, int idx) {
        int arrayIdx = idx >> 5;
        int bit = 1 << (idx & 0x1f);
        return (bits[arrayIdx] & bit) != 0;
    }

    /**
     * Sets the given bit to the given value.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param idx {@code >= 0, < getMax(set);} which bit
     * @param value the new value for the bit
     */
    public static void set(int[] bits, int idx, boolean value) {
        int arrayIdx = idx >> 5;
        int bit = 1 << (idx & 0x1f);

        if (value) {
            bits[arrayIdx] |= bit;
        } else {
            bits[arrayIdx] &= ~bit;
        }
    }

    /**
     * Sets the given bit to {@code true}.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param idx {@code >= 0, < getMax(set);} which bit
     */
    public static void set(int[] bits, int idx) {
        int arrayIdx = idx >> 5;
        int bit = 1 << (idx & 0x1f);
        bits[arrayIdx] |= bit;
    }

    /**
     * Sets the given bit to {@code false}.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param idx {@code >= 0, < getMax(set);} which bit
     */
    public static void clear(int[] bits, int idx) {
        int arrayIdx = idx >> 5;
        int bit = 1 << (idx & 0x1f);
        bits[arrayIdx] &= ~bit;
    }

    /**
     * Returns whether or not the given bit set is empty, that is, whether
     * no bit is set to {@code true}.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @return {@code true} iff all bits are {@code false}
     */
    public static boolean isEmpty(int[] bits) {
        int len = bits.length;

        for (int i = 0; i < len; i++) {
            if (bits[i] != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the number of bits set to {@code true} in the given bit set.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @return {@code >= 0;} the bit count (aka population count) of the set
     */
    public static int bitCount(int[] bits) {
        int len = bits.length;
        int count = 0;

        for (int i = 0; i < len; i++) {
            count += Integer.bitCount(bits[i]);
        }

        return count;
    }

    /**
     * Returns whether any bits are set to {@code true} in the
     * specified range.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param start {@code >= 0;} index of the first bit in the range (inclusive)
     * @param end {@code >= 0;} index of the last bit in the range (exclusive)
     * @return {@code true} if any bit is set to {@code true} in
     * the indicated range
     */
    public static boolean anyInRange(int[] bits, int start, int end) {
        int idx = findFirst(bits, start);
        return (idx >= 0) && (idx < end);
    }

    /**
     * Finds the lowest-order bit set at or after the given index in the
     * given bit set.
     *
     * @param bits {@code non-null;} bit set to operate on
     * @param idx {@code >= 0;} minimum index to return
     * @return {@code >= -1;} lowest-order bit set at or after {@code idx},
     * or {@code -1} if there is no appropriate bit index to return
     */
    public static int findFirst(int[] bits, int idx) {
        int len = bits.length;
        int minBit = idx & 0x1f;

        for (int arrayIdx = idx >> 5; arrayIdx < len; arrayIdx++) {
            int word = bits[arrayIdx];
            if (word != 0) {
                int bitIdx = findFirst(word, minBit);
                if (bitIdx >= 0) {
                    return (arrayIdx << 5) + bitIdx;
                }
            }
            minBit = 0;
        }

        return -1;
    }

    /**
     * Finds the lowest-order bit set at or after the given index in the
     * given {@code int}.
     *
     * @param value the value in question
     * @param idx 0..31 the minimum bit index to return
     * @return {@code >= -1;} lowest-order bit set at or after {@code idx},
     * or {@code -1} if there is no appropriate bit index to return
     */
    public static int findFirst(int value, int idx) {
        value &= ~((1 << idx) - 1); // Mask off too-low bits.
        int result = Integer.numberOfTrailingZeros(value);
        return (result == 32) ? -1 : result;
    }

    /**
     * Ors bit array {@code b} into bit array {@code a}.
     * {@code a.length} must be greater than or equal to
     * {@code b.length}.
     *
     * @param a {@code non-null;} int array to be ored with other argument. This
     * argument is modified.
     * @param b {@code non-null;} int array to be ored into {@code a}. This
     * argument is not modified.
     */
    public static void or(int[] a, int[] b) {
        for (int i = 0; i < b.length; i++) {
            a[i] |= b[i];
        }
    }

    public static String toHuman(int[] bits) {
        StringBuilder sb = new StringBuilder();

        boolean needsComma = false;

        sb.append('{');

        int bitsLength = 32 * bits.length;
        for (int i = 0; i < bitsLength; i++) {
            if (Bits.get(bits, i)) {
                if (needsComma) {
                    sb.append(',');
                }
                needsComma = true;
                sb.append(i);
            }
        }
        sb.append('}');

        return sb.toString();
    }
}
