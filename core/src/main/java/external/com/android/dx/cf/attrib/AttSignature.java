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
 * Attribute class for standards-track {@code Signature} attributes.
 */
public final class AttSignature extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "Signature";

    /** {@code non-null;} the signature string */
    private final CstString signature;

    /**
     * Constructs an instance.
     *
     * @param signature {@code non-null;} the signature string
     */
    public AttSignature(CstString signature) {
        super(ATTRIBUTE_NAME);

        if (signature == null) {
            throw new NullPointerException("signature == null");
        }

        this.signature = signature;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 8;
    }

    /**
     * Gets the signature string.
     *
     * @return {@code non-null;} the signature string
     */
    public CstString getSignature() {
        return signature;
    }
}
