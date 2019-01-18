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

package external.com.android.dx.cf.iface;

import external.com.android.dx.util.FixedSizeList;

/**
 * Standard implementation of {@link FieldList}, which directly stores
 * an array of {@link Field} objects and can be made immutable.
 */
public final class StdFieldList extends FixedSizeList implements FieldList {
    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     */
    public StdFieldList(int size) {
        super(size);
    }

    /** {@inheritDoc} */
    @Override
    public Field get(int n) {
        return (Field) get0(n);
    }

    /**
     * Sets the field at the given index.
     *
     * @param n {@code >= 0, < size();} which field
     * @param field {@code null-ok;} the field object
     */
    public void set(int n, Field field) {
        set0(n, field);
    }
}
