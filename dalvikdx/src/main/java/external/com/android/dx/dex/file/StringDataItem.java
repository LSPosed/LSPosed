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

import external.com.android.dex.Leb128;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;

/**
 * Representation of string data for a particular string, in a Dalvik file.
 */
public final class StringDataItem extends OffsettedItem {
    /** {@code non-null;} the string value */
    private final CstString value;

    /**
     * Constructs an instance.
     *
     * @param value {@code non-null;} the string value
     */
    public StringDataItem(CstString value) {
        super(1, writeSize(value));

        this.value = value;
    }

    /**
     * Gets the write size for a given value.
     *
     * @param value {@code non-null;} the string value
     * @return {@code >= 2}; the write size, in bytes
     */
    private static int writeSize(CstString value) {
        int utf16Size = value.getUtf16Size();

        // The +1 is for the '\0' termination byte.
        return Leb128.unsignedLeb128Size(utf16Size)
            + value.getUtf8Size() + 1;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_STRING_DATA_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        // Nothing to do here.
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo0(DexFile file, AnnotatedOutput out) {
        ByteArray bytes = value.getBytes();
        int utf16Size = value.getUtf16Size();

        if (out.annotates()) {
            out.annotate(Leb128.unsignedLeb128Size(utf16Size),
                    "utf16_size: " + Hex.u4(utf16Size));
            out.annotate(bytes.size() + 1, value.toQuoted());
        }

        out.writeUleb128(utf16Size);
        out.write(bytes);
        out.writeByte(0);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return value.toQuoted();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(OffsettedItem other) {
        StringDataItem otherData = (StringDataItem) other;

        return value.compareTo(otherData.value);
    }
}
