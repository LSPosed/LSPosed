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
 * Interface for lists of methods.
 */
public interface MethodList {
    /**
     * Get whether this instance is mutable. Note that the
     * {@code MethodList} interface itself doesn't provide any means
     * of mutation, but that doesn't mean that there isn't a non-interface
     * way of mutating an instance.
     *
     * @return {@code true} iff this instance is somehow mutable
     */
    public boolean isMutable();

    /**
     * Get the number of methods in the list.
     *
     * @return the size
     */
    public int size();

    /**
     * Get the {@code n}th method.
     *
     * @param n {@code n >= 0, n < size();} which method
     * @return {@code non-null;} the method in question
     */
    public Method get(int n);
}
