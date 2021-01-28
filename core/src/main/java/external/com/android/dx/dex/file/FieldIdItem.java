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

import external.com.android.dx.rop.cst.CstFieldRef;

/**
 * Representation of a field reference inside a Dalvik file.
 */
public final class FieldIdItem extends MemberIdItem {
    /**
     * Constructs an instance.
     *
     * @param field {@code non-null;} the constant for the field
     */
    public FieldIdItem(CstFieldRef field) {
        super(field);
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        super.addContents(file);

        TypeIdsSection typeIds = file.getTypeIds();
        typeIds.intern(getFieldRef().getType());
    }

    /**
     * Gets the field constant.
     *
     * @return {@code non-null;} the constant
     */
    public CstFieldRef getFieldRef() {
        return (CstFieldRef) getRef();
    }

    /** {@inheritDoc} */
    @Override
    protected int getTypoidIdx(DexFile file) {
        TypeIdsSection typeIds = file.getTypeIds();
        return typeIds.indexOf(getFieldRef().getType());
    }

    /** {@inheritDoc} */
    @Override
    protected String getTypoidName() {
        return "type_idx";
    }
}
