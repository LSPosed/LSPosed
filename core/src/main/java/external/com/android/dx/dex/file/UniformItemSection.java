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
import external.com.android.dx.util.AnnotatedOutput;
import java.util.Collection;

/**
 * A section of a {@code .dex} file which consists of a sequence of
 * {@link Item} objects. Each of the items must have the same size in
 * the output.
 */
public abstract class UniformItemSection extends Section {
    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param name {@code null-ok;} the name of this instance, for annotation
     * purposes
     * @param file {@code non-null;} file that this instance is part of
     * @param alignment {@code > 0;} alignment requirement for the final output;
     * must be a power of 2
     */
    public UniformItemSection(String name, DexFile file, int alignment) {
        super(name, file, alignment);
    }

    /** {@inheritDoc} */
    @Override
    public final int writeSize() {
        Collection<? extends Item> items = items();
        int sz = items.size();

        if (sz == 0) {
            return 0;
        }

        // Since each item has to be the same size, we can pick any.
        return sz * items.iterator().next().writeSize();
    }

    /**
     * Gets the item corresponding to the given {@link Constant}. This
     * will throw an exception if the constant is not found, including
     * if this instance isn't the sort that maps constants to {@link
     * IndexedItem} instances.
     *
     * @param cst {@code non-null;} constant to look for
     * @return {@code non-null;} the corresponding item found in this instance
     */
    public abstract IndexedItem get(Constant cst);

    /** {@inheritDoc} */
    @Override
    protected final void prepare0() {
        DexFile file = getFile();

        orderItems();

        for (Item one : items()) {
            one.addContents(file);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void writeTo0(AnnotatedOutput out) {
        DexFile file = getFile();
        int alignment = getAlignment();

        for (Item one : items()) {
            one.writeTo(file, out);
            out.alignTo(alignment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int getAbsoluteItemOffset(Item item) {
        /*
         * Since all items must be the same size, we can use the size
         * of the one we're given to calculate its offset.
         */
        IndexedItem ii = (IndexedItem) item;
        int relativeOffset = ii.getIndex() * ii.writeSize();

        return getAbsoluteOffset(relativeOffset);
    }

    /**
     * Alters or picks the order for items in this instance if desired,
     * so that subsequent calls to {@link #items} will yield a
     * so-ordered collection. If the items in this instance are indexed,
     * then this method should also assign indices.
     */
    protected abstract void orderItems();
}
