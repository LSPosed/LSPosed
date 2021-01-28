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

import external.com.android.dx.rop.cst.CstType;

/**
 * Representation of a reference to an item inside a Dalvik file.
 */
public abstract class IdItem extends IndexedItem {
    /**
     * {@code non-null;} the type constant for the defining class of
     * the reference
     */
    private final CstType type;

    /**
     * Constructs an instance.
     *
     * @param type {@code non-null;} the type constant for the defining
     * class of the reference
     */
    public IdItem(CstType type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }

        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        TypeIdsSection typeIds = file.getTypeIds();
        typeIds.intern(type);
    }

    /**
     * Gets the type constant for the defining class of the
     * reference.
     *
     * @return {@code non-null;} the type constant
     */
    public final CstType getDefiningClass() {
        return type;
    }
}
