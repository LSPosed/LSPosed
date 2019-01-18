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

import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.util.MutabilityException;

/**
 * Base class for annotations attributes.
 */
public abstract class BaseAnnotations extends BaseAttribute {
    /** {@code non-null;} list of annotations */
    private final Annotations annotations;

    /** {@code >= 0;} attribute data length in the original classfile (not
     * including the attribute header) */
    private final int byteLength;

    /**
     * Constructs an instance.
     *
     * @param attributeName {@code non-null;} the name of the attribute
     * @param annotations {@code non-null;} the list of annotations
     * @param byteLength {@code >= 0;} attribute data length in the original
     * classfile (not including the attribute header)
     */
    public BaseAnnotations(String attributeName, Annotations annotations,
            int byteLength) {
        super(attributeName);

        try {
            if (annotations.isMutable()) {
                throw new MutabilityException("annotations.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("annotations == null");
        }

        this.annotations = annotations;
        this.byteLength = byteLength;
    }

    /** {@inheritDoc} */
    @Override
    public final int byteLength() {
        // Add six for the standard attribute header.
        return byteLength + 6;
    }

    /**
     * Gets the list of annotations associated with this instance.
     *
     * @return {@code non-null;} the list
     */
    public final Annotations getAnnotations() {
        return annotations;
    }
}
