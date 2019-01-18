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

import external.com.android.dex.util.ByteOutput;

/**
 * Interface for a sink for binary output. This is similar to
 * {@code java.util.DataOutput}, but no {@code IOExceptions}
 * are declared, and multibyte output is defined to be little-endian.
 */
public interface Output extends ByteOutput {
    /**
     * Gets the current cursor position. This is the same as the number of
     * bytes written to this instance.
     *
     * @return {@code >= 0;} the cursor position
     */
    public int getCursor();

    /**
     * Asserts that the cursor is the given value.
     *
     * @param expectedCursor the expected cursor value
     * @throws RuntimeException thrown if {@code getCursor() !=
     * expectedCursor}
     */
    public void assertCursor(int expectedCursor);

    /**
     * Writes a {@code byte} to this instance.
     *
     * @param value the value to write; all but the low 8 bits are ignored
     */
    @Override
    public void writeByte(int value);

    /**
     * Writes a {@code short} to this instance.
     *
     * @param value the value to write; all but the low 16 bits are ignored
     */
    public void writeShort(int value);

    /**
     * Writes an {@code int} to this instance.
     *
     * @param value the value to write
     */
    public void writeInt(int value);

    /**
     * Writes a {@code long} to this instance.
     *
     * @param value the value to write
     */
    public void writeLong(long value);

    /**
     * Writes a DWARFv3-style unsigned LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @param value value to write, treated as an unsigned value
     * @return {@code 1..5;} the number of bytes actually written
     */
    public int writeUleb128(int value);

    /**
     * Writes a DWARFv3-style unsigned LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @param value value to write
     * @return {@code 1..5;} the number of bytes actually written
     */
    public int writeSleb128(int value);

    /**
     * Writes a {@link ByteArray} to this instance.
     *
     * @param bytes {@code non-null;} the array to write
     */
    public void write(ByteArray bytes);

    /**
     * Writes a portion of a {@code byte[]} to this instance.
     *
     * @param bytes {@code non-null;} the array to write
     * @param offset {@code >= 0;} offset into {@code bytes} for the first
     * byte to write
     * @param length {@code >= 0;} number of bytes to write
     */
    public void write(byte[] bytes, int offset, int length);

    /**
     * Writes a {@code byte[]} to this instance. This is just
     * a convenient shorthand for {@code write(bytes, 0, bytes.length)}.
     *
     * @param bytes {@code non-null;} the array to write
     */
    public void write(byte[] bytes);

    /**
     * Writes the given number of {@code 0} bytes.
     *
     * @param count {@code >= 0;} the number of zeroes to write
     */
    public void writeZeroes(int count);

    /**
     * Adds extra bytes if necessary (with value {@code 0}) to
     * force alignment of the output cursor as given.
     *
     * @param alignment {@code > 0;} the alignment; must be a power of two
     */
    public void alignTo(int alignment);
}
