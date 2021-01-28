/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dx.rop.type.StdTypeList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An immutable of types.
 */
final class TypeList {
    final TypeId<?>[] types;
    final StdTypeList ropTypes;

    TypeList(TypeId<?>[] types) {
        this.types = types.clone();
        this.ropTypes = new StdTypeList(types.length);
        for (int i = 0; i < types.length; i++) {
            ropTypes.set(i, types[i].ropType);
        }
    }

    /**
     * Returns an immutable list.
     */
    public List<TypeId<?>> asList() {
        return Collections.unmodifiableList(Arrays.asList(types));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TypeList && Arrays.equals(((TypeList) o).types, types);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(types[i]);
        }
        return result.toString();
    }
}
