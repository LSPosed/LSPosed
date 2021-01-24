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

package external.com.android.dx.dex.file;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstBaseMethodRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Method refs list section of a {@code .dex} file.
 */
public final class MethodIdsSection extends MemberIdsSection {
    /**
     * {@code non-null;} map from method constants to {@link
     * MethodIdItem} instances
     */
    private final TreeMap<CstBaseMethodRef, MethodIdItem> methodIds;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public MethodIdsSection(DexFile file) {
        super("method_ids", file);

        methodIds = new TreeMap<CstBaseMethodRef, MethodIdItem>();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return methodIds.values();
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        throwIfNotPrepared();

        IndexedItem result = methodIds.get((CstBaseMethodRef) cst);

        if (result == null) {
            throw new IllegalArgumentException("not found");
        }

        return result;
    }

    /**
     * Writes the portion of the file header that refers to this instance.
     *
     * @param out {@code non-null;} where to write
     */
    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();

        int sz = methodIds.size();
        int offset = (sz == 0) ? 0 : getFileOffset();

        if (out.annotates()) {
            out.annotate(4, "method_ids_size: " + Hex.u4(sz));
            out.annotate(4, "method_ids_off:  " + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Interns an element into this instance.
     *
     * @param method {@code non-null;} the reference to intern
     * @return {@code non-null;} the interned reference
     */
    public synchronized MethodIdItem intern(CstBaseMethodRef method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        throwIfPrepared();

        MethodIdItem result = methodIds.get(method);

        if (result == null) {
            result = new MethodIdItem(method);
            methodIds.put(method, result);
        }

        return result;
    }

    /**
     * Gets the index of the given reference, which must have been added
     * to this instance.
     *
     * @param ref {@code non-null;} the reference to look up
     * @return {@code >= 0;} the reference's index
     */
    public int indexOf(CstBaseMethodRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }

        throwIfNotPrepared();

        MethodIdItem item = methodIds.get(ref);

        if (item == null) {
            throw new IllegalArgumentException("not found");
        }

        return item.getIndex();
    }
}
