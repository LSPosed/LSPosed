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

package external.com.android.dx.dex.file;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Proto (method prototype) identifiers list section of a
 * {@code .dex} file.
 */
public final class ProtoIdsSection extends UniformItemSection {
    /**
     * {@code non-null;} map from method prototypes to {@link ProtoIdItem} instances
     */
    private final TreeMap<Prototype, ProtoIdItem> protoIds;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public ProtoIdsSection(DexFile file) {
        super("proto_ids", file, 4);

        protoIds = new TreeMap<Prototype, ProtoIdItem>();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return protoIds.values();
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        if (!(cst instanceof CstProtoRef)) {
            throw new IllegalArgumentException("cst not instance of CstProtoRef");
        }

        throwIfNotPrepared();
        CstProtoRef protoRef = (CstProtoRef) cst;
        IndexedItem result = protoIds.get(protoRef.getPrototype());
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

        int sz = protoIds.size();
        int offset = (sz == 0) ? 0 : getFileOffset();

        if (sz > 65536) {
            throw new UnsupportedOperationException("too many proto ids");
        }

        if (out.annotates()) {
            out.annotate(4, "proto_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "proto_ids_off:   " + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Interns an element into this instance.
     *
     * @param prototype {@code non-null;} the prototype to intern
     * @return {@code non-null;} the interned reference
     */
    public synchronized ProtoIdItem intern(Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }

        throwIfPrepared();

        ProtoIdItem result = protoIds.get(prototype);

        if (result == null) {
            result = new ProtoIdItem(prototype);
            protoIds.put(prototype, result);
        }

        return result;
    }

    /**
     * Gets the index of the given prototype, which must have
     * been added to this instance.
     *
     * @param prototype {@code non-null;} the prototype to look up
     * @return {@code >= 0;} the reference's index
     */
    public int indexOf(Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }

        throwIfNotPrepared();

        ProtoIdItem item = protoIds.get(prototype);

        if (item == null) {
            throw new IllegalArgumentException("not found");
        }

        return item.getIndex();
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int idx = 0;

        for (Object i : items()) {
            ((ProtoIdItem) i).setIndex(idx);
            idx++;
        }
    }
}
