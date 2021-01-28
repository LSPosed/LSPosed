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

import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.MutabilityException;

/**
 * Attribute class for standard {@code Exceptions} attributes.
 */
public final class AttExceptions extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "Exceptions";

    /** {@code non-null;} list of exception classes */
    private final TypeList exceptions;

    /**
     * Constructs an instance.
     *
     * @param exceptions {@code non-null;} list of classes, presumed but not
     * verified to be subclasses of {@code Throwable}
     */
    public AttExceptions(TypeList exceptions) {
        super(ATTRIBUTE_NAME);

        try {
            if (exceptions.isMutable()) {
                throw new MutabilityException("exceptions.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("exceptions == null");
        }

        this.exceptions = exceptions;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8 + exceptions.size() * 2;
    }

    /**
     * Gets the list of classes associated with this instance. In
     * general, these classes are not pre-verified to be subclasses of
     * {@code Throwable}.
     *
     * @return {@code non-null;} the list of classes
     */
    public TypeList getExceptions() {
        return exceptions;
    }
}
