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

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * A section of a {@code .dex} file which consists of a sequence of
 * {@link OffsettedItem} objects, which may each be of a different concrete
 * class and/or size.
 *
 * <b>Note:</b> It is invalid for an item in an instance of this class to
 * have a larger alignment requirement than the alignment of this instance.
 */
public final class MixedItemSection extends Section {
    static enum SortType {
        /** no sorting */
        NONE,

        /** sort by type only */
        TYPE,

        /** sort in class-major order, with instances sorted per-class */
        INSTANCE;
    };

    /** {@code non-null;} sorter which sorts instances by type */
    private static final Comparator<OffsettedItem> TYPE_SORTER =
        new Comparator<OffsettedItem>() {
        @Override
        public int compare(OffsettedItem item1, OffsettedItem item2) {
            ItemType type1 = item1.itemType();
            ItemType type2 = item2.itemType();
            return type1.compareTo(type2);
        }
    };

    /** {@code non-null;} the items in this part */
    private final ArrayList<OffsettedItem> items;

    /** {@code non-null;} items that have been explicitly interned */
    private final HashMap<OffsettedItem, OffsettedItem> interns;

    /** {@code non-null;} how to sort the items */
    private final SortType sort;

    /**
     * {@code >= -1;} the current size of this part, in bytes, or {@code -1}
     * if not yet calculated
     */
    private int writeSize;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param name {@code null-ok;} the name of this instance, for annotation
     * purposes
     * @param file {@code non-null;} file that this instance is part of
     * @param alignment {@code > 0;} alignment requirement for the final output;
     * must be a power of 2
     * @param sort how the items should be sorted in the final output
     */
    public MixedItemSection(String name, DexFile file, int alignment,
            SortType sort) {
        super(name, file, alignment);

        this.items = new ArrayList<OffsettedItem>(100);
        this.interns = new HashMap<OffsettedItem, OffsettedItem>(100);
        this.sort = sort;
        this.writeSize = -1;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return items;
    }

    /** {@inheritDoc} */
    @Override
    public int writeSize() {
        throwIfNotPrepared();
        return writeSize;
    }

    /** {@inheritDoc} */
    @Override
    public int getAbsoluteItemOffset(Item item) {
        OffsettedItem oi = (OffsettedItem) item;
        return oi.getAbsoluteOffset();
    }

    /**
     * Gets the size of this instance, in items.
     *
     * @return {@code >= 0;} the size
     */
    public int size() {
        return items.size();
    }

    /**
     * Writes the portion of the file header that refers to this instance.
     *
     * @param out {@code non-null;} where to write
     */
    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();

        if (writeSize == -1) {
            throw new RuntimeException("write size not yet set");
        }

        int sz = writeSize;
        int offset = (sz == 0) ? 0 : getFileOffset();
        String name = getName();

        if (name == null) {
            name = "<unnamed>";
        }

        int spaceCount = 15 - name.length();
        char[] spaceArr = new char[spaceCount];
        Arrays.fill(spaceArr, ' ');
        String spaces = new String(spaceArr);

        if (out.annotates()) {
            out.annotate(4, name + "_size:" + spaces + Hex.u4(sz));
            out.annotate(4, name + "_off: " + spaces + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Adds an item to this instance. This will in turn tell the given item
     * that it has been added to this instance. It is invalid to add the
     * same item to more than one instance, nor to add the same items
     * multiple times to a single instance.
     *
     * @param item {@code non-null;} the item to add
     */
    public void add(OffsettedItem item) {
        throwIfPrepared();

        try {
            if (item.getAlignment() > getAlignment()) {
                throw new IllegalArgumentException(
                        "incompatible item alignment");
            }
        } catch (NullPointerException ex) {
            // Elucidate the exception.
            throw new NullPointerException("item == null");
        }

        items.add(item);
    }

    /**
     * Interns an item in this instance, returning the interned instance
     * (which may not be the one passed in). This will add the item if no
     * equal item has been added.
     *
     * @param item {@code non-null;} the item to intern
     * @return {@code non-null;} the equivalent interned instance
     */
    public synchronized <T extends OffsettedItem> T intern(T item) {
        throwIfPrepared();

        OffsettedItem result = interns.get(item);

        if (result != null) {
            return (T) result;
        }

        add(item);
        interns.put(item, item);
        return item;
    }

    /**
     * Gets an item which was previously interned.
     *
     * @param item {@code non-null;} the item to look for
     * @return {@code non-null;} the equivalent already-interned instance
     */
    public <T extends OffsettedItem> T get(T item) {
        throwIfNotPrepared();

        OffsettedItem result = interns.get(item);

        if (result != null) {
            return (T) result;
        }

        throw new NoSuchElementException(item.toString());
    }

    /**
     * Writes an index of contents of the items in this instance of the
     * given type. If there are none, this writes nothing. If there are any,
     * then the index is preceded by the given intro string.
     *
     * @param out {@code non-null;} where to write to
     * @param itemType {@code non-null;} the item type of interest
     * @param intro {@code non-null;} the introductory string for non-empty indices
     */
    public void writeIndexAnnotation(AnnotatedOutput out, ItemType itemType,
            String intro) {
        throwIfNotPrepared();

        TreeMap<String, OffsettedItem> index =
            new TreeMap<String, OffsettedItem>();

        for (OffsettedItem item : items) {
            if (item.itemType() == itemType) {
                String label = item.toHuman();
                index.put(label, item);
            }
        }

        if (index.size() == 0) {
            return;
        }

        out.annotate(0, intro);

        for (Map.Entry<String, OffsettedItem> entry : index.entrySet()) {
            String label = entry.getKey();
            OffsettedItem item = entry.getValue();
            out.annotate(0, item.offsetString() + ' ' + label + '\n');
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void prepare0() {
        DexFile file = getFile();

        /*
         * It's okay for new items to be added as a result of an
         * addContents() call; we just have to deal with the possibility.
         */

        int i = 0;
        for (;;) {
            int sz = items.size();
            if (i >= sz) {
                break;
            }

            for (/*i*/; i < sz; i++) {
                OffsettedItem one = items.get(i);
                one.addContents(file);
            }
        }
    }

    /**
     * Places all the items in this instance at particular offsets. This
     * will call {@link OffsettedItem#place} on each item. If an item
     * does not know its write size before the call to {@code place},
     * it is that call which is responsible for setting the write size.
     * This method may only be called once per instance; subsequent calls
     * will throw an exception.
     */
    public void placeItems() {
        throwIfNotPrepared();

        switch (sort) {
            case INSTANCE: {
                Collections.sort(items);
                break;
            }
            case TYPE: {
                Collections.sort(items, TYPE_SORTER);
                break;
            }
        }

        int sz = items.size();
        int outAt = 0;
        for (int i = 0; i < sz; i++) {
            OffsettedItem one = items.get(i);
            try {
                int placedAt = one.place(this, outAt);

                if (placedAt < outAt) {
                    throw new RuntimeException("bogus place() result for " +
                            one);
                }

                outAt = placedAt + one.writeSize();
            } catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex,
                        "...while placing " + one);
            }
        }

        writeSize = outAt;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(AnnotatedOutput out) {
        boolean annotates = out.annotates();
        boolean first = true;
        DexFile file = getFile();
        int at = 0;

        for (OffsettedItem one : items) {
            if (annotates) {
                if (first) {
                    first = false;
                } else {
                    out.annotate(0, "\n");
                }
            }

            int alignMask = one.getAlignment() - 1;
            int writeAt = (at + alignMask) & ~alignMask;

            if (at != writeAt) {
                out.writeZeroes(writeAt - at);
                at = writeAt;
            }

            one.writeTo(file, out);
            at += one.writeSize();
        }

        if (at != writeSize) {
            throw new RuntimeException("output size mismatch");
        }
    }
}
