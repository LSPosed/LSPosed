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

/**
 * An item in a Dalvik file which is referenced by absolute offset.
 */
public abstract class OffsettedItem extends Item
        implements Comparable<OffsettedItem> {
    /** {@code > 0;} alignment requirement */
    private final int alignment;

    /** {@code >= -1;} the size of this instance when written, in bytes, or
     * {@code -1} if not yet known */
    private int writeSize;

    /**
     * {@code null-ok;} section the item was added to, or {@code null} if
     * not yet added
     */
    private Section addedTo;

    /**
     * {@code >= -1;} assigned offset of the item from the start of its section,
     * or {@code -1} if not yet assigned
     */
    private int offset;

    /**
     * Gets the absolute offset of the given item, returning {@code 0}
     * if handed {@code null}.
     *
     * @param item {@code null-ok;} the item in question
     * @return {@code >= 0;} the item's absolute offset, or {@code 0}
     * if {@code item == null}
     */
    public static int getAbsoluteOffsetOr0(OffsettedItem item) {
        if (item == null) {
            return 0;
        }

        return item.getAbsoluteOffset();
    }

    /**
     * Constructs an instance. The offset is initially unassigned.
     *
     * @param alignment {@code > 0;} output alignment requirement; must be a
     * power of 2
     * @param writeSize {@code >= -1;} the size of this instance when written,
     * in bytes, or {@code -1} if not immediately known
     */
    public OffsettedItem(int alignment, int writeSize) {
        Section.validateAlignment(alignment);

        if (writeSize < -1) {
            throw new IllegalArgumentException("writeSize < -1");
        }

        this.alignment = alignment;
        this.writeSize = writeSize;
        this.addedTo = null;
        this.offset = -1;
    }

    /**
     * {@inheritDoc}
     *
     * Comparisons for this class are defined to be type-major (if the
     * types don't match then the objects are not equal), with
     * {@link #compareTo0} deciding same-type comparisons.
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        OffsettedItem otherItem = (OffsettedItem) other;
        ItemType thisType = itemType();
        ItemType otherType = otherItem.itemType();

        if (thisType != otherType) {
            return false;
        }

        return (compareTo0(otherItem) == 0);
    }

    /**
     * {@inheritDoc}
     *
     * Comparisons for this class are defined to be class-major (if the
     * classes don't match then the objects are not equal), with
     * {@link #compareTo0} deciding same-class comparisons.
     */
    @Override
    public final int compareTo(OffsettedItem other) {
        if (this == other) {
            return 0;
        }

        ItemType thisType = itemType();
        ItemType otherType = other.itemType();

        if (thisType != otherType) {
            return thisType.compareTo(otherType);
        }

        return compareTo0(other);
    }

    /**
     * Sets the write size of this item. This may only be called once
     * per instance, and only if the size was unknown upon instance
     * creation.
     *
     * @param writeSize {@code > 0;} the write size, in bytes
     */
    public final void setWriteSize(int writeSize) {
        if (writeSize < 0) {
            throw new IllegalArgumentException("writeSize < 0");
        }

        if (this.writeSize >= 0) {
            throw new UnsupportedOperationException("writeSize already set");
        }

        this.writeSize = writeSize;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException thrown if the write size
     * is not yet known
     */
    @Override
    public final int writeSize() {
        if (writeSize < 0) {
            throw new UnsupportedOperationException("writeSize is unknown");
        }

        return writeSize;
    }

    /** {@inheritDoc} */
    @Override
    public final void writeTo(DexFile file, AnnotatedOutput out) {
        out.alignTo(alignment);

        try {
            if (writeSize < 0) {
                throw new UnsupportedOperationException(
                        "writeSize is unknown");
            }
            out.assertCursor(getAbsoluteOffset());
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex,
                    "...while writing " + this);
        }

        writeTo0(file, out);
    }

    /**
     * Gets the relative item offset. The offset is from the start of
     * the section which the instance was written to.
     *
     * @return {@code >= 0;} the offset
     * @throws RuntimeException thrown if the offset is not yet known
     */
    public final int getRelativeOffset() {
        if (offset < 0) {
            throw new RuntimeException("offset not yet known");
        }

        return offset;
    }

    /**
     * Gets the absolute item offset. The offset is from the start of
     * the file which the instance was written to.
     *
     * @return {@code >= 0;} the offset
     * @throws RuntimeException thrown if the offset is not yet known
     */
    public final int getAbsoluteOffset() {
        if (offset < 0) {
            throw new RuntimeException("offset not yet known");
        }

        return addedTo.getAbsoluteOffset(offset);
    }

    /**
     * Indicates that this item has been added to the given section at
     * the given offset. It is only valid to call this method once per
     * instance.
     *
     * @param addedTo {@code non-null;} the section this instance has
     * been added to
     * @param offset {@code >= 0;} the desired offset from the start of the
     * section where this instance was placed
     * @return {@code >= 0;} the offset that this instance should be placed at
     * in order to meet its alignment constraint
     */
    public final int place(Section addedTo, int offset) {
        if (addedTo == null) {
            throw new NullPointerException("addedTo == null");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }

        if (this.addedTo != null) {
            throw new RuntimeException("already written");
        }

        int mask = alignment - 1;
        offset = (offset + mask) & ~mask;

        this.addedTo = addedTo;
        this.offset = offset;

        place0(addedTo, offset);

        return offset;
    }

    /**
     * Gets the alignment requirement of this instance. An instance should
     * only be written when so aligned.
     *
     * @return {@code > 0;} the alignment requirement; must be a power of 2
     */
    public final int getAlignment() {
        return alignment;
    }

    /**
     * Gets the absolute offset of this item as a string, suitable for
     * including in annotations.
     *
     * @return {@code non-null;} the offset string
     */
    public final String offsetString() {
        return '[' + Integer.toHexString(getAbsoluteOffset()) + ']';
    }

    /**
     * Gets a short human-readable string representing this instance.
     *
     * @return {@code non-null;} the human form
     */
    public abstract String toHuman();

    /**
     * Compares this instance to another which is guaranteed to be of
     * the same class. The default implementation of this method is to
     * throw an exception (unsupported operation). If a particular
     * class needs to actually sort, then it should override this
     * method.
     *
     * @param other {@code non-null;} instance to compare to
     * @return {@code -1}, {@code 0}, or {@code 1}, depending
     * on the sort order of this instance and the other
     */
    protected int compareTo0(OffsettedItem other) {
        throw new UnsupportedOperationException("unsupported");
    }

    /**
     * Does additional work required when placing an instance. The
     * default implementation of this method is a no-op. If a
     * particular class needs to do something special, then it should
     * override this method. In particular, if this instance did not
     * know its write size up-front, then this method is responsible
     * for setting it.
     *
     * @param addedTo {@code non-null;} the section this instance has been added to
     * @param offset {@code >= 0;} the offset from the start of the
     * section where this instance was placed
     */
    protected void place0(Section addedTo, int offset) {
        // This space intentionally left blank.
    }

    /**
     * Performs the actual write of the contents of this instance to
     * the given data section. This is called by {@link #writeTo},
     * which will have taken care of ensuring alignment.
     *
     * @param file {@code non-null;} the file to use for reference
     * @param out {@code non-null;} where to write to
     */
    protected abstract void writeTo0(DexFile file, AnnotatedOutput out);
}
