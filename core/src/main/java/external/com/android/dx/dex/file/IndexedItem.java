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

/**
 * An item in a Dalvik file which is referenced by index.
 */
public abstract class IndexedItem extends Item {
    /** {@code >= -1;} assigned index of the item, or {@code -1} if not
     * yet assigned */
    private int index;

    /**
     * Constructs an instance. The index is initially unassigned.
     */
    public IndexedItem() {
        index = -1;
    }

    /**
     * Gets whether or not this instance has been assigned an index.
     *
     * @return {@code true} iff this instance has been assigned an index
     */
    public final boolean hasIndex() {
        return (index >= 0);
    }

    /**
     * Gets the item index.
     *
     * @return {@code >= 0;} the index
     * @throws RuntimeException thrown if the item index is not yet assigned
     */
    public final int getIndex() {
        if (index < 0) {
            throw new RuntimeException("index not yet set");
        }

        return index;
    }

    /**
     * Sets the item index. This method may only ever be called once
     * per instance, and this will throw a {@code RuntimeException} if
     * called a second (or subsequent) time.
     *
     * @param index {@code >= 0;} the item index
     */
    public final void setIndex(int index) {
        if (this.index != -1) {
            throw new RuntimeException("index already set");
        }

        this.index = index;
    }

    /**
     * Gets the index of this item as a string, suitable for including in
     * annotations.
     *
     * @return {@code non-null;} the index string
     */
    public final String indexString() {
        return '[' + Integer.toHexString(index) + ']';
    }
}
