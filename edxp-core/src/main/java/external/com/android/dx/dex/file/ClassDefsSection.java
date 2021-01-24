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
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Class definitions list section of a {@code .dex} file.
 */
public final class ClassDefsSection extends UniformItemSection {
    /**
     * {@code non-null;} map from type constants for classes to {@link
     * ClassDefItem} instances that define those classes
     */
    private final TreeMap<Type, ClassDefItem> classDefs;

    /** {@code null-ok;} ordered list of classes; set in {@link #orderItems} */
    private ArrayList<ClassDefItem> orderedDefs;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public ClassDefsSection(DexFile file) {
        super("class_defs", file, 4);

        classDefs = new TreeMap<Type, ClassDefItem>();
        orderedDefs = null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        if (orderedDefs != null) {
            return orderedDefs;
        }

        return classDefs.values();
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        throwIfNotPrepared();

        Type type = ((CstType) cst).getClassType();
        IndexedItem result = classDefs.get(type);

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

        int sz = classDefs.size();
        int offset = (sz == 0) ? 0 : getFileOffset();

        if (out.annotates()) {
            out.annotate(4, "class_defs_size: " + Hex.u4(sz));
            out.annotate(4, "class_defs_off:  " + Hex.u4(offset));
        }

        out.writeInt(sz);
        out.writeInt(offset);
    }

    /**
     * Adds an element to this instance. It is illegal to attempt to add more
     * than one class with the same name.
     *
     * @param clazz {@code non-null;} the class def to add
     */
    public void add(ClassDefItem clazz) {
        Type type;

        try {
            type = clazz.getThisClass().getClassType();
        } catch (NullPointerException ex) {
            // Elucidate the exception.
            throw new NullPointerException("clazz == null");
        }

        throwIfPrepared();

        if (classDefs.get(type) != null) {
            throw new IllegalArgumentException("already added: " + type);
        }

        classDefs.put(type, clazz);
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int sz = classDefs.size();
        int idx = 0;

        orderedDefs = new ArrayList<ClassDefItem>(sz);

        /*
         * Iterate over all the classes, recursively assigning an
         * index to each, implicitly skipping the ones that have
         * already been assigned by the time this (top-level)
         * iteration reaches them.
         */
        for (Type type : classDefs.keySet()) {
            idx = orderItems0(type, idx, sz - idx);
        }
    }

    /**
     * Helper for {@link #orderItems}, which recursively assigns indices
     * to classes.
     *
     * @param type {@code null-ok;} type ref to assign, if any
     * @param idx {@code >= 0;} the next index to assign
     * @param maxDepth maximum recursion depth; if negative, this will
     * throw an exception indicating class definition circularity
     * @return {@code >= 0;} the next index to assign
     */
    private int orderItems0(Type type, int idx, int maxDepth) {
        ClassDefItem c = classDefs.get(type);

        if ((c == null) || (c.hasIndex())) {
            return idx;
        }

        if (maxDepth < 0) {
            throw new RuntimeException("class circularity with " + type);
        }

        maxDepth--;

        CstType superclassCst = c.getSuperclass();
        if (superclassCst != null) {
            Type superclass = superclassCst.getClassType();
            idx = orderItems0(superclass, idx, maxDepth);
        }

        TypeList interfaces = c.getInterfaces();
        int sz = interfaces.size();
        for (int i = 0; i < sz; i++) {
            idx = orderItems0(interfaces.getType(i), idx, maxDepth);
        }

        c.setIndex(idx);
        orderedDefs.add(c);
        return idx + 1;
    }
}
