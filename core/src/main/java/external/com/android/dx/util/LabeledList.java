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

package external.com.android.dx.util;

import java.util.Arrays;

/**
 * A list of labeled items, allowing easy lookup by label.
 */
public class LabeledList extends FixedSizeList {
    /**
     * Sparse array indexed by label to FixedSizeList index;
     * {@code -1} for an invalid label.
     */
    private final IntList labelToIndex;

    /** {@inheritDoc} */
    public LabeledList(int size) {
        super(size);

        labelToIndex = new IntList(size);
    }

    /**
     * Constructs a new instance that is a copy of the old instance.
     *
     * @param old instance to copy
     */
    public LabeledList(LabeledList old) {
        super(old.size());
        labelToIndex = old.labelToIndex.mutableCopy();

        int sz = old.size();

        for (int i = 0; i < sz; i++) {
            Object one = old.get0(i);
            if (one != null) {
                set0(i, one);
            }
        }
    }

    /**
     * Gets the maximum label (exclusive) of any block added to this instance.
     *
     * @return {@code >= 0;} the maximum label
     */
    public final int getMaxLabel() {
        int sz = labelToIndex.size();

        // Gobble any deleted labels that may be at the end.
        int i;
        for (i = sz - 1; (i >= 0) && (labelToIndex.get(i) < 0); i--)
            /*empty*/ ;

        int newSize = i + 1;

        labelToIndex.shrink(newSize);

        return newSize;
    }

    /**
     * Removes a label from the label-to-index mapping.
     *
     * @param oldLabel label to remove
     */
    private void removeLabel(int oldLabel) {
        labelToIndex.set(oldLabel, -1);
    }

    /**
     * Adds a label and index to the label-to-index mapping.
     *
     * @param label new label
     * @param index index of block.
     */
    private void addLabelIndex(int label, int index) {
        int origSz = labelToIndex.size();

        for (int i = 0; i <= (label - origSz); i++) {
            labelToIndex.add(-1);
        }

        labelToIndex.set(label, index);
    }

    /**
     * Gets the index of the first item in the list with the given
     * label, if any.
     *
     * @param label {@code >= 0;} the label to look for
     * @return {@code >= -1;} the index of the so-labelled item, or {@code -1}
     * if none is found
     */
    public final int indexOfLabel(int label) {
        if (label >= labelToIndex.size()) {
            return -1;
        } else {
            return labelToIndex.get(label);
        }
    }

    /**
     * Gets an array containing all of the labels used in this instance,
     * in order. The returned array is freshly-allocated and so may be
     * modified safely by the caller without impacting this instance.
     *
     * @return {@code non-null;} ordered array of labels
     * @throws NullPointerException thrown if there are any {@code null}
     * items in this instance
     */
    public final int[] getLabelsInOrder() {
        int sz = size();
        int[] result = new int[sz];

        for (int i = 0; i < sz; i++) {
            LabeledItem li = (LabeledItem) get0(i);
            if (li == null) {
                throw new NullPointerException("null at index " + i);
            }
            result[i] = li.getLabel();
        }

        Arrays.sort(result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void shrinkToFit() {
        super.shrinkToFit();

        rebuildLabelToIndex();
    }

    /**
     * Rebuilds the label-to-index mapping after a {@code shrinkToFit()}.
     * Note: This assumes that the labels that are in the list are the
     * same, although the indicies may have changed.
     */
    private void rebuildLabelToIndex() {
        int szItems = size();

        for (int i = 0; i < szItems; i++) {
            LabeledItem li = (LabeledItem) get0(i);

            if (li != null) {
                labelToIndex.set(li.getLabel(), i);
            }
        }
    }

    /**
     * Sets the element at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param item {@code null-ok;} the value to store
     */
    protected void set(int n, LabeledItem item) {
        LabeledItem old = (LabeledItem) getOrNull0(n);

        set0(n, item);

        if (old != null) {
            removeLabel(old.getLabel());
        }

        if (item != null) {
            addLabelIndex(item.getLabel(), n);
        }
    }
}
