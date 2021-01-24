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

import external.com.android.dx.cf.code.LineNumberList;
import external.com.android.dx.util.MutabilityException;

/**
 * Attribute class for standard {@code LineNumberTable} attributes.
 */
public final class AttLineNumberTable extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "LineNumberTable";

    /** {@code non-null;} list of line number entries */
    private final LineNumberList lineNumbers;

    /**
     * Constructs an instance.
     *
     * @param lineNumbers {@code non-null;} list of line number entries
     */
    public AttLineNumberTable(LineNumberList lineNumbers) {
        super(ATTRIBUTE_NAME);

        try {
            if (lineNumbers.isMutable()) {
                throw new MutabilityException("lineNumbers.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("lineNumbers == null");
        }

        this.lineNumbers = lineNumbers;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8 + 4 * lineNumbers.size();
    }

    /**
     * Gets the list of "line number" entries associated with this instance.
     *
     * @return {@code non-null;} the list
     */
    public LineNumberList getLineNumbers() {
        return lineNumbers;
    }
}
