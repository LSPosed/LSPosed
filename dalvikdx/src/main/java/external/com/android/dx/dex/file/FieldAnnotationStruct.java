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

import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.ToHuman;

/**
 * Association of a field and its annotations.
 */
public final class FieldAnnotationStruct
        implements ToHuman, Comparable<FieldAnnotationStruct> {
    /** {@code non-null;} the field in question */
    private final CstFieldRef field;

    /** {@code non-null;} the associated annotations */
    private AnnotationSetItem annotations;

    /**
     * Constructs an instance.
     *
     * @param field {@code non-null;} the field in question
     * @param annotations {@code non-null;} the associated annotations
     */
    public FieldAnnotationStruct(CstFieldRef field,
            AnnotationSetItem annotations) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }

        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }

        this.field = field;
        this.annotations = annotations;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return field.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof FieldAnnotationStruct)) {
            return false;
        }

        return field.equals(((FieldAnnotationStruct) other).field);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(FieldAnnotationStruct other) {
        return field.compareTo(other.field);
    }

    /** {@inheritDoc} */
    public void addContents(DexFile file) {
        FieldIdsSection fieldIds = file.getFieldIds();
        MixedItemSection wordData = file.getWordData();

        fieldIds.intern(field);
        annotations = wordData.intern(annotations);
    }

    /** {@inheritDoc} */
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int fieldIdx = file.getFieldIds().indexOf(field);
        int annotationsOff = annotations.getAbsoluteOffset();

        if (out.annotates()) {
            out.annotate(0, "    " + field.toHuman());
            out.annotate(4, "      field_idx:       " + Hex.u4(fieldIdx));
            out.annotate(4, "      annotations_off: " +
                    Hex.u4(annotationsOff));
        }

        out.writeInt(fieldIdx);
        out.writeInt(annotationsOff);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return field.toHuman() + ": " + annotations;
    }

    /**
     * Gets the field this item is for.
     *
     * @return {@code non-null;} the field
     */
    public CstFieldRef getField() {
        return field;
    }

    /**
     * Gets the associated annotations.
     *
     * @return {@code non-null;} the annotations
     */
    public Annotations getAnnotations() {
        return annotations.getAnnotations();
    }
}
