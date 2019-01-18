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
 * Interface representing attributes of class files (directly or indirectly).
 */
public interface Attribute {
    /**
     * Get the name of the attribute.
     *
     * @return {@code non-null;} the name
     */
    public String getName();

    /**
     * Get the total length of the attribute in bytes, including the
     * header. Since the header is always six bytes, the result of
     * this method is always at least {@code 6}.
     *
     * @return {@code >= 6;} the total length, in bytes
     */
    public int byteLength();
}
