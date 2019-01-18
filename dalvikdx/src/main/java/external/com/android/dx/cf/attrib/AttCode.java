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

import external.com.android.dx.cf.code.ByteCatchList;
import external.com.android.dx.cf.code.BytecodeArray;
import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.util.MutabilityException;

/**
 * Attribute class for standard {@code Code} attributes.
 */
public final class AttCode extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "Code";

    /** {@code >= 0;} the stack size */
    private final int maxStack;

    /** {@code >= 0;} the number of locals */
    private final int maxLocals;

    /** {@code non-null;} array containing the bytecode per se */
    private final BytecodeArray code;

    /** {@code non-null;} the exception table */
    private final ByteCatchList catches;

    /** {@code non-null;} the associated list of attributes */
    private final AttributeList attributes;

    /**
     * Constructs an instance.
     *
     * @param maxStack {@code >= 0;} the stack size
     * @param maxLocals {@code >= 0;} the number of locals
     * @param code {@code non-null;} array containing the bytecode per se
     * @param catches {@code non-null;} the exception table
     * @param attributes {@code non-null;} the associated list of attributes
     */
    public AttCode(int maxStack, int maxLocals, BytecodeArray code,
                   ByteCatchList catches, AttributeList attributes) {
        super(ATTRIBUTE_NAME);

        if (maxStack < 0) {
            throw new IllegalArgumentException("maxStack < 0");
        }

        if (maxLocals < 0) {
            throw new IllegalArgumentException("maxLocals < 0");
        }

        if (code == null) {
            throw new NullPointerException("code == null");
        }

        try {
            if (catches.isMutable()) {
                throw new MutabilityException("catches.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("catches == null");
        }

        try {
            if (attributes.isMutable()) {
                throw new MutabilityException("attributes.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("attributes == null");
        }

        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        this.code = code;
        this.catches = catches;
        this.attributes = attributes;
    }

    @Override
    public int byteLength() {
        return 10 + code.byteLength() + catches.byteLength() +
            attributes.byteLength();
    }

    /**
     * Gets the maximum stack size.
     *
     * @return {@code >= 0;} the maximum stack size
     */
    public int getMaxStack() {
        return maxStack;
    }

    /**
     * Gets the number of locals.
     *
     * @return {@code >= 0;} the number of locals
     */
    public int getMaxLocals() {
        return maxLocals;
    }

    /**
     * Gets the bytecode array.
     *
     * @return {@code non-null;} the bytecode array
     */
    public BytecodeArray getCode() {
        return code;
    }

    /**
     * Gets the exception table.
     *
     * @return {@code non-null;} the exception table
     */
    public ByteCatchList getCatches() {
        return catches;
    }

    /**
     * Gets the associated attribute list.
     *
     * @return {@code non-null;} the attribute list
     */
    public AttributeList getAttributes() {
        return attributes;
    }
}
