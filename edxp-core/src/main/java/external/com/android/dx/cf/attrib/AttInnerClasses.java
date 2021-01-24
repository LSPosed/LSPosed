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

package external.com.android.dx.cf.attrib;

import external.com.android.dx.util.MutabilityException;

/**
 * Attribute class for standard {@code InnerClasses} attributes.
 */
public final class AttInnerClasses extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "InnerClasses";

    /** {@code non-null;} list of inner class entries */
    private final InnerClassList innerClasses;

    /**
     * Constructs an instance.
     *
     * @param innerClasses {@code non-null;} list of inner class entries
     */
    public AttInnerClasses(InnerClassList innerClasses) {
        super(ATTRIBUTE_NAME);

        try {
            if (innerClasses.isMutable()) {
                throw new MutabilityException("innerClasses.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("innerClasses == null");
        }

        this.innerClasses = innerClasses;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8 + innerClasses.size() * 8;
    }

    /**
     * Gets the list of "inner class" entries associated with this instance.
     *
     * @return {@code non-null;} the list
     */
    public InnerClassList getInnerClasses() {
        return innerClasses;
    }
}
