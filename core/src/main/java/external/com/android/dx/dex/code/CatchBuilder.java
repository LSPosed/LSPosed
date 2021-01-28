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

package external.com.android.dx.dex.code;

import external.com.android.dx.rop.type.Type;
import java.util.HashSet;

/**
 * Interface for the construction of {@link CatchTable} instances.
 */
public interface CatchBuilder {
    /**
     * Builds and returns the catch table for this instance.
     *
     * @return {@code non-null;} the constructed table
     */
    public CatchTable build();

    /**
     * Gets whether this instance has any catches at all (either typed
     * or catch-all).
     *
     * @return whether this instance has any catches at all
     */
    public boolean hasAnyCatches();

    /**
     * Gets the set of catch types associated with this instance.
     *
     * @return {@code non-null;} the set of catch types
     */
    public HashSet<Type> getCatchTypes();
}
