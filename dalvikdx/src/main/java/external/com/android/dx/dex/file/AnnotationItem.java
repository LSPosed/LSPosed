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

import external.com.android.dx.rop.annotation.Annotation;
import external.com.android.dx.rop.annotation.AnnotationVisibility;
import external.com.android.dx.rop.annotation.NameValuePair;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ByteArrayAnnotatedOutput;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Single annotation, which consists of a type and a set of name-value
 * element pairs.
 */
public final class AnnotationItem extends OffsettedItem {
    /** annotation visibility constant: visible at build time only */
    private static final int VISIBILITY_BUILD = 0;

    /** annotation visibility constant: visible at runtime */
    private static final int VISIBILITY_RUNTIME = 1;

    /** annotation visibility constant: visible at runtime only to system */
    private static final int VISIBILITY_SYSTEM = 2;

    /** the required alignment for instances of this class */
    private static final int ALIGNMENT = 1;

    /** {@code non-null;} unique instance of
     * {@link external.com.android.dx.dex.file.AnnotationItem.TypeIdSorter} */
    private static final TypeIdSorter TYPE_ID_SORTER = new TypeIdSorter();

    /** {@code non-null;} the annotation to represent */
    private final Annotation annotation;

    /**
     * {@code null-ok;} type reference for the annotation type; set during
     * {@link #addContents}
     */
    private TypeIdItem type;

    /**
     * {@code null-ok;} encoded form, ready for writing to a file; set during
     * {@link #place0}
     */
    private byte[] encodedForm;

    /**
     * Comparator that sorts (outer) instances by type id index.
     */
    private static class TypeIdSorter implements Comparator<AnnotationItem> {
        /** {@inheritDoc} */
        @Override
        public int compare(AnnotationItem item1, AnnotationItem item2) {
            int index1 = item1.type.getIndex();
            int index2 = item2.type.getIndex();

            if (index1 < index2) {
                return -1;
            } else if (index1 > index2) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * Sorts an array of instances, in place, by type id index,
     * ignoring all other aspects of the elements. This is only valid
     * to use after type id indices are known.
     *
     * @param array {@code non-null;} array to sort
     */
    public static void sortByTypeIdIndex(AnnotationItem[] array) {
        Arrays.sort(array, TYPE_ID_SORTER);
    }

    /**
     * Constructs an instance.
     *
     * @param annotation {@code non-null;} annotation to represent
     * @param dexFile {@code non-null;} dex output
     */
    public AnnotationItem(Annotation annotation, DexFile dexFile) {
        /*
         * The write size isn't known up-front because (the variable-lengthed)
         * leb128 type is used to represent some things.
         */
        super(ALIGNMENT, -1);

        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }

        this.annotation = annotation;
        this.type = null;
        this.encodedForm = null;
        addContents(dexFile);
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return annotation.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(OffsettedItem other) {
        AnnotationItem otherAnnotation = (AnnotationItem) other;

        return annotation.compareTo(otherAnnotation.annotation);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return annotation.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        type = file.getTypeIds().intern(annotation.getType());
        ValueEncoder.addContents(file, annotation);
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        // Encode the data and note the size.

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);

        encoder.writeAnnotation(annotation, false);
        encodedForm = out.toByteArray();

        // Add one for the visibility byte in front of the encoded annotation.
        setWriteSize(encodedForm.length + 1);
    }

    /**
     * Write a (listing file) annotation for this instance to the given
     * output, that consumes no bytes of output. This is for annotating
     * a reference to this instance at the point of the reference.
     *
     * @param out {@code non-null;} where to output to
     * @param prefix {@code non-null;} prefix for each line of output
     */
    public void annotateTo(AnnotatedOutput out, String prefix) {
        out.annotate(0, prefix + "visibility: " +
                annotation.getVisibility().toHuman());
        out.annotate(0, prefix + "type: " + annotation.getType().toHuman());

        for (NameValuePair pair : annotation.getNameValuePairs()) {
            CstString name = pair.getName();
            Constant value = pair.getValue();

            out.annotate(0, prefix + name.toHuman() + ": " +
                    ValueEncoder.constantToHuman(value));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();
        AnnotationVisibility visibility = annotation.getVisibility();

        if (annotates) {
            out.annotate(0, offsetString() + " annotation");
            out.annotate(1, "  visibility: VISBILITY_" + visibility);
        }

        switch (visibility) {
            case BUILD:   out.writeByte(VISIBILITY_BUILD); break;
            case RUNTIME: out.writeByte(VISIBILITY_RUNTIME); break;
            case SYSTEM:  out.writeByte(VISIBILITY_SYSTEM); break;
            default: {
                // EMBEDDED shouldn't appear at the top level.
                throw new RuntimeException("shouldn't happen");
            }
        }

        if (annotates) {
            /*
             * The output is to be annotated, so redo the work previously
             * done by place0(), except this time annotations will actually
             * get emitted.
             */
            ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeAnnotation(annotation, true);
        } else {
            out.write(encodedForm);
        }
    }
}
