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

package external.com.android.dx.cf.iface;

/**
 * Interface for lists of attributes.
 */
public interface AttributeList {
    /**
     * Get whether this instance is mutable. Note that the
     * {@code AttributeList} interface itself doesn't provide any means
     * of mutation, but that doesn't mean that there isn't a non-interface
     * way of mutating an instance.
     *
     * @return {@code true} iff this instance is somehow mutable
     */
    public boolean isMutable();

    /**
     * Get the number of attributes in the list.
     *
     * @return the size
     */
    public int size();

    /**
     * Get the {@code n}th attribute.
     *
     * @param n {@code n >= 0, n < size();} which attribute
     * @return {@code non-null;} the attribute in question
     */
    public Attribute get(int n);

    /**
     * Get the total length of this list in bytes, when part of a
     * class file. The returned value includes the two bytes for the
     * {@code attributes_count} length indicator.
     *
     * @return {@code >= 2;} the total length, in bytes
     */
    public int byteLength();

    /**
     * Get the first attribute in the list with the given name, if any.
     *
     * @param name {@code non-null;} attribute name
     * @return {@code null-ok;} first attribute in the list with the given name,
     * or {@code null} if there is none
     */
    public Attribute findFirst(String name);

    /**
     * Get the next attribute in the list after the given one, with the same
     * name, if any.
     *
     * @param attrib {@code non-null;} attribute to start looking after
     * @return {@code null-ok;} next attribute after {@code attrib} with the
     * same name as {@code attrib}
     */
    public Attribute findNext(Attribute attrib);
}
