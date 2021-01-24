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

import external.com.android.dx.rop.cst.CstString;

/**
 * Attribute class for standard {@code SourceFile} attributes.
 */
public final class AttSourceFile extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "SourceFile";

    /** {@code non-null;} name of the source file */
    private final CstString sourceFile;

    /**
     * Constructs an instance.
     *
     * @param sourceFile {@code non-null;} the name of the source file
     */
    public AttSourceFile(CstString sourceFile) {
        super(ATTRIBUTE_NAME);

        if (sourceFile == null) {
            throw new NullPointerException("sourceFile == null");
        }

        this.sourceFile = sourceFile;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8;
    }

    /**
     * Gets the source file name of this instance.
     *
     * @return {@code non-null;} the source file
     */
    public CstString getSourceFile() {
        return sourceFile;
    }
}
