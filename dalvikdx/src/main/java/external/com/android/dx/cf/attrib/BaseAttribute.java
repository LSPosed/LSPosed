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

import external.com.android.dx.cf.iface.Attribute;

/**
 * Base implementation of {@link Attribute}, which directly stores
 * the attribute name but leaves the rest up to subclasses.
 */
public abstract class BaseAttribute implements Attribute {
    /** {@code non-null;} attribute name */
    private final String name;

    /**
     * Constructs an instance.
     *
     * @param name {@code non-null;} attribute name
     */
    public BaseAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }

        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }
}
