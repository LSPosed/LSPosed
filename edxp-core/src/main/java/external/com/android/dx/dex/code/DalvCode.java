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

package external.com.android.dx.dex.code;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.type.Type;
import java.util.HashSet;

/**
 * Container for all the pieces of a concrete method. Each instance
 * corresponds to a {@code code} structure in a {@code .dex} file.
 */
public final class DalvCode {
    /**
     * how much position info to preserve; one of the static
     * constants in {@link PositionList}
     */
    private final int positionInfo;

    /**
     * {@code null-ok;} the instruction list, ready for final processing;
     * nulled out in {@link #finishProcessingIfNecessary}
     */
    private OutputFinisher unprocessedInsns;

    /**
     * {@code non-null;} unprocessed catch table;
     * nulled out in {@link #finishProcessingIfNecessary}
     */
    private CatchBuilder unprocessedCatches;

    /**
     * {@code null-ok;} catch table; set in
     * {@link #finishProcessingIfNecessary}
     */
    private CatchTable catches;

    /**
     * {@code null-ok;} source positions list; set in
     * {@link #finishProcessingIfNecessary}
     */
    private PositionList positions;

    /**
     * {@code null-ok;} local variable list; set in
     * {@link #finishProcessingIfNecessary}
     */
    private LocalList locals;

    /**
     * {@code null-ok;} the processed instruction list; set in
     * {@link #finishProcessingIfNecessary}
     */
    private DalvInsnList insns;

    /**
     * Constructs an instance.
     *
     * @param positionInfo how much position info to preserve; one of the
     * static constants in {@link PositionList}
     * @param unprocessedInsns {@code non-null;} the instruction list, ready
     * for final processing
     * @param unprocessedCatches {@code non-null;} unprocessed catch
     * (exception handler) table
     */
    public DalvCode(int positionInfo, OutputFinisher unprocessedInsns,
            CatchBuilder unprocessedCatches) {
        if (unprocessedInsns == null) {
            throw new NullPointerException("unprocessedInsns == null");
        }

        if (unprocessedCatches == null) {
            throw new NullPointerException("unprocessedCatches == null");
        }

        this.positionInfo = positionInfo;
        this.unprocessedInsns = unprocessedInsns;
        this.unprocessedCatches = unprocessedCatches;
        this.catches = null;
        this.positions = null;
        this.locals = null;
        this.insns = null;
    }

    /**
     * Finish up processing of the method.
     */
    private void finishProcessingIfNecessary() {
        if (insns != null) {
            return;
        }

        insns = unprocessedInsns.finishProcessingAndGetList();
        positions = PositionList.make(insns, positionInfo);
        locals = LocalList.make(insns);
        catches = unprocessedCatches.build();

        // Let them be gc'ed.
        unprocessedInsns = null;
        unprocessedCatches = null;
    }

    /**
     * Assign indices in all instructions that need them, using the
     * given callback to perform lookups. This must be called before
     * {@link #getInsns}.
     *
     * @param callback {@code non-null;} callback object
     */
    public void assignIndices(AssignIndicesCallback callback) {
        unprocessedInsns.assignIndices(callback);
    }

    /**
     * Gets whether this instance has any position data to represent.
     *
     * @return {@code true} iff this instance has any position
     * data to represent
     */
    public boolean hasPositions() {
        return (positionInfo != PositionList.NONE)
            && unprocessedInsns.hasAnyPositionInfo();
    }

    /**
     * Gets whether this instance has any local variable data to represent.
     *
     * @return {@code true} iff this instance has any local variable
     * data to represent
     */
    public boolean hasLocals() {
        return unprocessedInsns.hasAnyLocalInfo();
    }

    /**
     * Gets whether this instance has any catches at all (either typed
     * or catch-all).
     *
     * @return whether this instance has any catches at all
     */
    public boolean hasAnyCatches() {
        return unprocessedCatches.hasAnyCatches();
    }

    /**
     * Gets the set of catch types handled anywhere in the code.
     *
     * @return {@code non-null;} the set of catch types
     */
    public HashSet<Type> getCatchTypes() {
        return unprocessedCatches.getCatchTypes();
    }

    /**
     * Gets the set of all constants referred to by instructions in
     * the code.
     *
     * @return {@code non-null;} the set of constants
     */
    public HashSet<Constant> getInsnConstants() {
        return unprocessedInsns.getAllConstants();
    }

    /**
     * Gets the list of instructions.
     *
     * @return {@code non-null;} the instruction list
     */
    public DalvInsnList getInsns() {
        finishProcessingIfNecessary();
        return insns;
    }

    /**
     * Gets the catch (exception handler) table.
     *
     * @return {@code non-null;} the catch table
     */
    public CatchTable getCatches() {
        finishProcessingIfNecessary();
        return catches;
    }

    /**
     * Gets the source positions list.
     *
     * @return {@code non-null;} the source positions list
     */
    public PositionList getPositions() {
        finishProcessingIfNecessary();
        return positions;
    }

    /**
     * Gets the source positions list.
     *
     * @return {@code non-null;} the source positions list
     */
    public LocalList getLocals() {
        finishProcessingIfNecessary();
        return locals;
    }

    /**
     * Class used as a callback for {@link #assignIndices}.
     */
    public static interface AssignIndicesCallback {
        /**
         * Gets the index for the given constant.
         *
         * @param cst {@code non-null;} the constant
         * @return {@code >= -1;} the index or {@code -1} if the constant
         * shouldn't actually be reified with an index
         */
        public int getIndex(Constant cst);
    }
}
