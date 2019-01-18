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

package external.com.android.dx.rop.cst;

import external.com.android.dx.rop.annotation.Annotation;

/**
 * Constant type that represents an annotation.
 */
public final class CstAnnotation extends Constant {
    /** {@code non-null;} the actual annotation */
    private final Annotation annotation;

    /**
     * Constructs an instance.
     *
     * @param annotation {@code non-null;} the annotation to hold
     */
    public CstAnnotation(Annotation annotation) {
        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }

        annotation.throwIfMutable();

        this.annotation = annotation;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof CstAnnotation)) {
            return false;
        }

        return annotation.equals(((CstAnnotation) other).annotation);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return annotation.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        return annotation.compareTo(((CstAnnotation) other).annotation);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return annotation.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "annotation";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return annotation.toString();
    }

    /**
     * Get the underlying annotation.
     *
     * @return {@code non-null;} the annotation
     */
    public Annotation getAnnotation() {
        return annotation;
    }
}
