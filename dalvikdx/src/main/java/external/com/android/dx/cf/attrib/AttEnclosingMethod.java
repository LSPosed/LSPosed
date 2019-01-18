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

import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstType;

/**
 * Attribute class for standards-track {@code EnclosingMethod}
 * attributes.
 */
public final class AttEnclosingMethod extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "EnclosingMethod";

    /** {@code non-null;} the innermost enclosing class */
    private final CstType type;

    /** {@code null-ok;} the name-and-type of the innermost enclosing method, if any */
    private final CstNat method;

    /**
     * Constructs an instance.
     *
     * @param type {@code non-null;} the innermost enclosing class
     * @param method {@code null-ok;} the name-and-type of the innermost enclosing
     * method, if any
     */
    public AttEnclosingMethod(CstType type, CstNat method) {
        super(ATTRIBUTE_NAME);

        if (type == null) {
            throw new NullPointerException("type == null");
        }

        this.type = type;
        this.method = method;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        return 10;
    }

    /**
     * Gets the innermost enclosing class.
     *
     * @return {@code non-null;} the innermost enclosing class
     */
    public CstType getEnclosingClass() {
        return type;
    }

    /**
     * Gets the name-and-type of the innermost enclosing method, if
     * any.
     *
     * @return {@code null-ok;} the name-and-type of the innermost enclosing
     * method, if any
     */
    public CstNat getMethod() {
        return method;
    }
}
