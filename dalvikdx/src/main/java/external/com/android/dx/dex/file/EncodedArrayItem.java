/*
 * Copyright (C) 2008 The Android Open Source Project
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

import external.com.android.dx.rop.cst.CstArray;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ByteArrayAnnotatedOutput;

/**
 * Encoded array of constant values.
 */
public final class EncodedArrayItem extends OffsettedItem {
    /** the required alignment for instances of this class */
    private static final int ALIGNMENT = 1;

    /** {@code non-null;} the array to represent */
    private final CstArray array;

    /**
     * {@code null-ok;} encoded form, ready for writing to a file; set during
     * {@link #place0}
     */
    private byte[] encodedForm;

    /**
     * Constructs an instance.
     *
     * @param array {@code non-null;} array to represent
     */
    public EncodedArrayItem(CstArray array) {
        /*
         * The write size isn't known up-front because (the variable-lengthed)
         * leb128 type is used to represent some things.
         */
        super(ALIGNMENT, -1);

        if (array == null) {
            throw new NullPointerException("array == null");
        }

        this.array = array;
        this.encodedForm = null;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return array.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(OffsettedItem other) {
        EncodedArrayItem otherArray = (EncodedArrayItem) other;

        return array.compareTo(otherArray.array);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return array.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        ValueEncoder.addContents(file, array);
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        // Encode the data and note the size.

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);

        encoder.writeArray(array, false);
        encodedForm = out.toByteArray();
        setWriteSize(encodedForm.length);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();

        if (annotates) {
            out.annotate(0, offsetString() + " encoded array");

            /*
             * The output is to be annotated, so redo the work previously
             * done by place0(), except this time annotations will actually
             * get emitted.
             */
            ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeArray(array, true);
        } else {
            out.write(encodedForm);
        }
    }
}
