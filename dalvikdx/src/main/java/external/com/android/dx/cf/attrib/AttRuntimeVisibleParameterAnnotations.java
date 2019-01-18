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

/**
 * Attribute class for standard {@code RuntimeVisibleParameterAnnotations}
 * attributes.
 */
public final class AttRuntimeVisibleParameterAnnotations
        extends BaseParameterAnnotations {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME =
        "RuntimeVisibleParameterAnnotations";

    /**
     * Constructs an instance.
     *
     * @param annotations {@code non-null;} the parameter annotations
     * @param byteLength {@code >= 0;} attribute data length in the original
     * classfile (not including the attribute header)
     */
    public AttRuntimeVisibleParameterAnnotations(
            AnnotationsList annotations, int byteLength) {
        super(ATTRIBUTE_NAME, annotations, byteLength);
    }
}
