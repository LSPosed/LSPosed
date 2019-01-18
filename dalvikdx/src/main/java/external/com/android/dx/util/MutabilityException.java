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

import external.com.android.dex.util.ExceptionWithContext;

/**
 * Exception due to a mutability problem.
 */
public class MutabilityException
        extends ExceptionWithContext {
    public MutabilityException(String message) {
        super(message);
    }

    public MutabilityException(Throwable cause) {
        super(cause);
    }

    public MutabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
