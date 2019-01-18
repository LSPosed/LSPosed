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

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Strings list section of a {@code .dex} file.
 */
public final class StringIdsSection
        extends UniformItemSection {
    /**
     * {@code non-null;} map from string constants to {@link
     * StringIdItem} instances
     */
    private final TreeMap<CstString, StringIdItem> strings;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public StringIdsSection(DexFile file) {
        super("string_ids", file, 4);

        strings = new TreeMap<CstString, StringIdItem>();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return strings.values();
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        throwIfNotPrepared();

        IndexedItem result = strings.get((CstString) cst);

        if (result == null) {
            throw new IllegalArgumentException("not found");
        }

        return result;
    }

    /**
     * Writes the portion of the file header that refers to this instance.
     *
     * @param out {@code non-null;} where to write
     */
    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();

        int sz = strings.size();
        int offset = (sz == 0) ? 0 : getFileOffset();

        if (out.annotates()) {
            out.annotate(4, "string_ids_size: " + Hex.u4(sz));
            out.annotate(4, "string_ids_off:  " + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Interns an element into this instance.
     *
     * @param string {@code non-null;} the string to intern, as a regular Java
     * {@code String}
     * @return {@code non-null;} the interned string
     */
    public StringIdItem intern(String string) {
        return intern(new StringIdItem(new CstString(string)));
    }

    /**
     * Interns an element into this instance.
     *
     * @param string {@code non-null;} the string to intern, as a constant
     * @return {@code non-null;} the interned string
     */
    public StringIdItem intern(CstString string) {
        return intern(new StringIdItem(string));
    }

    /**
     * Interns an element into this instance.
     *
     * @param string {@code non-null;} the string to intern
     * @return {@code non-null;} the interned string
     */
    public synchronized StringIdItem intern(StringIdItem string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }

        throwIfPrepared();

        CstString value = string.getValue();
        StringIdItem already = strings.get(value);

        if (already != null) {
            return already;
        }

        strings.put(value, string);
        return string;
    }

    /**
     * Interns the components of a name-and-type into this instance.
     *
     * @param nat {@code non-null;} the name-and-type
     */
    public synchronized void intern(CstNat nat) {
        intern(nat.getName());
        intern(nat.getDescriptor());
    }

    /**
     * Gets the index of the given string, which must have been added
     * to this instance.
     *
     * @param string {@code non-null;} the string to look up
     * @return {@code >= 0;} the string's index
     */
    public int indexOf(CstString string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }

        throwIfNotPrepared();

        StringIdItem s = strings.get(string);

        if (s == null) {
            throw new IllegalArgumentException("not found");
        }

        return s.getIndex();
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int idx = 0;

        for (StringIdItem s : strings.values()) {
            s.setIndex(idx);
            idx++;
        }
    }
}
