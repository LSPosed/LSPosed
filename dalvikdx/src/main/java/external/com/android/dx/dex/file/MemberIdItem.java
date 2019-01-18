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
import external.com.android.dx.rop.cst.CstMemberRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;

/**
 * Representation of a member (field or method) reference inside a
 * Dalvik file.
 */
public abstract class MemberIdItem extends IdItem {
    /** {@code non-null;} the constant for the member */
    private final CstMemberRef cst;

    /**
     * Constructs an instance.
     *
     * @param cst {@code non-null;} the constant for the member
     */
    public MemberIdItem(CstMemberRef cst) {
        super(cst.getDefiningClass());

        this.cst = cst;
    }

    /** {@inheritDoc} */
    @Override
    public int writeSize() {
        return SizeOf.MEMBER_ID_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        super.addContents(file);

        StringIdsSection stringIds = file.getStringIds();
        stringIds.intern(getRef().getNat().getName());
    }

    /** {@inheritDoc} */
    @Override
    public final void writeTo(DexFile file, AnnotatedOutput out) {
        TypeIdsSection typeIds = file.getTypeIds();
        StringIdsSection stringIds = file.getStringIds();
        CstNat nat = cst.getNat();
        int classIdx = typeIds.indexOf(getDefiningClass());
        int nameIdx = stringIds.indexOf(nat.getName());
        int typoidIdx = getTypoidIdx(file);

        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + cst.toHuman());
            out.annotate(2, "  class_idx: " + Hex.u2(classIdx));
            out.annotate(2, String.format("  %-10s %s", getTypoidName() + ':',
                            Hex.u2(typoidIdx)));
            out.annotate(4, "  name_idx:  " + Hex.u4(nameIdx));
        }

        out.writeShort(classIdx);
        out.writeShort(typoidIdx);
        out.writeInt(nameIdx);
    }

    /**
     * Returns the index of the type-like thing associated with
     * this item, in order that it may be written out. Subclasses must
     * override this to get whatever it is they need to store.
     *
     * @param file {@code non-null;} the file being written
     * @return the index in question
     */
    protected abstract int getTypoidIdx(DexFile file);

    /**
     * Returns the field name of the type-like thing associated with
     * this item, for listing-generating purposes. Subclasses must override
     * this.
     *
     * @return {@code non-null;} the name in question
     */
    protected abstract String getTypoidName();

    /**
     * Gets the member constant.
     *
     * @return {@code non-null;} the constant
     */
    public final CstMemberRef getRef() {
        return cst;
    }
}
