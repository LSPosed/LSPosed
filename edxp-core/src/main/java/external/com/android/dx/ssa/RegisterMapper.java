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

package external.com.android.dx.ssa;

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.RegisterSpecSet;

/**
 * Represents a mapping between two register numbering schemes.
 * Subclasses of this may be mutable, and as such the mapping provided
 * is only valid for the lifetime of the method call in which
 * instances of this class are passed.
 */
public abstract class RegisterMapper {
    /**
     * Gets the count of registers (really, the total register width, since
     * category width is counted) in the new namespace.
     * @return &ge; 0 width of new namespace.
     */
    public abstract int getNewRegisterCount();

    /**
     * @param registerSpec old register
     * @return register in new space
     */
    public abstract RegisterSpec map(RegisterSpec registerSpec);

    /**
     *
     * @param sources old register list
     * @return new mapped register list, or old if nothing has changed.
     */
    public final RegisterSpecList map(RegisterSpecList sources) {
        int sz = sources.size();
        RegisterSpecList newSources = new RegisterSpecList(sz);

        for (int i = 0; i < sz; i++) {
            newSources.set(i, map(sources.get(i)));
        }

        newSources.setImmutable();

        // Return the old sources if nothing has changed.
        return newSources.equals(sources) ? sources : newSources;
    }

    /**
     *
     * @param sources old register set
     * @return new mapped register set, or old if nothing has changed.
     */
    public final RegisterSpecSet map(RegisterSpecSet sources) {
        int sz = sources.getMaxSize();
        RegisterSpecSet newSources = new RegisterSpecSet(getNewRegisterCount());

        for (int i = 0; i < sz; i++) {
            RegisterSpec registerSpec = sources.get(i);
            if (registerSpec != null) {
                newSources.put(map(registerSpec));
            }
        }

        newSources.setImmutable();

        // Return the old sources if nothing has changed.
        return newSources.equals(sources) ? sources : newSources;
    }
}
