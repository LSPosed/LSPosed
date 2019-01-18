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

import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;

/**
 * Indirect reference to an {@link AnnotationSetItem}.
 */
public final class AnnotationSetRefItem extends OffsettedItem {
    /** the required alignment for instances of this class */
    private static final int ALIGNMENT = 4;

    /** write size of this class, in bytes */
    private static final int WRITE_SIZE = 4;

    /** {@code non-null;} the annotation set to refer to */
    private AnnotationSetItem annotations;

    /**
     * Constructs an instance.
     *
     * @param annotations {@code non-null;} the annotation set to refer to
     */
    public AnnotationSetRefItem(AnnotationSetItem annotations) {
        super(ALIGNMENT, WRITE_SIZE);

        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }

        this.annotations = annotations;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_SET_REF_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        MixedItemSection wordData = file.getWordData();

        annotations = wordData.intern(annotations);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return annotations.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        int annotationsOff = annotations.getAbsoluteOffset();

        if (out.annotates()) {
            out.annotate(4, "  annotations_off: " + Hex.u4(annotationsOff));
        }

        out.writeInt(annotationsOff);
    }
}
