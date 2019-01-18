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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Wrapper for a {@code byte[]}, which provides read-only access and
 * can "reveal" a partial slice of the underlying array.
 *
 * <b>Note:</b> Multibyte accessors all use big-endian order.
 */
public final class ByteArray {
    /** {@code non-null;} underlying array */
    private final byte[] bytes;

    /** {@code >= 0}; start index of the slice (inclusive) */
    private final int start;

    /** {@code >= 0, <= bytes.length}; size computed as
     * {@code end - start} (in the constructor) */
    private final int size;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the underlying array
     * @param start {@code >= 0;} start index of the slice (inclusive)
     * @param end {@code >= start, <= bytes.length;} end index of
     * the slice (exclusive)
     */
    public ByteArray(byte[] bytes, int start, int end) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }

        if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        }

        if (end < start) {
            throw new IllegalArgumentException("end < start");
        }

        if (end > bytes.length) {
            throw new IllegalArgumentException("end > bytes.length");
        }

        this.bytes = bytes;
        this.start = start;
        this.size = end - start;
    }

    /**
     * Constructs an instance from an entire {@code byte[]}.
     *
     * @param bytes {@code non-null;} the underlying array
     */
    public ByteArray(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Gets the size of the array, in bytes.
     *
     * @return {@code >= 0;} the size
     */
    public int size() {
        return size;
    }

    /**
     * Returns a slice (that is, a sub-array) of this instance.
     *
     * @param start {@code >= 0;} start index of the slice (inclusive)
     * @param end {@code >= start, <= size();} end index of
     * the slice (exclusive)
     * @return {@code non-null;} the slice
     */
    public ByteArray slice(int start, int end) {
        checkOffsets(start, end);
        byte[] slicedOut = Arrays.copyOfRange(bytes, start, end);
        return new ByteArray(slicedOut);
    }

    /**
     * Returns the offset into the given array represented by the given
     * offset into this instance.
     *
     * @param offset offset into this instance
     * @return corresponding offset into {@code bytes}
     * @throws IllegalArgumentException thrown if {@code bytes} is
     * not the underlying array of this instance
     */
    public int underlyingOffset(int offset) {
        return start + offset;
    }

    /**
     * Gets the {@code signed byte} value at a particular offset.
     *
     * @param off {@code >= 0, < size();} offset to fetch
     * @return {@code signed byte} at that offset
     */
    public int getByte(int off) {
        checkOffsets(off, off + 1);
        return getByte0(off);
    }

    /**
     * Gets the {@code signed short} value at a particular offset.
     *
     * @param off {@code >= 0, < (size() - 1);} offset to fetch
     * @return {@code signed short} at that offset
     */
    public int getShort(int off) {
        checkOffsets(off, off + 2);
        return (getByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    /**
     * Gets the {@code signed int} value at a particular offset.
     *
     * @param off {@code >= 0, < (size() - 3);} offset to fetch
     * @return {@code signed int} at that offset
     */
    public int getInt(int off) {
        checkOffsets(off, off + 4);
        return (getByte0(off) << 24) |
            (getUnsignedByte0(off + 1) << 16) |
            (getUnsignedByte0(off + 2) << 8) |
            getUnsignedByte0(off + 3);
    }

    /**
     * Gets the {@code signed long} value at a particular offset.
     *
     * @param off {@code >= 0, < (size() - 7);} offset to fetch
     * @return {@code signed int} at that offset
     */
    public long getLong(int off) {
        checkOffsets(off, off + 8);
        int part1 = (getByte0(off) << 24) |
            (getUnsignedByte0(off + 1) << 16) |
            (getUnsignedByte0(off + 2) << 8) |
            getUnsignedByte0(off + 3);
        int part2 = (getByte0(off + 4) << 24) |
            (getUnsignedByte0(off + 5) << 16) |
            (getUnsignedByte0(off + 6) << 8) |
            getUnsignedByte0(off + 7);

        return (part2 & 0xffffffffL) | ((long) part1) << 32;
    }

    /**
     * Gets the {@code unsigned byte} value at a particular offset.
     *
     * @param off {@code >= 0, < size();} offset to fetch
     * @return {@code unsigned byte} at that offset
     */
    public int getUnsignedByte(int off) {
        checkOffsets(off, off + 1);
        return getUnsignedByte0(off);
    }

    /**
     * Gets the {@code unsigned short} value at a particular offset.
     *
     * @param off {@code >= 0, < (size() - 1);} offset to fetch
     * @return {@code unsigned short} at that offset
     */
    public int getUnsignedShort(int off) {
        checkOffsets(off, off + 2);
        return (getUnsignedByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    /**
     * Copies the contents of this instance into the given raw
     * {@code byte[]} at the given offset. The given array must be
     * large enough.
     *
     * @param out {@code non-null;} array to hold the output
     * @param offset {@code non-null;} index into {@code out} for the first
     * byte of output
     */
    public void getBytes(byte[] out, int offset) {
        if ((out.length - offset) < size) {
            throw new IndexOutOfBoundsException("(out.length - offset) < " +
                                                "size()");
        }

        System.arraycopy(bytes, start, out, offset, size);
    }

    /**
     * Checks a range of offsets for validity, throwing if invalid.
     *
     * @param s start offset (inclusive)
     * @param e end offset (exclusive)
     */
    private void checkOffsets(int s, int e) {
        if ((s < 0) || (e < s) || (e > size)) {
            throw new IllegalArgumentException("bad range: " + s + ".." + e +
                                               "; actual size " + size);
        }
    }

    /**
     * Gets the {@code signed byte} value at the given offset,
     * without doing any argument checking.
     *
     * @param off offset to fetch
     * @return byte at that offset
     */
    private int getByte0(int off) {
        return bytes[start + off];
    }

    /**
     * Gets the {@code unsigned byte} value at the given offset,
     * without doing any argument checking.
     *
     * @param off offset to fetch
     * @return byte at that offset
     */
    private int getUnsignedByte0(int off) {
        return bytes[start + off] & 0xff;
    }

    /**
     * Gets a {@code DataInputStream} that reads from this instance,
     * with the cursor starting at the beginning of this instance's data.
     * <b>Note:</b> The returned instance may be cast to {@link GetCursor}
     * if needed.
     *
     * @return {@code non-null;} an appropriately-constructed
     * {@code DataInputStream} instance
     */
    public MyDataInputStream makeDataInputStream() {
        return new MyDataInputStream(makeInputStream());
    }

    /**
     * Gets a {@code InputStream} that reads from this instance,
     * with the cursor starting at the beginning of this instance's data.
     * <b>Note:</b> The returned instance may be cast to {@link GetCursor}
     * if needed.
     *
     * @return {@code non-null;} an appropriately-constructed
     * {@code InputStream} instancex
     */
    public MyInputStream makeInputStream() {
        return new MyInputStream();
    }

    /**
     * Helper interface that allows one to get the cursor (of a stream).
     */
    public interface GetCursor {
        /**
         * Gets the current cursor.
         *
         * @return {@code 0..size();} the cursor
         */
        int getCursor();
    }

    /**
     * Helper class for {@link #makeInputStream}, which implements the
     * stream functionality.
     */
    public class MyInputStream extends InputStream {
        /** 0..size; the cursor */
        private int cursor;

        /** 0..size; the mark */
        private int mark;

        public MyInputStream() {
            cursor = 0;
            mark = 0;
        }

        @Override
        public int read() throws IOException {
            if (cursor >= size) {
                return -1;
            }

            int result = getUnsignedByte0(cursor);
            cursor++;
            return result;
        }

        @Override
        public int read(byte[] arr, int offset, int length) {
            if ((offset + length) > arr.length) {
                length = arr.length - offset;
            }

            int maxLength = size - cursor;
            if (length > maxLength) {
                length = maxLength;
            }

            System.arraycopy(bytes, cursor + start, arr, offset, length);
            cursor += length;
            return length;
        }

        @Override
        public int available() {
            return size - cursor;
        }

        @Override
        public void mark(int reserve) {
            mark = cursor;
        }

        @Override
        public void reset() {
            cursor = mark;
        }

        @Override
        public boolean markSupported() {
            return true;
        }
    }

    /**
     * Helper class for {@link #makeDataInputStream}. This is used
     * simply so that the cursor of a wrapped {@link MyInputStream}
     * instance may be easily determined.
     */
    public static class MyDataInputStream extends DataInputStream {
        /** {@code non-null;} the underlying {@link MyInputStream} */
        private final MyInputStream wrapped;

        public MyDataInputStream(MyInputStream wrapped) {
            super(wrapped);

            this.wrapped = wrapped;
        }
    }
}
