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
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.ToHuman;

/**
 * Association of a method and its annotations.
 */
public final class MethodAnnotationStruct
        implements ToHuman, Comparable<MethodAnnotationStruct> {
    /** {@code non-null;} the method in question */
    private final CstMethodRef method;

    /** {@code non-null;} the associated annotations */
    private AnnotationSetItem annotations;

    /**
     * Constructs an instance.
     *
     * @param method {@code non-null;} the method in question
     * @param annotations {@code non-null;} the associated annotations
     */
    public MethodAnnotationStruct(CstMethodRef method,
            AnnotationSetItem annotations) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }

        this.method = method;
        this.annotations = annotations;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return method.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof MethodAnnotationStruct)) {
            return false;
        }

        return method.equals(((MethodAnnotationStruct) other).method);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(MethodAnnotationStruct other) {
        return method.compareTo(other.method);
    }

    /** {@inheritDoc} */
    public void addContents(DexFile file) {
        MethodIdsSection methodIds = file.getMethodIds();
        MixedItemSection wordData = file.getWordData();

        methodIds.intern(method);
        annotations = wordData.intern(annotations);
    }

    /** {@inheritDoc} */
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int methodIdx = file.getMethodIds().indexOf(method);
        int annotationsOff = annotations.getAbsoluteOffset();

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
        return method.toHuman() + ": " + annotations;
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
     * Gets the associated annotations.
     *
     * @return {@code non-null;} the annotations
     */
    public Annotations getAnnotations() {
        return annotations.getAnnotations();
    }
}
