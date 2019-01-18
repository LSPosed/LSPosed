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
import external.com.android.dx.util.MutabilityControl;
import external.com.android.dx.util.ToHuman;

/**
 * Representation of an array of local variables, with Java semantics.
 *
 * <p><b>Note:</b> For the most part, the documentation for this class
 * ignores the distinction between {@link Type} and {@link
 * TypeBearer}.</p>
 */
public abstract class LocalsArray extends MutabilityControl implements ToHuman {

    /**
     * Constructs an instance, explicitly indicating the mutability.
     *
     * @param mutable {@code true} if this instance is mutable
     */
    protected LocalsArray(boolean mutable) {
        super(mutable);
    }

    /**
     * Makes and returns a mutable copy of this instance.
     *
     * @return {@code non-null;} the copy
     */
    public abstract LocalsArray copy();

    /**
     * Annotates (adds context to) the given exception with information
     * about this instance.
     *
     * @param ex {@code non-null;} the exception to annotate
     */
    public abstract void annotate(ExceptionWithContext ex);

    /**
     * Replaces all the occurrences of the given uninitialized type in
     * this array with its initialized equivalent.
     *
     * @param type {@code non-null;} type to replace
     */
    public abstract void makeInitialized(Type type);

    /**
     * Gets the maximum number of locals this instance can refer to.
     *
     * @return the max locals
     */
    public abstract int getMaxLocals();

    /**
     * Sets the type stored at the given local index. If the given type
     * is category-2, then (a) the index must be at least two less than
     * {@link #getMaxLocals} and (b) the next index gets invalidated
     * by the operation. In case of either category, if the <i>previous</i>
     * local contains a category-2 value, then it too is invalidated by
     * this operation.
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     * @param type {@code non-null;} new type for the local at {@code idx}
     */
    public abstract void set(int idx, TypeBearer type);

    /**
     * Sets the type for the local indicated by the given register spec
     * to that register spec (which includes type and optional name
     * information). This is identical to calling
     * {@code set(spec.getReg(), spec)}.
     *
     * @param spec {@code non-null;} register spec to use as the basis for the update
     */
    public abstract void set(RegisterSpec spec);

    /**
     * Invalidates the local at the given index.
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     */
    public abstract void invalidate(int idx);

    /**
     * Gets the type stored at the given local index, or {@code null}
     * if the given local is uninitialized / invalid.
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     * @return {@code null-ok;} the type of value stored in that local
     */
    public abstract TypeBearer getOrNull(int idx);

    /**
     * Gets the type stored at the given local index, only succeeding if
     * the given local contains a valid type (though it is allowed to
     * be an uninitialized instance).
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     * @return {@code non-null;} the type of value stored in that local
     * @throws SimException thrown if {@code idx} is valid, but
     * the contents are invalid
     */
    public abstract TypeBearer get(int idx);

    /**
     * Gets the type stored at the given local index, which is expected
     * to be an initialized category-1 value.
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     * @return {@code non-null;} the type of value stored in that local
     * @throws SimException thrown if {@code idx} is valid, but
     * one of the following holds: (a) the local is invalid; (b) the local
     * contains an uninitialized instance; (c) the local contains a
     * category-2 value
     */
    public abstract TypeBearer getCategory1(int idx);

    /**
     * Gets the type stored at the given local index, which is expected
     * to be a category-2 value.
     *
     * @param idx {@code >= 0, < getMaxLocals();} which local
     * @return {@code non-null;} the type of value stored in that local
     * @throws SimException thrown if {@code idx} is valid, but
     * one of the following holds: (a) the local is invalid; (b) the local
     * contains a category-1 value
     */
    public abstract TypeBearer getCategory2(int idx);

    /**
     * Merges this instance with {@code other}. If the merged result is
     * the same as this instance, then this is returned (not a copy).
     *
     * @param other {@code non-null;} another LocalsArray
     * @return {@code non-null;} the merge result, a new instance or this
     */
    public abstract LocalsArray merge(LocalsArray other);

    /**
     * Merges this instance with a {@code LocalsSet} from a subroutine
     * caller. To be used when merging in the first block of a subroutine.
     *
     * @param other {@code other non-null;} another LocalsArray. The final locals
     * state of a subroutine caller.
     * @param predLabel the label of the subroutine caller block.
     * @return {@code non-null;} the merge result, a new instance or this
     */
    public abstract LocalsArraySet mergeWithSubroutineCaller
            (LocalsArray other, int predLabel);

    /**
     * Gets the locals set appropriate for the current execution context.
     * That is, if this is a {@code OneLocalsArray} instance, then return
     * {@code this}, otherwise return {@code LocalsArraySet}'s
     * primary.
     *
     * @return locals for this execution context.
     */
    protected abstract OneLocalsArray getPrimary();

}
