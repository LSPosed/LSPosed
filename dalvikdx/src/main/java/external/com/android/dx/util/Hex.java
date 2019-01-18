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
 * Utilities for formatting numbers as hexadecimal.
 */
public final class Hex {
    /**
     * This class is uninstantiable.
     */
    private Hex() {
        // This space intentionally left blank.
    }

    /**
     * Formats a {@code long} as an 8-byte unsigned hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u8(long v) {
        char[] result = new char[16];
        for (int i = 0; i < 16; i++) {
            result[15 - i] = Character.forDigit((int) v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 4-byte unsigned hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u4(int v) {
        char[] result = new char[8];
        for (int i = 0; i < 8; i++) {
            result[7 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 3-byte unsigned hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u3(int v) {
        char[] result = new char[6];
        for (int i = 0; i < 6; i++) {
            result[5 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 2-byte unsigned hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u2(int v) {
        char[] result = new char[4];
        for (int i = 0; i < 4; i++) {
            result[3 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as either a 2-byte unsigned hex value
     * (if the value is small enough) or a 4-byte unsigned hex value (if
     * not).
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u2or4(int v) {
        if (v == (char) v) {
            return u2(v);
        } else {
            return u4(v);
        }
    }

    /**
     * Formats an {@code int} as a 1-byte unsigned hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String u1(int v) {
        char[] result = new char[2];
        for (int i = 0; i < 2; i++) {
            result[1 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 4-bit unsigned hex nibble.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String uNibble(int v) {
        char[] result = new char[1];

        result[0] = Character.forDigit(v & 0x0f, 16);
        return new String(result);
    }

    /**
     * Formats a {@code long} as an 8-byte signed hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String s8(long v) {
        char[] result = new char[17];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 16; i++) {
            result[16 - i] = Character.forDigit((int) v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 4-byte signed hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String s4(int v) {
        char[] result = new char[9];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 8; i++) {
            result[8 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 2-byte signed hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String s2(int v) {
        char[] result = new char[5];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 4; i++) {
            result[4 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an {@code int} as a 1-byte signed hex value.
     *
     * @param v value to format
     * @return {@code non-null;} formatted form
     */
    public static String s1(int v) {
        char[] result = new char[3];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 2; i++) {
            result[2 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats a hex dump of a portion of a {@code byte[]}. The result
     * is always newline-terminated, unless the passed-in length was zero,
     * in which case the result is always the empty string ({@code ""}).
     *
     * @param arr {@code non-null;} array to format
     * @param offset {@code >= 0;} offset to the part to dump
     * @param length {@code >= 0;} number of bytes to dump
     * @param outOffset {@code >= 0;} first output offset to print
     * @param bpl {@code >= 0;} number of bytes of output per line
     * @param addressLength {@code {2,4,6,8};} number of characters for each address
     * header
     * @return {@code non-null;} a string of the dump
     */
    public static String dump(byte[] arr, int offset, int length,
                              int outOffset, int bpl, int addressLength) {
        int end = offset + length;

        // twos-complement math trick: ((x < 0) || (y < 0)) <=> ((x|y) < 0)
        if (((offset | length | end) < 0) || (end > arr.length)) {
            throw new IndexOutOfBoundsException("arr.length " +
                                                arr.length + "; " +
                                                offset + "..!" + end);
        }

        if (outOffset < 0) {
            throw new IllegalArgumentException("outOffset < 0");
        }

        if (length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(length * 4 + 6);
        boolean bol = true;
        int col = 0;

        while (length > 0) {
            if (col == 0) {
                String astr;
                switch (addressLength) {
                    case 2:  astr = Hex.u1(outOffset); break;
                    case 4:  astr = Hex.u2(outOffset); break;
                    case 6:  astr = Hex.u3(outOffset); break;
                    default: astr = Hex.u4(outOffset); break;
                }
                sb.append(astr);
                sb.append(": ");
            } else if ((col & 1) == 0) {
                sb.append(' ');
            }
            sb.append(Hex.u1(arr[offset]));
            outOffset++;
            offset++;
            col++;
            if (col == bpl) {
                sb.append('\n');
                col = 0;
            }
            length--;
        }

        if (col != 0) {
            sb.append('\n');
        }

        return sb.toString();
    }
}
