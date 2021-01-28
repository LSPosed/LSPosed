/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx.io.instructions;

/**
 * Implementation of {@code CodeOutput} that writes to a {@code short[]}.
 */
public final class ShortArrayCodeOutput extends BaseCodeCursor
        implements CodeOutput {
    /** array to write to */
    private final short[] array;

    /**
     * Constructs an instance.
     *
     * @param maxSize the maximum number of code units that will be written
     */
    public ShortArrayCodeOutput(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize < 0");
        }

        this.array = new short[maxSize];
    }

    /**
     * Gets the array. The returned array contains exactly the data
     * written (e.g. no leftover space at the end).
     */
    public short[] getArray() {
        int cursor = cursor();

        if (cursor == array.length) {
            return array;
        }

        short[] result = new short[cursor];
        System.arraycopy(array, 0, result, 0, cursor);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void write(short codeUnit) {
        array[cursor()] = codeUnit;
        advance(1);
    }

    /** {@inheritDoc} */
    @Override
    public void write(short u0, short u1) {
        write(u0);
        write(u1);
    }

    /** {@inheritDoc} */
    @Override
    public void write(short u0, short u1, short u2) {
        write(u0);
        write(u1);
        write(u2);
    }

    /** {@inheritDoc} */
    @Override
    public void write(short u0, short u1, short u2, short u3) {
        write(u0);
        write(u1);
        write(u2);
        write(u3);
    }

    /** {@inheritDoc} */
    @Override
    public void write(short u0, short u1, short u2, short u3, short u4) {
        write(u0);
        write(u1);
        write(u2);
        write(u3);
        write(u4);
    }

    /** {@inheritDoc} */
    @Override
    public void writeInt(int value) {
        write((short) value);
        write((short) (value >> 16));
    }

    /** {@inheritDoc} */
    @Override
    public void writeLong(long value) {
        write((short) value);
        write((short) (value >> 16));
        write((short) (value >> 32));
        write((short) (value >> 48));
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte[] data) {
        int value = 0;
        boolean even = true;
        for (byte b : data) {
            if (even) {
                value = b & 0xff;
                even = false;
            } else {
                value |= b << 8;
                write((short) value);
                even = true;
            }
        }

        if (!even) {
            write((short) value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(short[] data) {
        for (short unit : data) {
            write(unit);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(int[] data) {
        for (int i : data) {
            writeInt(i);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(long[] data) {
        for (long l : data) {
            writeLong(l);
        }
    }
}
