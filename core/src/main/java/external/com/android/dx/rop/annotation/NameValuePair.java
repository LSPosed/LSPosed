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

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstString;

/**
 * A (name, value) pair. These are used as the contents of an annotation.
 */
public final class NameValuePair implements Comparable<NameValuePair> {
    /** {@code non-null;} the name */
    private final CstString name;

    /** {@code non-null;} the value */
    private final Constant value;

    /**
     * Construct an instance.
     *
     * @param name {@code non-null;} the name
     * @param value {@code non-null;} the value
     */
    public NameValuePair(CstString name, Constant value) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }

        if (value == null) {
            throw new NullPointerException("value == null");
        }

        this.name = name;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name.toHuman() + ":" + value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return name.hashCode() * 31 + value.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof NameValuePair)) {
            return false;
        }

        NameValuePair otherPair = (NameValuePair) other;

        return name.equals(otherPair.name)
            && value.equals(otherPair.value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Instances of this class compare in name-major and value-minor
     * order.</p>
     */
    @Override
    public int compareTo(NameValuePair other) {
        int result = name.compareTo(other.name);

        if (result != 0) {
            return result;
        }

        return value.compareTo(other.value);
    }

    /**
     * Gets the name.
     *
     * @return {@code non-null;} the name
     */
    public CstString getName() {
        return name;
    }

    /**
     * Gets the value.
     *
     * @return {@code non-null;} the value
     */
    public Constant getValue() {
        return value;
    }
}
