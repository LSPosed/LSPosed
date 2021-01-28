/*
 * Copyright (C) 2013 The Android Open Source Project
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

package external.com.android.multidex;

import java.io.IOException;
import java.io.InputStream;

/**
 * An element of the class path in which class files can be found.
 */
interface ClassPathElement {

    char SEPARATOR_CHAR = '/';

    /**
     * Open a "file" from this {@code ClassPathElement}.
     * @param path a '/' separated relative path to the wanted file.
     * @return an {@code InputStream} ready to read the requested file.
     * @throws IOException if the path can not be found or if an error occurred while opening it.
     */
    InputStream open(String path) throws IOException;

    void close() throws IOException;

    Iterable<String> list();

}
