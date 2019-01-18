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
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Field refs list section of a {@code .dex} file.
 */
public final class FieldIdsSection extends MemberIdsSection {
    /**
     * {@code non-null;} map from field constants to {@link
     * FieldIdItem} instances
     */
    private final TreeMap<CstFieldRef, FieldIdItem> fieldIds;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public FieldIdsSection(DexFile file) {
        super("field_ids", file);

        fieldIds = new TreeMap<CstFieldRef, FieldIdItem>();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return fieldIds.values();
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        throwIfNotPrepared();

        IndexedItem result = fieldIds.get((CstFieldRef) cst);

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

        int sz = fieldIds.size();
        int offset = (sz == 0) ? 0 : getFileOffset();

        if (out.annotates()) {
            out.annotate(4, "field_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "field_ids_off:   " + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Interns an element into this instance.
     *
     * @param field {@code non-null;} the reference to intern
     * @return {@code non-null;} the interned reference
     */
    public synchronized FieldIdItem intern(CstFieldRef field) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }

        throwIfPrepared();

        FieldIdItem result = fieldIds.get(field);

        if (result == null) {
            result = new FieldIdItem(field);
            fieldIds.put(field, result);
        }

        return result;
    }

    /**
     * Gets the index of the given reference, which must have been added
     * to this instance.
     *
     * @param ref {@code non-null;} the reference to look up
     * @return {@code >= 0;} the reference's index
     */
    public int indexOf(CstFieldRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }

        throwIfNotPrepared();

        FieldIdItem item = fieldIds.get(ref);

        if (item == null) {
            throw new IllegalArgumentException("not found");
        }

        return item.getIndex();
    }
}
