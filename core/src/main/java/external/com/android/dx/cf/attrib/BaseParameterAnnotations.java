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

import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.util.MutabilityException;

/**
 * Base class for parameter annotation list attributes.
 */
public abstract class BaseParameterAnnotations extends BaseAttribute {
    /** {@code non-null;} list of annotations */
    private final AnnotationsList parameterAnnotations;

    /** {@code >= 0;} attribute data length in the original classfile (not
     * including the attribute header) */
    private final int byteLength;

    /**
     * Constructs an instance.
     *
     * @param attributeName {@code non-null;} the name of the attribute
     * @param parameterAnnotations {@code non-null;} the annotations
     * @param byteLength {@code >= 0;} attribute data length in the original
     * classfile (not including the attribute header)
     */
    public BaseParameterAnnotations(String attributeName,
            AnnotationsList parameterAnnotations, int byteLength) {
        super(attributeName);

        try {
            if (parameterAnnotations.isMutable()) {
                throw new MutabilityException(
                        "parameterAnnotations.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("parameterAnnotations == null");
        }

        this.parameterAnnotations = parameterAnnotations;
        this.byteLength = byteLength;
    }

    /** {@inheritDoc} */
    @Override
    public final int byteLength() {
        // Add six for the standard attribute header.
        return byteLength + 6;
    }

    /**
     * Gets the list of annotation lists associated with this instance.
     *
     * @return {@code non-null;} the list
     */
    public final AnnotationsList getParameterAnnotations() {
        return parameterAnnotations;
    }
}
