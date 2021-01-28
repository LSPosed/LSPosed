/*
 * Copyright (C) 2017 The Android Open Source Project
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

import external.com.android.dx.rop.cst.CstCallSite;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ByteArrayAnnotatedOutput;

/**
 * Representation of a call site in a DEX file.
 */
public final class CallSiteItem extends OffsettedItem {

    /** {@code non-null;} the call site value */
    private final CstCallSite value;

    /** {@code null-ok;} the encoded representation of the call site value */
    private byte[] encodedForm;

    /**
     * Constructs an instance.
     *
     * @param value {@code non-null;} the string value
     */
    public CallSiteItem(CstCallSite value) {
        super(1, writeSize(value));

        this.value = value;
    }

    /**
     * Gets the write size for a given value.
     *
     * @param value {@code non-null;} the call site value
     * @return {@code >= 2}; the write size, in bytes
     */
    private static int writeSize(CstCallSite value) {
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        // Encode the data and note the size.

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);

        encoder.writeArray(value, true);
        encodedForm = out.toByteArray();
        setWriteSize(encodedForm.length);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return value.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return value.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(0, offsetString() + " call site");
            ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeArray(value, true);
        } else {
            out.write(encodedForm);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        // A call site is an encoded array with additional constraints
        // on the element kinds. It is not listed separately in the
        // DEX file's table of contents.
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        ValueEncoder.addContents(file, value);
    }
}
