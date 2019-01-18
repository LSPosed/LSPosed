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

package external.com.android.dx.merge;

import external.com.android.dex.ClassDef;
import external.com.android.dex.Dex;
import external.com.android.dex.DexException;
import java.util.Comparator;

/**
 * Name and structure of a type. Used to order types such that each type is
 * preceded by its supertype and implemented interfaces.
 */
final class SortableType {
    public static final Comparator<SortableType> NULLS_LAST_ORDER = new Comparator<SortableType>() {
        @Override
        public int compare(SortableType a, SortableType b) {
            if (a == b) {
                return 0;
            }
            if (b == null) {
                return -1;
            }
            if (a == null) {
                return 1;
            }
            if (a.depth != b.depth) {
                return a.depth - b.depth;
            }
            return a.getTypeIndex() - b.getTypeIndex();
        }
    };

    private final Dex dex;
    private final IndexMap indexMap;
    private final ClassDef classDef;
    private int depth = -1;

    public SortableType(Dex dex, IndexMap indexMap, ClassDef classDef) {
        this.dex = dex;
        this.indexMap = indexMap;
        this.classDef = classDef;
    }

    public Dex getDex() {
        return dex;
    }

    public IndexMap getIndexMap() {
        return indexMap;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public int getTypeIndex() {
        return classDef.getTypeIndex();
    }

    /**
     * Assigns this type's depth if the depths of its supertype and implemented
     * interfaces are known. Returns false if the depth couldn't be computed
     * yet.
     */
    public boolean tryAssignDepth(SortableType[] types) {
        int max;
        if (classDef.getSupertypeIndex() == ClassDef.NO_INDEX) {
            max = 0; // this is Object.class or an interface
        } else if (classDef.getSupertypeIndex() == classDef.getTypeIndex()) {
            // This is an invalid class extending itself.
            throw new DexException("Class with type index " + classDef.getTypeIndex()
                    + " extends itself");
        } else {
            SortableType sortableSupertype = types[classDef.getSupertypeIndex()];
            if (sortableSupertype == null) {
                max = 1; // unknown, so assume it's a root.
            } else if (sortableSupertype.depth == -1) {
                return false;
            } else {
                max = sortableSupertype.depth;
            }
        }

        for (short interfaceIndex : classDef.getInterfaces()) {
            SortableType implemented = types[interfaceIndex];
            if (implemented == null) {
                max = Math.max(max, 1); // unknown, so assume it's a root.
            } else if (implemented.depth == -1) {
                return false;
            } else {
                max = Math.max(max, implemented.depth);
            }
        }

        depth = max + 1;
        return true;
    }

    public boolean isDepthAssigned() {
        return depth != -1;
    }
}
