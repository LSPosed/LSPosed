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

import external.com.android.dx.cf.code.LocalVariableList;
import external.com.android.dx.util.MutabilityException;

/**
 * Base attribute class for standard {@code LocalVariableTable}
 * and {@code LocalVariableTypeTable} attributes.
 */
public abstract class BaseLocalVariables extends BaseAttribute {
    /** {@code non-null;} list of local variable entries */
    private final LocalVariableList localVariables;

    /**
     * Constructs an instance.
     *
     * @param name {@code non-null;} attribute name
     * @param localVariables {@code non-null;} list of local variable entries
     */
    public BaseLocalVariables(String name,
            LocalVariableList localVariables) {
        super(name);

        try {
            if (localVariables.isMutable()) {
                throw new MutabilityException("localVariables.isMutable()");
            }
        } catch (NullPointerException ex) {
            // Translate the exception.
            throw new NullPointerException("localVariables == null");
        }

        this.localVariables = localVariables;
    }

    /** {@inheritDoc} */
    @Override
    public final int byteLength() {
        return 8 + localVariables.size() * 10;
    }

    /**
     * Gets the list of "local variable" entries associated with this instance.
     *
     * @return {@code non-null;} the list
     */
    public final LocalVariableList getLocalVariables() {
        return localVariables;
    }
}
