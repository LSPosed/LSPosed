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

import external.com.android.dx.util.ToHuman;

/**
 * Base class for constants of all sorts.
 */
public abstract class Constant
        implements ToHuman, Comparable<Constant> {
    /**
     * Returns {@code true} if this instance is a category-2 constant,
     * meaning it takes up two slots in the constant pool, or
     * {@code false} if this instance is category-1.
     *
     * @return {@code true} iff this instance is category-2
     */
    public abstract boolean isCategory2();

    /**
     * Returns the human name for the particular type of constant
     * this instance is.
     *
     * @return {@code non-null;} the name
     */
    public abstract String typeName();

    /**
     * {@inheritDoc}
     *
     * This compares in class-major and value-minor order.
     */
    @Override
    public final int compareTo(Constant other) {
        Class clazz = getClass();
        Class otherClazz = other.getClass();

        if (clazz != otherClazz) {
            return clazz.getName().compareTo(otherClazz.getName());
        }

        return compareTo0(other);
    }

    /**
     * Compare the values of this and another instance, which are guaranteed
     * to be of the same class. Subclasses must implement this.
     *
     * @param other {@code non-null;} the instance to compare to
     * @return {@code -1}, {@code 0}, or {@code 1}, as usual
     * for a comparison
     */
    protected abstract int compareTo0(Constant other);
}
