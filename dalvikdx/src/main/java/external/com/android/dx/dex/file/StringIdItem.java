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

package external.com.android.dx.dex.file;

import external.com.android.dex.SizeOf;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;

/**
 * Representation of a string inside a Dalvik file.
 */
public final class StringIdItem
        extends IndexedItem implements Comparable {
    /** {@code non-null;} the string value */
    private final CstString value;

    /** {@code null-ok;} associated string data object, if known */
    private StringDataItem data;

    /**
     * Constructs an instance.
     *
     * @param value {@code non-null;} the string value
     */
    public StringIdItem(CstString value) {
        if (value == null) {
            throw new NullPointerException("value == null");
        }

        this.value = value;
        this.data = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StringIdItem)) {
            return false;
        }

        StringIdItem otherString = (StringIdItem) other;
        return value.equals(otherString.value);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Object other) {
        StringIdItem otherString = (StringIdItem) other;
        return value.compareTo(otherString.value);
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_STRING_ID_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public int writeSize() {
        return SizeOf.STRING_ID_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        if (data == null) {
            // The string data hasn't yet been added, so add it.
            MixedItemSection stringData = file.getStringData();
            data = new StringDataItem(value);
            stringData.add(data);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int dataOff = data.getAbsoluteOffset();

        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + value.toQuoted(100));
            out.annotate(4, "  string_data_off: " + Hex.u4(dataOff));
        }

        out.writeInt(dataOff);
    }

    /**
     * Gets the string value.
     *
     * @return {@code non-null;} the value
     */
    public CstString getValue() {
        return value;
    }

    /**
     * Gets the associated data object for this instance, if known.
     *
     * @return {@code null-ok;} the associated data object or {@code null}
     * if not yet known
     */
    public StringDataItem getData() {
        return data;
    }
}
