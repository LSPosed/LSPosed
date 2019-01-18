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

package external.com.android.dx.util;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * Utilities for dealing with {@code Writer}s.
 */
public final class Writers {
    /**
     * This class is uninstantiable.
     */
    private Writers() {
        // This space intentionally left blank.
    }

    /**
     * Makes a {@code PrintWriter} for the given {@code Writer},
     * returning the given writer if it already happens to be the right
     * class.
     *
     * @param writer {@code non-null;} writer to (possibly) wrap
     * @return {@code non-null;} an appropriate instance
     */
    public static PrintWriter printWriterFor(Writer writer) {
        if (writer instanceof PrintWriter) {
            return (PrintWriter) writer;
        }

        return new PrintWriter(writer);
    }
}
