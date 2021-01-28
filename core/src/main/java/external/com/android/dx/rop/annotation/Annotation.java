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

import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.MutabilityControl;
import external.com.android.dx.util.ToHuman;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * An annotation on an element of a class. Annotations have an
 * associated type and additionally consist of a set of (name, value)
 * pairs, where the names are unique.
 */
public final class Annotation extends MutabilityControl
        implements Comparable<Annotation>, ToHuman {
    /** {@code non-null;} type of the annotation */
    private final CstType type;

    /** {@code non-null;} the visibility of the annotation */
    private final AnnotationVisibility visibility;

    /** {@code non-null;} map from names to {@link NameValuePair} instances */
    private final TreeMap<CstString, NameValuePair> elements;

    /**
     * Construct an instance. It initially contains no elements.
     *
     * @param type {@code non-null;} type of the annotation
     * @param visibility {@code non-null;} the visibility of the annotation
     */
    public Annotation(CstType type, AnnotationVisibility visibility) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }

        if (visibility == null) {
            throw new NullPointerException("visibility == null");
        }

        this.type = type;
        this.visibility = visibility;
        this.elements = new TreeMap<CstString, NameValuePair>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof Annotation)) {
            return false;
        }

        Annotation otherAnnotation = (Annotation) other;

        if (! (type.equals(otherAnnotation.type)
                        && (visibility == otherAnnotation.visibility))) {
            return false;
        }

        return elements.equals(otherAnnotation.elements);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = type.hashCode();
        hash = (hash * 31) + elements.hashCode();
        hash = (hash * 31) + visibility.hashCode();
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Annotation other) {
        int result = type.compareTo(other.type);

        if (result != 0) {
            return result;
        }

        result = visibility.compareTo(other.visibility);

        if (result != 0) {
            return result;
        }

        Iterator<NameValuePair> thisIter = elements.values().iterator();
        Iterator<NameValuePair> otherIter = other.elements.values().iterator();

        while (thisIter.hasNext() && otherIter.hasNext()) {
            NameValuePair thisOne = thisIter.next();
            NameValuePair otherOne = otherIter.next();

            result = thisOne.compareTo(otherOne);
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
        return toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        StringBuilder sb = new StringBuilder();

        sb.append(visibility.toHuman());
        sb.append("-annotation ");
        sb.append(type.toHuman());
        sb.append(" {");

        boolean first = true;
        for (NameValuePair pair : elements.values()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(pair.getName().toHuman());
            sb.append(": ");
            sb.append(pair.getValue().toHuman());
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets the type of this instance.
     *
     * @return {@code non-null;} the type
     */
    public CstType getType() {
        return type;
    }

    /**
     * Gets the visibility of this instance.
     *
     * @return {@code non-null;} the visibility
     */
    public AnnotationVisibility getVisibility() {
        return visibility;
    }

    /**
     * Put an element into the set of (name, value) pairs for this instance.
     * If there is a preexisting element with the same name, it will be
     * replaced by this method.
     *
     * @param pair {@code non-null;} the (name, value) pair to place into this instance
     */
    public void put(NameValuePair pair) {
        throwIfImmutable();

        if (pair == null) {
            throw new NullPointerException("pair == null");
        }

        elements.put(pair.getName(), pair);
    }

    /**
     * Add an element to the set of (name, value) pairs for this instance.
     * It is an error to call this method if there is a preexisting element
     * with the same name.
     *
     * @param pair {@code non-null;} the (name, value) pair to add to this instance
     */
    public void add(NameValuePair pair) {
        throwIfImmutable();

        if (pair == null) {
            throw new NullPointerException("pair == null");
        }

        CstString name = pair.getName();

        if (elements.get(name) != null) {
            throw new IllegalArgumentException("name already added: " + name);
        }

        elements.put(name, pair);
    }

    /**
     * Gets the set of name-value pairs contained in this instance. The
     * result is always unmodifiable.
     *
     * @return {@code non-null;} the set of name-value pairs
     */
    public Collection<NameValuePair> getNameValuePairs() {
        return Collections.unmodifiableCollection(elements.values());
    }
}
