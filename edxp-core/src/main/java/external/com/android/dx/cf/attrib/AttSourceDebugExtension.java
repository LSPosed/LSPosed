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
 * Attribute class for standard {@code SourceDebugExtension} attributes.
 */
public final class AttSourceDebugExtension extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "SourceDebugExtension";

    /** {@code non-null;} Contents of SMAP */
    private final CstString smapString;

    /**
     * Constructs an instance.
     *
     * @param smapString {@code non-null;} the SMAP data from the class file.
     */
    public AttSourceDebugExtension(CstString smapString) {
        super(ATTRIBUTE_NAME);

        if (smapString == null) {
            throw new NullPointerException("smapString == null");
        }

        this.smapString = smapString;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        // Add 6 for the standard attribute header: the attribute name
        // index (2 bytes) and the attribute length (4 bytes).
        return 6 + smapString.getUtf8Size();
    }

    /**
     * Gets the SMAP data of this instance.
     *
     * @return {@code non-null;} the SMAP data.
     */
    public CstString getSmapString() {
        return smapString;
    }
}
