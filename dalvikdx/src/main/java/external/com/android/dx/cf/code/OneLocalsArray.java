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

package external.com.android.dx.cf.code;

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.Hex;

/**
 * Representation of an array of local variables, with Java semantics.
 *
 * <p><b>Note:</b> For the most part, the documentation for this class
 * ignores the distinction between {@link external.com.android.dx.rop.type.Type} and {@link
 * external.com.android.dx.rop.type.TypeBearer}.</p>
 */
public class OneLocalsArray extends LocalsArray {
    /** {@code non-null;} actual array */
    private final TypeBearer[] locals;

    /**
     * Constructs an instance. The locals array initially consists of
     * all-uninitialized values (represented as {@code null}s).
     *
     * @param maxLocals {@code >= 0;} the maximum number of locals this instance
     * can refer to
     */
    public OneLocalsArray(int maxLocals) {
        super(maxLocals != 0);
        locals = new TypeBearer[maxLocals];
    }

    /** {@inheritDoc} */
    @Override
    public OneLocalsArray copy() {
        OneLocalsArray result = new OneLocalsArray(locals.length);

        System.arraycopy(locals, 0, result.locals, 0, locals.length);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void annotate(ExceptionWithContext ex) {
        for (int i = 0; i < locals.length; i++) {
            TypeBearer type = locals[i];
            String s = (type == null) ? "<invalid>" : type.toString();
            ex.addContext("locals[" + Hex.u2(i) + "]: " + s);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < locals.length; i++) {
            TypeBearer type = locals[i];
            String s = (type == null) ? "<invalid>" : type.toString();
            sb.append("locals[" + Hex.u2(i) + "]: " + s + "\n");
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void makeInitialized(Type type) {
        int len = locals.length;

        if (len == 0) {
            // We have to check for this before checking for immutability.
            return;
        }

        throwIfImmutable();

        Type initializedType = type.getInitializedType();

        for (int i = 0; i < len; i++) {
            if (locals[i] == type) {
                locals[i] = initializedType;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLocals() {
        return locals.length;
    }

    /** {@inheritDoc} */
    @Override
    public void set(int idx, TypeBearer type) {
        throwIfImmutable();

        try {
            type = type.getFrameType();
        } catch (NullPointerException ex) {
            // Elucidate the exception
            throw new NullPointerException("type == null");
        }

        if (idx < 0) {
            throw new IndexOutOfBoundsException("idx < 0");
        }

        // Make highest possible out-of-bounds check happen first.
        if (type.getType().isCategory2()) {
            locals[idx + 1] = null;
        }

        locals[idx] = type;

        if (idx != 0) {
            TypeBearer prev = locals[idx - 1];
            if ((prev != null) && prev.getType().isCategory2()) {
                locals[idx - 1] = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(RegisterSpec spec) {
        set(spec.getReg(), spec);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate(int idx) {
        throwIfImmutable();
        locals[idx] = null;
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer getOrNull(int idx) {
        return locals[idx];
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer get(int idx) {
        TypeBearer result = locals[idx];

        if (result == null) {
            return throwSimException(idx, "invalid");
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer getCategory1(int idx) {
        TypeBearer result = get(idx);
        Type type = result.getType();

        if (type.isUninitialized()) {
            return throwSimException(idx, "uninitialized instance");
        }

        if (type.isCategory2()) {
            return throwSimException(idx, "category-2");
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer getCategory2(int idx) {
        TypeBearer result = get(idx);

        if (result.getType().isCategory1()) {
            return throwSimException(idx, "category-1");
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public LocalsArray merge(LocalsArray other) {
        if (other instanceof OneLocalsArray) {
            return merge((OneLocalsArray)other);
        } else { //LocalsArraySet
            // LocalsArraySet knows how to merge me.
            return other.merge(this);
        }
    }

    /**
     * Merges this OneLocalsArray instance with another OneLocalsArray
     * instance. A more-refined version of {@link #merge(LocalsArray) merge}
     * which is called by that method when appropriate.
     *
     * @param other locals array with which to merge
     * @return this instance if merge was a no-op, or a new instance if
     * the merge resulted in a change.
     */
    public OneLocalsArray merge(OneLocalsArray other) {
        try {
            return Merger.mergeLocals(this, other);
        } catch (SimException ex) {
            ex.addContext("underlay locals:");
            annotate(ex);
            ex.addContext("overlay locals:");
            other.annotate(ex);
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public LocalsArraySet mergeWithSubroutineCaller
            (LocalsArray other, int predLabel) {

        LocalsArraySet result = new LocalsArraySet(getMaxLocals());
        return result.mergeWithSubroutineCaller(other, predLabel);
    }

    /**{@inheritDoc}*/
    @Override
    protected OneLocalsArray getPrimary() {
        return this;
    }

    /**
     * Throws a properly-formatted exception.
     *
     * @param idx the salient local index
     * @param msg {@code non-null;} useful message
     * @return never (keeps compiler happy)
     */
    private static TypeBearer throwSimException(int idx, String msg) {
        throw new SimException("local " + Hex.u2(idx) + ": " + msg);
    }
}
