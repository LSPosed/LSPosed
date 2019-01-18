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
import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.ToHuman;
import java.util.ArrayList;

/**
 * Association of a method and its parameter annotations.
 */
public final class ParameterAnnotationStruct
        implements ToHuman, Comparable<ParameterAnnotationStruct> {
    /** {@code non-null;} the method in question */
    private final CstMethodRef method;

    /** {@code non-null;} the associated annotations list */
    private final AnnotationsList annotationsList;

    /** {@code non-null;} the associated annotations list, as an item */
    private final UniformListItem<AnnotationSetRefItem> annotationsItem;

    /**
     * Constructs an instance.
     *
     * @param method {@code non-null;} the method in question
     * @param annotationsList {@code non-null;} the associated annotations list
     * @param dexFile {@code non-null;} dex output
     */
    public ParameterAnnotationStruct(CstMethodRef method,
            AnnotationsList annotationsList, DexFile dexFile) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (annotationsList == null) {
            throw new NullPointerException("annotationsList == null");
        }

        this.method = method;
        this.annotationsList = annotationsList;

        /*
         * Construct an item for the annotations list. TODO: This
         * requires way too much copying; fix it.
         */

        int size = annotationsList.size();
        ArrayList<AnnotationSetRefItem> arrayList = new
            ArrayList<AnnotationSetRefItem>(size);

        for (int i = 0; i < size; i++) {
            Annotations annotations = annotationsList.get(i);
            AnnotationSetItem item = new AnnotationSetItem(annotations, dexFile);
            arrayList.add(new AnnotationSetRefItem(item));
        }

        this.annotationsItem = new UniformListItem<AnnotationSetRefItem>(
                ItemType.TYPE_ANNOTATION_SET_REF_LIST, arrayList);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return method.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof ParameterAnnotationStruct)) {
            return false;
        }

        return method.equals(((ParameterAnnotationStruct) other).method);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(ParameterAnnotationStruct other) {
        return method.compareTo(other.method);
    }

    /** {@inheritDoc} */
    public void addContents(DexFile file) {
        MethodIdsSection methodIds = file.getMethodIds();
        MixedItemSection wordData = file.getWordData();

        methodIds.intern(method);
        wordData.add(annotationsItem);
    }

    /** {@inheritDoc} */
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int methodIdx = file.getMethodIds().indexOf(method);
        int annotationsOff = annotationsItem.getAbsoluteOffset();

        if (out.annotates()) {
            out.annotate(0, "    " + method.toHuman());
            out.annotate(4, "      method_idx:      " + Hex.u4(methodIdx));
            out.annotate(4, "      annotations_off: " +
                    Hex.u4(annotationsOff));
        }

        out.writeInt(methodIdx);
        out.writeInt(annotationsOff);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        StringBuilder sb = new StringBuilder();

        sb.append(method.toHuman());
        sb.append(": ");

        boolean first = true;
        for (AnnotationSetRefItem item : annotationsItem.getItems()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(item.toHuman());
        }

        return sb.toString();
    }

    /**
     * Gets the method this item is for.
     *
     * @return {@code non-null;} the method
     */
    public CstMethodRef getMethod() {
        return method;
    }

    /**
     * Gets the associated annotations list.
     *
     * @return {@code non-null;} the annotations list
     */
    public AnnotationsList getAnnotationsList() {
        return annotationsList;
    }
}
