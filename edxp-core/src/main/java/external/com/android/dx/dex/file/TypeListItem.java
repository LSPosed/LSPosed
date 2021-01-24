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

import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;

/**
 * Representation of a list of class references.
 */
public final class TypeListItem extends OffsettedItem {
    /** alignment requirement */
    private static final int ALIGNMENT = 4;

    /** element size in bytes */
    private static final int ELEMENT_SIZE = 2;

    /** header size in bytes */
    private static final int HEADER_SIZE = 4;

    /** {@code non-null;} the actual list */
    private final TypeList list;

    /**
     * Constructs an instance.
     *
     * @param list {@code non-null;} the actual list
     */
    public TypeListItem(TypeList list) {
        super(ALIGNMENT, (list.size() * ELEMENT_SIZE) + HEADER_SIZE);

        this.list = list;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return StdTypeList.hashContents(list);
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_TYPE_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        TypeIdsSection typeIds = file.getTypeIds();
        int sz = list.size();

        for (int i = 0; i < sz; i++) {
            typeIds.intern(list.getType(i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }

    /**
     * Gets the underlying list.
     *
     * @return {@code non-null;} the list
     */
    public TypeList getList() {
        return list;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        TypeIdsSection typeIds = file.getTypeIds();
        int sz = list.size();

        if (out.annotates()) {
            out.annotate(0, offsetString() + " type_list");
            out.annotate(HEADER_SIZE, "  size: " + Hex.u4(sz));
            for (int i = 0; i < sz; i++) {
                Type one = list.getType(i);
                int idx = typeIds.indexOf(one);
                out.annotate(ELEMENT_SIZE,
                             "  " + Hex.u2(idx) + " // " + one.toHuman());
            }
        }

        out.writeInt(sz);

        for (int i = 0; i < sz; i++) {
            out.writeShort(typeIds.indexOf(list.getType(i)));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(OffsettedItem other) {
        TypeList thisList = this.list;
        TypeList otherList = ((TypeListItem) other).list;

        return StdTypeList.compareContents(thisList, otherList);
    }
}
