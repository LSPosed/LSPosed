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

package external.com.android.dx.rop.annotation;

import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.MutabilityControl;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * List of {@link Annotation} instances.
 */
public final class Annotations extends MutabilityControl
        implements Comparable<Annotations> {
    /** {@code non-null;} immutable empty instance */
    public static final Annotations EMPTY = new Annotations();

    static {
        EMPTY.setImmutable();
    }

    /** {@code non-null;} map from types to annotations */
    private final TreeMap<CstType, Annotation> annotations;

    /**
     * Constructs an immutable instance which is the combination of the
     * two given instances. The two instances must contain disjoint sets
     * of types.
     *
     * @param a1 {@code non-null;} an instance
     * @param a2 {@code non-null;} the other instance
     * @return {@code non-null;} the combination
     * @throws IllegalArgumentException thrown if there is a duplicate type
     */
    public static Annotations combine(Annotations a1, Annotations a2) {
        Annotations result = new Annotations();

        result.addAll(a1);
        result.addAll(a2);
        result.setImmutable();

        return result;
    }

    /**
     * Constructs an immutable instance which is the combination of the
     * given instance with the given additional annotation. The latter's
     * type must not already appear in the former.
     *
     * @param annotations {@code non-null;} the instance to augment
     * @param annotation {@code non-null;} the additional annotation
     * @return {@code non-null;} the combination
     * @throws IllegalArgumentException thrown if there is a duplicate type
     */
    public static Annotations combine(Annotations annotations,
            Annotation annotation) {
        Annotations result = new Annotations();

        result.addAll(annotations);
        result.add(annotation);
        result.setImmutable();

        return result;
    }

    /**
     * Constructs an empty instance.
     */
    public Annotations() {
        annotations = new TreeMap<CstType, Annotation>();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return annotations.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof Annotations)) {
            return false;
        }

        Annotations otherAnnotations = (Annotations) other;

        return annotations.equals(otherAnnotations.annotations);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Annotations other) {
        Iterator<Annotation> thisIter = annotations.values().iterator();
        Iterator<Annotation> otherIter = other.annotations.values().iterator();

        while (thisIter.hasNext() && otherIter.hasNext()) {
            Annotation thisOne = thisIter.next();
            Annotation otherOne = otherIter.next();

            int result = thisOne.compareTo(otherOne);
            if (result != 0) {
                return result;
            }
        }

        if (thisIter.hasNext()) {
            return 1;
        } else if (otherIter.hasNext()) {
            return -1;
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append("annotations{");

        for (Annotation a : annotations.values()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(a.toHuman());
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets the number of elements in this instance.
     *
     * @return {@code >= 0;} the size
     */
    public int size() {
        return annotations.size();
    }

    /**
     * Adds an element to this instance. There must not already be an
     * element of the same type.
     *
     * @param annotation {@code non-null;} the element to add
     * @throws IllegalArgumentException thrown if there is a duplicate type
     */
    public void add(Annotation annotation) {
        throwIfImmutable();

        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }

        CstType type = annotation.getType();

        if (annotations.containsKey(type)) {
            throw new IllegalArgumentException("duplicate type: " +
                    type.toHuman());
        }

        annotations.put(type, annotation);
    }

    /**
     * Adds all of the elements of the given instance to this one. The
     * instances must not have any duplicate types.
     *
     * @param toAdd {@code non-null;} the annotations to add
     * @throws IllegalArgumentException thrown if there is a duplicate type
     */
    public void addAll(Annotations toAdd) {
        throwIfImmutable();

        if (toAdd == null) {
            throw new NullPointerException("toAdd == null");
        }

        for (Annotation a : toAdd.annotations.values()) {
            add(a);
        }
    }

    /**
     * Gets the set of annotations contained in this instance. The
     * result is always unmodifiable.
     *
     * @return {@code non-null;} the set of annotations
     */
    public Collection<Annotation> getAnnotations() {
        return Collections.unmodifiableCollection(annotations.values());
    }
}
