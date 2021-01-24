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

import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import external.com.android.dx.util.LabeledItem;

/**
 * Representation of a basic block in a bytecode array.
 */
public final class ByteBlock implements LabeledItem {
    /** {@code >= 0;} label for this block */
    private final int label;

    /** {@code >= 0;} bytecode offset (inclusive) of the start of the block */
    private final int start;

    /** {@code > start;} bytecode offset (exclusive) of the end of the block */
    private final int end;

    /** {@code non-null;} list of successors that this block may branch to */
    private final IntList successors;

    /** {@code non-null;} list of exceptions caught and their handler targets */
    private final ByteCatchList catches;

    /**
     * Constructs an instance.
     *
     * @param label {@code >= 0;} target label for this block
     * @param start {@code >= 0;} bytecode offset (inclusive) of the start
     * of the block
     * @param end {@code > start;} bytecode offset (exclusive) of the end
     * of the block
     * @param successors {@code non-null;} list of successors that this block may
     * branch to
     * @param catches {@code non-null;} list of exceptions caught and their
     * handler targets
     */
    public ByteBlock(int label, int start, int end, IntList successors,
                     ByteCatchList catches) {
        if (label < 0) {
            throw new IllegalArgumentException("label < 0");
        }

        if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        }

        if (end <= start) {
            throw new IllegalArgumentException("end <= start");
        }

        if (successors == null) {
            throw new NullPointerException("targets == null");
        }

        int sz = successors.size();
        for (int i = 0; i < sz; i++) {
            if (successors.get(i) < 0) {
                throw new IllegalArgumentException("successors[" + i +
                                                   "] == " +
                                                   successors.get(i));
            }
        }

        if (catches == null) {
            throw new NullPointerException("catches == null");
        }

        this.label = label;
        this.start = start;
        this.end = end;
        this.successors = successors;
        this.catches = catches;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return '{' + Hex.u2(label) + ": " + Hex.u2(start) + ".." +
            Hex.u2(end) + '}';
    }

    /**
     * Gets the label of this block.
     *
     * @return {@code >= 0;} the label
     */
    @Override
    public int getLabel() {
        return label;
    }

    /**
     * Gets the bytecode offset (inclusive) of the start of this block.
     *
     * @return {@code >= 0;} the start offset
     */
    public int getStart() {
        return start;
    }

    /**
     * Gets the bytecode offset (exclusive) of the end of this block.
     *
     * @return {@code > getStart();} the end offset
     */
    public int getEnd() {
        return end;
    }

    /**
     * Gets the list of successors that this block may branch to
     * non-exceptionally.
     *
     * @return {@code non-null;} the successor list
     */
    public IntList getSuccessors() {
        return successors;
    }

    /**
     * Gets the list of exceptions caught and their handler targets.
     *
     * @return {@code non-null;} the catch list
     */
    public ByteCatchList getCatches() {
        return catches;
    }
}
