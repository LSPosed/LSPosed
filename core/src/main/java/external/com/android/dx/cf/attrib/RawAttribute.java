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

package external.com.android.dx.cf.attrib;

import external.com.android.dx.rop.cst.ConstantPool;
import external.com.android.dx.util.ByteArray;

/**
 * Raw attribute, for holding onto attributes that are unrecognized.
 */
public final class RawAttribute extends BaseAttribute {
    /** {@code non-null;} attribute data */
    private final ByteArray data;

    /**
     * {@code null-ok;} constant pool to use for resolution of cpis in {@link
     * #data}
     */
    private final ConstantPool pool;

    /**
     * Constructs an instance.
     *
     * @param name {@code non-null;} attribute name
     * @param data {@code non-null;} attribute data
     * @param pool {@code null-ok;} constant pool to use for cpi resolution
     */
    public RawAttribute(String name, ByteArray data, ConstantPool pool) {
        super(name);

        if (data == null) {
            throw new NullPointerException("data == null");
        }

        this.data = data;
        this.pool = pool;
    }

    /**
     * Constructs an instance from a sub-array of a {@link ByteArray}.
     *
     * @param name {@code non-null;} attribute name
     * @param data {@code non-null;} array containing the attribute data
     * @param offset offset in {@code data} to the attribute data
     * @param length length of the attribute data, in bytes
     * @param pool {@code null-ok;} constant pool to use for cpi resolution
     */
    public RawAttribute(String name, ByteArray data, int offset,
                        int length, ConstantPool pool) {
        this(name, data.slice(offset, offset + length), pool);
    }

    /**
     * Get the raw data of the attribute.
     *
     * @return {@code non-null;} the data
     */
    public ByteArray getData() {
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return data.size() + 6;
    }

    /**
     * Gets the constant pool to use for cpi resolution, if any. It
     * presumably came from the class file that this attribute came
     * from.
     *
     * @return {@code null-ok;} the constant pool
     */
    public ConstantPool getPool() {
        return pool;
    }
}
