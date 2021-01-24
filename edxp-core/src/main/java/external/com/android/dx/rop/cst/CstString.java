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

import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;

/**
 * Constants of type {@code CONSTANT_Utf8_info} or {@code CONSTANT_String_info}.
 */
public final class CstString extends TypedConstant {
    /**
     * {@code non-null;} instance representing {@code ""}, that is, the
     * empty string
     */
    public static final CstString EMPTY_STRING = new CstString("");

    /** {@code non-null;} the UTF-8 value as a string */
    private final String string;

    /** {@code non-null;} the UTF-8 value as bytes */
    private final ByteArray bytes;

    /**
     * Converts a string into its MUTF-8 form. MUTF-8 differs from normal UTF-8
     * in the handling of character '\0' and surrogate pairs.
     *
     * @param string {@code non-null;} the string to convert
     * @return {@code non-null;} the UTF-8 bytes for it
     */
    public static byte[] stringToUtf8Bytes(String string) {
        int len = string.length();
        byte[] bytes = new byte[len * 3]; // Avoid having to reallocate.
        int outAt = 0;

        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if ((c != 0) && (c < 0x80)) {
                bytes[outAt] = (byte) c;
                outAt++;
            } else if (c < 0x800) {
                bytes[outAt] = (byte) (((c >> 6) & 0x1f) | 0xc0);
                bytes[outAt + 1] = (byte) ((c & 0x3f) | 0x80);
                outAt += 2;
            } else {
                bytes[outAt] = (byte) (((c >> 12) & 0x0f) | 0xe0);
                bytes[outAt + 1] = (byte) (((c >> 6) & 0x3f) | 0x80);
                bytes[outAt + 2] = (byte) ((c & 0x3f) | 0x80);
                outAt += 3;
            }
        }

        byte[] result = new byte[outAt];
        System.arraycopy(bytes, 0, result, 0, outAt);
        return result;
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes {@code non-null;} the bytes to convert
     * @return {@code non-null;} the converted string
     */
    public static String utf8BytesToString(ByteArray bytes) {
        int length = bytes.size();
        char[] chars = new char[length]; // This is sized to avoid a realloc.
        int outAt = 0;

        for (int at = 0; length > 0; /*at*/) {
            int v0 = bytes.getUnsignedByte(at);
            char out;
            switch (v0 >> 4) {
                case 0x00: case 0x01: case 0x02: case 0x03:
                case 0x04: case 0x05: case 0x06: case 0x07: {
                    // 0XXXXXXX -- single-byte encoding
                    length--;
                    if (v0 == 0) {
                        // A single zero byte is illegal.
                        return throwBadUtf8(v0, at);
                    }
                    out = (char) v0;
                    at++;
                    break;
                }
                case 0x0c: case 0x0d: {
                    // 110XXXXX -- two-byte encoding
                    length -= 2;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    int v1 = bytes.getUnsignedByte(at + 1);
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int value = ((v0 & 0x1f) << 6) | (v1 & 0x3f);
                    if ((value != 0) && (value < 0x80)) {
                        /*
                         * This should have been represented with
                         * one-byte encoding.
                         */
                        return throwBadUtf8(v1, at + 1);
                    }
                    out = (char) value;
                    at += 2;
                    break;
                }
                case 0x0e: {
                    // 1110XXXX -- three-byte encoding
                    length -= 3;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    int v1 = bytes.getUnsignedByte(at + 1);
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int v2 = bytes.getUnsignedByte(at + 2);
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v2, at + 2);
                    }
                    int value = ((v0 & 0x0f) << 12) | ((v1 & 0x3f) << 6) |
                        (v2 & 0x3f);
                    if (value < 0x800) {
                        /*
                         * This should have been represented with one- or
                         * two-byte encoding.
                         */
                        return throwBadUtf8(v2, at + 2);
                    }
                    out = (char) value;
                    at += 3;
                    break;
                }
                default: {
                    // 10XXXXXX, 1111XXXX -- illegal
                    return throwBadUtf8(v0, at);
                }
            }
            chars[outAt] = out;
            outAt++;
        }

        return new String(chars, 0, outAt);
    }

    /**
     * Helper for {@link #utf8BytesToString}, which throws the right
     * exception for a bogus utf-8 byte.
     *
     * @param value the byte value
     * @param offset the file offset
     * @return never
     * @throws IllegalArgumentException always thrown
     */
    private static String throwBadUtf8(int value, int offset) {
        throw new IllegalArgumentException("bad utf-8 byte " + Hex.u1(value) +
                                           " at offset " + Hex.u4(offset));
    }

    /**
     * Constructs an instance from a {@code String}.
     *
     * @param string {@code non-null;} the UTF-8 value as a string
     */
    public CstString(String string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }

        this.string = string.intern();
        this.bytes = new ByteArray(stringToUtf8Bytes(string));
    }

    /**
     * Constructs an instance from some UTF-8 bytes.
     *
     * @param bytes {@code non-null;} array of the UTF-8 bytes
     */
    public CstString(ByteArray bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }

        this.bytes = bytes;
        this.string = utf8BytesToString(bytes).intern();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CstString)) {
            return false;
        }

        return string.equals(((CstString) other).string);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return string.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        return string.compareTo(((CstString) other).string);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "string{\"" + toHuman() + "\"}";
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "utf8";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        int len = string.length();
        StringBuilder sb = new StringBuilder(len * 3 / 2);

        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if ((c >= ' ') && (c < 0x7f)) {
                if ((c == '\'') || (c == '\"') || (c == '\\')) {
                    sb.append('\\');
                }
                sb.append(c);
            } else if (c <= 0x7f) {
                switch (c) {
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default: {
                        /*
                         * Represent the character as an octal escape.
                         * If the next character is a valid octal
                         * digit, disambiguate by using the
                         * three-digit form.
                         */
                        char nextChar =
                            (i < (len - 1)) ? string.charAt(i + 1) : 0;
                        boolean displayZero =
                            (nextChar >= '0') && (nextChar <= '7');
                        sb.append('\\');
                        for (int shift = 6; shift >= 0; shift -= 3) {
                            char outChar = (char) (((c >> shift) & 7) + '0');
                            if ((outChar != '0') || displayZero) {
                                sb.append(outChar);
                                displayZero = true;
                            }
                        }
                        if (! displayZero) {
                            // Ironic edge case: The original value was 0.
                            sb.append('0');
                        }
                        break;
                    }
                }
            } else {
                sb.append("\\u");
                sb.append(Character.forDigit(c >> 12, 16));
                sb.append(Character.forDigit((c >> 8) & 0x0f, 16));
                sb.append(Character.forDigit((c >> 4) & 0x0f, 16));
                sb.append(Character.forDigit(c & 0x0f, 16));
            }
        }

        return sb.toString();
    }

    /**
     * Gets the value as a human-oriented string, surrounded by double
     * quotes.
     *
     * @return {@code non-null;} the quoted string
     */
    public String toQuoted() {
        return '\"' + toHuman() + '\"';
    }

    /**
     * Gets the value as a human-oriented string, surrounded by double
     * quotes, but ellipsizes the result if it is longer than the given
     * maximum length
     *
     * @param maxLength {@code >= 5;} the maximum length of the string to return
     * @return {@code non-null;} the quoted string
     */
    public String toQuoted(int maxLength) {
        String string = toHuman();
        int length = string.length();
        String ellipses;

        if (length <= (maxLength - 2)) {
            ellipses = "";
        } else {
            string = string.substring(0, maxLength - 5);
            ellipses = "...";
        }

        return '\"' + string + ellipses + '\"';
    }

    /**
     * Gets the UTF-8 value as a string.
     * The returned string is always already interned.
     *
     * @return {@code non-null;} the UTF-8 value as a string
     */
    public String getString() {
        return string;
    }

    /**
     * Gets the UTF-8 value as UTF-8 encoded bytes.
     *
     * @return {@code non-null;} an array of the UTF-8 bytes
     */
    public ByteArray getBytes() {
        return bytes;
    }

    /**
     * Gets the size of this instance as UTF-8 code points. That is,
     * get the number of bytes in the UTF-8 encoding of this instance.
     *
     * @return {@code >= 0;} the UTF-8 size
     */
    public int getUtf8Size() {
        return bytes.size();
    }

    /**
     * Gets the size of this instance as UTF-16 code points. That is,
     * get the number of 16-bit chars in the UTF-16 encoding of this
     * instance. This is the same as the {@code length} of the
     * Java {@code String} representation of this instance.
     *
     * @return {@code >= 0;} the UTF-16 size
     */
    public int getUtf16Size() {
        return string.length();
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }
}
