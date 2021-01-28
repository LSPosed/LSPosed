/*
 * Copyright (C) 2008 The Android Open Source Project
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

package external.com.android.dx.rop.annotation;

import external.com.android.dx.util.ToHuman;

/**
 * Visibility scope of an annotation.
 */
public enum AnnotationVisibility implements ToHuman {
    RUNTIME("runtime"),
    BUILD("build"),
    SYSTEM("system"),
    EMBEDDED("embedded");

    /** {@code non-null;} the human-oriented string representation */
    private final String human;

    /**
     * Constructs an instance.
     *
     * @param human {@code non-null;} the human-oriented string representation
     */
    private AnnotationVisibility(String human) {
        this.human = human;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return human;
    }
}
