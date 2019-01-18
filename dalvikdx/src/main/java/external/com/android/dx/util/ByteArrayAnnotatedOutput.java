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

import external.com.android.dex.Leb128;
import external.com.android.dex.util.ByteOutput;
import external.com.android.dex.util.ExceptionWithContext;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implementation of {@link AnnotatedOutput} which stores the written data
 * into a {@code byte[]}.
 *
 * <p><b>Note:</b> As per the {@link Output} interface, multi-byte
 * writes all use little-endian order.</p>
 */
public final class ByteArrayAnnotatedOutput
        implements AnnotatedOutput, ByteOutput {
    /** default size for stretchy instances */
    private static final int DEFAULT_SIZE = 1000;

    /**
     * whether the instance is stretchy, that is, whether its array
     * may be resized to increase capacity
     */
    private final boolean stretchy;

    /** {@code non-null;} the data itself */
    private byte[] data;

    /** {@code >= 0;} current output cursor */
    private int cursor;

    /** whether annotations are to be verbose */
    private boolean verbose;

    /**
     * {@code null-ok;} list of annotations, or {@code null} if this instance
     * isn't keeping them
     */
    private ArrayList<Annotation> annotations;

    /** {@code >= 40 (if used);} the desired maximum annotation width */
    private int annotationWidth;

    /**
     * {@code >= 8 (if used);} the number of bytes of hex output to use
     * in annotations
     */
    private int hexCols;

    /**
     * Constructs an instance with a fixed maximum size. Note that the
     * given array is the only one that will be used to store data. In
     * particular, no reallocation will occur in order to expand the
     * capacity of the resulting instance. Also, the constructed
     * instance does not keep annotations by default.
     *
     * @param data {@code non-null;} data array to use for output
     */
    public ByteArrayAnnotatedOutput(byte[] data) {
        this(data, false);
    }

    /**
     * Constructs a "stretchy" instance. The underlying array may be
     * reallocated. The constructed instance does not keep annotations
     * by default.
     */
    public ByteArrayAnnotatedOutput() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructs a "stretchy" instance with initial size {@code size}. The
     * underlying array may be reallocated. The constructed instance does not
     * keep annotations by default.
     */
    public ByteArrayAnnotatedOutput(int size) {
        this(new byte[size], true);
    }

    /**
     * Internal constructor.
     *
     * @param data {@code non-null;} data array to use for output
     * @param stretchy whether the instance is to be stretchy
     */
    private ByteArrayAnnotatedOutput(byte[] data, boolean stretchy) {
        if (data == null) {
            throw new NullPointerException("data == null");
        }

        this.stretchy = stretchy;
        this.data = data;
        this.cursor = 0;
        this.verbose = false;
        this.annotations = null;
        this.annotationWidth = 0;
        this.hexCols = 0;
    }

    /**
     * Gets the underlying {@code byte[]} of this instance, which
     * may be larger than the number of bytes written
     *
     * @see #toByteArray
     *
     * @return {@code non-null;} the {@code byte[]}
     */
    public byte[] getArray() {
        return data;
    }

    /**
     * Constructs and returns a new {@code byte[]} that contains
     * the written contents exactly (that is, with no extra unwritten
     * bytes at the end).
     *
     * @see #getArray
     *
     * @return {@code non-null;} an appropriately-constructed array
     */
    public byte[] toByteArray() {
        byte[] result = new byte[cursor];
        System.arraycopy(data, 0, result, 0, cursor);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int getCursor() {
        return cursor;
    }

    /** {@inheritDoc} */
    @Override
    public void assertCursor(int expectedCursor) {
        if (cursor != expectedCursor) {
            throw new ExceptionWithContext("expected cursor " +
                    expectedCursor + "; actual value: " + cursor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeByte(int value) {
        int writeAt = cursor;
        int end = writeAt + 1;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        data[writeAt] = (byte) value;
        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void writeShort(int value) {
        int writeAt = cursor;
        int end = writeAt + 2;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        data[writeAt] = (byte) value;
        data[writeAt + 1] = (byte) (value >> 8);
        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void writeInt(int value) {
        int writeAt = cursor;
        int end = writeAt + 4;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        data[writeAt] = (byte) value;
        data[writeAt + 1] = (byte) (value >> 8);
        data[writeAt + 2] = (byte) (value >> 16);
        data[writeAt + 3] = (byte) (value >> 24);
        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void writeLong(long value) {
        int writeAt = cursor;
        int end = writeAt + 8;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        int half = (int) value;
        data[writeAt] = (byte) half;
        data[writeAt + 1] = (byte) (half >> 8);
        data[writeAt + 2] = (byte) (half >> 16);
        data[writeAt + 3] = (byte) (half >> 24);

        half = (int) (value >> 32);
        data[writeAt + 4] = (byte) half;
        data[writeAt + 5] = (byte) (half >> 8);
        data[writeAt + 6] = (byte) (half >> 16);
        data[writeAt + 7] = (byte) (half >> 24);

        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public int writeUleb128(int value) {
        if (stretchy) {
            ensureCapacity(cursor + 5); // pessimistic
        }
        int cursorBefore = cursor;
        Leb128.writeUnsignedLeb128(this, value);
        return (cursor - cursorBefore);
    }

    /** {@inheritDoc} */
    @Override
    public int writeSleb128(int value) {
        if (stretchy) {
            ensureCapacity(cursor + 5); // pessimistic
        }
        int cursorBefore = cursor;
        Leb128.writeSignedLeb128(this, value);
        return (cursor - cursorBefore);
    }

    /** {@inheritDoc} */
    @Override
    public void write(ByteArray bytes) {
        int blen = bytes.size();
        int writeAt = cursor;
        int end = writeAt + blen;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        bytes.getBytes(data, writeAt);
        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte[] bytes, int offset, int length) {
        int writeAt = cursor;
        int end = writeAt + length;
        int bytesEnd = offset + length;

        // twos-complement math trick: ((x < 0) || (y < 0)) <=> ((x|y) < 0)
        if (((offset | length | end) < 0) || (bytesEnd > bytes.length)) {
            throw new IndexOutOfBoundsException("bytes.length " +
                                                bytes.length + "; " +
                                                offset + "..!" + end);
        }

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        System.arraycopy(bytes, offset, data, writeAt, length);
        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    /** {@inheritDoc} */
    @Override
    public void writeZeroes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }

        int end = cursor + count;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        /*
         * We need to write zeroes, since the array might be reused across different dx invocations.
         */
        Arrays.fill(data, cursor, end, (byte) 0);

        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public void alignTo(int alignment) {
        int mask = alignment - 1;

        if ((alignment < 0) || ((mask & alignment) != 0)) {
            throw new IllegalArgumentException("bogus alignment");
        }

        int end = (cursor + mask) & ~mask;

        if (stretchy) {
            ensureCapacity(end);
        } else if (end > data.length) {
            throwBounds();
            return;
        }

        /*
         * We need to write zeroes, since the array might be reused across different dx invocations.
         */
        Arrays.fill(data, cursor, end, (byte) 0);

        cursor = end;
    }

    /** {@inheritDoc} */
    @Override
    public boolean annotates() {
        return (annotations != null);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVerbose() {
        return verbose;
    }

    /** {@inheritDoc} */
    @Override
    public void annotate(String msg) {
        if (annotations == null) {
            return;
        }

        endAnnotation();
        annotations.add(new Annotation(cursor, msg));
    }

    /** {@inheritDoc} */
    @Override
    public void annotate(int amt, String msg) {
        if (annotations == null) {
            return;
        }

        endAnnotation();

        int asz = annotations.size();
        int lastEnd = (asz == 0) ? 0 : annotations.get(asz - 1).getEnd();
        int startAt;

        if (lastEnd <= cursor) {
            startAt = cursor;
        } else {
            startAt = lastEnd;
        }

        annotations.add(new Annotation(startAt, startAt + amt, msg));
    }

    /** {@inheritDoc} */
    @Override
    public void endAnnotation() {
        if (annotations == null) {
            return;
        }

        int sz = annotations.size();

        if (sz != 0) {
            annotations.get(sz - 1).setEndIfUnset(cursor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getAnnotationWidth() {
        int leftWidth = 8 + (hexCols * 2) + (hexCols / 2);

        return annotationWidth - leftWidth;
    }

    /**
     * Indicates that this instance should keep annotations. This method may
     * be called only once per instance, and only before any data has been
     * written to the it.
     *
     * @param annotationWidth {@code >= 40;} the desired maximum annotation width
     * @param verbose whether or not to indicate verbose annotations
     */
    public void enableAnnotations(int annotationWidth, boolean verbose) {
        if ((annotations != null) || (cursor != 0)) {
            throw new RuntimeException("cannot enable annotations");
        }

        if (annotationWidth < 40) {
            throw new IllegalArgumentException("annotationWidth < 40");
        }

        int hexCols = (((annotationWidth - 7) / 15) + 1) & ~1;
        if (hexCols < 6) {
            hexCols = 6;
        } else if (hexCols > 10) {
            hexCols = 10;
        }

        this.annotations = new ArrayList<Annotation>(1000);
        this.annotationWidth = annotationWidth;
        this.hexCols = hexCols;
        this.verbose = verbose;
    }

    /**
     * Finishes up annotation processing. This closes off any open
     * annotations and removes annotations that don't refer to written
     * data.
     */
    public void finishAnnotating() {
        // Close off the final annotation, if any.
        endAnnotation();

        // Remove annotations that refer to unwritten data.
        if (annotations != null) {
            int asz = annotations.size();
            while (asz > 0) {
                Annotation last = annotations.get(asz - 1);
                if (last.getStart() > cursor) {
                    annotations.remove(asz - 1);
                    asz--;
                } else if (last.getEnd() > cursor) {
                    last.setEnd(cursor);
                    break;
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Writes the annotated content of this instance to the given writer.
     *
     * @param out {@code non-null;} where to write to
     */
    public void writeAnnotationsTo(Writer out) throws IOException {
        int width2 = getAnnotationWidth();
        int width1 = annotationWidth - width2 - 1;

        TwoColumnOutput twoc = new TwoColumnOutput(out, width1, width2, "|");
        Writer left = twoc.getLeft();
        Writer right = twoc.getRight();
        int leftAt = 0; // left-hand byte output cursor
        int rightAt = 0; // right-hand annotation index
        int rightSz = annotations.size();

        while ((leftAt < cursor) && (rightAt < rightSz)) {
            Annotation a = annotations.get(rightAt);
            int start = a.getStart();
            int end;
            String text;

            if (leftAt < start) {
                // This is an area with no annotation.
                end = start;
                start = leftAt;
                text = "";
            } else {
                // This is an area with an annotation.
                end = a.getEnd();
                text = a.getText();
                rightAt++;
            }

            left.write(Hex.dump(data, start, end - start, start, hexCols, 6));
            right.write(text);
            twoc.flush();
            leftAt = end;
        }

        if (leftAt < cursor) {
            // There is unannotated output at the end.
            left.write(Hex.dump(data, leftAt, cursor - leftAt, leftAt,
                                hexCols, 6));
        }

        while (rightAt < rightSz) {
            // There are zero-byte annotations at the end.
            right.write(annotations.get(rightAt).getText());
            rightAt++;
        }

        twoc.flush();
    }

    /**
     * Throws the excpetion for when an attempt is made to write past the
     * end of the instance.
     */
    private static void throwBounds() {
        throw new IndexOutOfBoundsException("attempt to write past the end");
    }

    /**
     * Reallocates the underlying array if necessary. Calls to this method
     * should be guarded by a test of {@link #stretchy}.
     *
     * @param desiredSize {@code >= 0;} the desired minimum total size of the array
     */
    private void ensureCapacity(int desiredSize) {
        if (data.length < desiredSize) {
            byte[] newData = new byte[desiredSize * 2 + 1000];
            System.arraycopy(data, 0, newData, 0, cursor);
            data = newData;
        }
    }

    /**
     * Annotation on output.
     */
    private static class Annotation {
        /** {@code >= 0;} start of annotated range (inclusive) */
        private final int start;

        /**
         * {@code >= 0;} end of annotated range (exclusive);
         * {@code Integer.MAX_VALUE} if unclosed
         */
        private int end;

        /** {@code non-null;} annotation text */
        private final String text;

        /**
         * Constructs an instance.
         *
         * @param start {@code >= 0;} start of annotated range
         * @param end {@code >= start;} end of annotated range (exclusive) or
         * {@code Integer.MAX_VALUE} if unclosed
         * @param text {@code non-null;} annotation text
         */
        public Annotation(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }

        /**
         * Constructs an instance. It is initally unclosed.
         *
         * @param start {@code >= 0;} start of annotated range
         * @param text {@code non-null;} annotation text
         */
        public Annotation(int start, String text) {
            this(start, Integer.MAX_VALUE, text);
        }

        /**
         * Sets the end as given, but only if the instance is unclosed;
         * otherwise, do nothing.
         *
         * @param end {@code >= start;} the end
         */
        public void setEndIfUnset(int end) {
            if (this.end == Integer.MAX_VALUE) {
                this.end = end;
            }
        }

        /**
         * Sets the end as given.
         *
         * @param end {@code >= start;} the end
         */
        public void setEnd(int end) {
            this.end = end;
        }

        /**
         * Gets the start.
         *
         * @return the start
         */
        public int getStart() {
            return start;
        }

        /**
         * Gets the end.
         *
         * @return the end
         */
        public int getEnd() {
            return end;
        }

        /**
         * Gets the text.
         *
         * @return {@code non-null;} the text
         */
        public String getText() {
            return text;
        }
    }
}
