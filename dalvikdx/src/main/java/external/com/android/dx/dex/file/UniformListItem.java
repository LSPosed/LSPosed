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

import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.List;

/**
 * Class that represents a contiguous list of uniform items. Each
 * item in the list, in particular, must have the same write size and
 * alignment.
 *
 * <p>This class inherits its alignment from its items, bumped up to
 * {@code 4} if the items have a looser alignment requirement. If
 * it is more than {@code 4}, then there will be a gap after the
 * output list size (which is four bytes) and before the first item.</p>
 *
 * @param <T> type of element contained in an instance
 */
public final class UniformListItem<T extends OffsettedItem>
        extends OffsettedItem {
    /** the size of the list header */
    private static final int HEADER_SIZE = 4;

    /** {@code non-null;} the item type */
    private final ItemType itemType;

    /** {@code non-null;} the contents */
    private final List<T> items;

    /**
     * Constructs an instance. It is illegal to modify the given list once
     * it is used to construct an instance of this class.
     *
     * @param itemType {@code non-null;} the type of the item
     * @param items {@code non-null and non-empty;} list of items to represent
     */
    public UniformListItem(ItemType itemType, List<T> items) {
        super(getAlignment(items), writeSize(items));

        if (itemType == null) {
            throw new NullPointerException("itemType == null");
        }

        this.items = items;
        this.itemType = itemType;
    }

    /**
     * Helper for {@link #UniformListItem}, which returns the alignment
     * requirement implied by the given list. See the header comment for
     * more details.
     *
     * @param items {@code non-null;} list of items being represented
     * @return {@code >= 4;} the alignment requirement
     */
    private static int getAlignment(List<? extends OffsettedItem> items) {
        try {
            // Since they all must have the same alignment, any one will do.
            return Math.max(HEADER_SIZE, items.get(0).getAlignment());
        } catch (IndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("items.size() == 0");
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("items == null");
        }
    }

    /**
     * Calculates the write size for the given list.
     *
     * @param items {@code non-null;} the list in question
     * @return {@code >= 0;} the write size
     */
    private static int writeSize(List<? extends OffsettedItem> items) {
        /*
         * This class assumes all included items are the same size,
         * an assumption which is verified in place0().
         */
        OffsettedItem first = items.get(0);
        return (items.size() * first.writeSize()) + getAlignment(items);
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return itemType;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);

        sb.append(getClass().getName());
        sb.append(items);

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        for (OffsettedItem i : items) {
            i.addContents(file);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String toHuman() {
        StringBuilder sb = new StringBuilder(100);
        boolean first = true;

        sb.append("{");

        for (OffsettedItem i : items) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(i.toHuman());
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets the underlying list of items.
     *
     * @return {@code non-null;} the list
     */
    public final List<T> getItems() {
        return items;
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        offset += headerSize();

        boolean first = true;
        int theSize = -1;
        int theAlignment = -1;

        for (OffsettedItem i : items) {
            int size = i.writeSize();
            if (first) {
                theSize = size;
                theAlignment = i.getAlignment();
                first = false;
            } else {
                if (size != theSize) {
                    throw new UnsupportedOperationException(
                            "item size mismatch");
                }
                if (i.getAlignment() != theAlignment) {
                    throw new UnsupportedOperationException(
                            "item alignment mismatch");
                }
            }

            offset = i.place(addedTo, offset) + size;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        int size = items.size();

        if (out.annotates()) {
            out.annotate(0, offsetString() + " " + typeName());
            out.annotate(4, "  size: " + Hex.u4(size));
        }

        out.writeInt(size);

        for (OffsettedItem i : items) {
            i.writeTo(file, out);
        }
    }

    /**
     * Get the size of the header of this list.
     *
     * @return {@code >= 0;} the header size
     */
    private int headerSize() {
        /*
         * Because of how this instance was set up, this is the same
         * as the alignment.
         */
        return getAlignment();
    }
}
