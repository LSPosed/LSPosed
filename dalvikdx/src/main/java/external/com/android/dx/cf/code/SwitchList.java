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

import external.com.android.dx.util.IntList;
import external.com.android.dx.util.MutabilityControl;

/**
 * List of (value, target) mappings representing the choices of a
 * {@code tableswitch} or {@code lookupswitch} instruction. It
 * also holds the default target for the switch.
 */
public final class SwitchList extends MutabilityControl {
    /** {@code non-null;} list of test values */
    private final IntList values;

    /**
     * {@code non-null;} list of targets corresponding to the test values; there
     * is always one extra element in the target list, to hold the
     * default target
     */
    private final IntList targets;

    /** ultimate size of the list */
    private int size;

    /**
     * Constructs an instance.
     *
     * @param size {@code >= 0;} the number of elements to be in the table
     */
    public SwitchList(int size) {
        super(true);
        this.values = new IntList(size);
        this.targets = new IntList(size + 1);
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override
    public void setImmutable() {
        values.setImmutable();
        targets.setImmutable();
        super.setImmutable();
    }

    /**
     * Gets the size of the list.
     *
     * @return {@code >= 0;} the list size
     */
    public int size() {
        return size;
    }

    /**
     * Gets the indicated test value.
     *
     * @param n {@code >= 0;}, &lt; size(); which index
     * @return the test value
     */
    public int getValue(int n) {
        return values.get(n);
    }

    /**
     * Gets the indicated target. Asking for the target at {@code size()}
     * returns the default target.
     *
     * @param n {@code >= 0, <= size();} which index
     * @return {@code >= 0;} the target
     */
    public int getTarget(int n) {
        return targets.get(n);
    }

    /**
     * Gets the default target. This is just a shorthand for
     * {@code getTarget(size())}.
     *
     * @return {@code >= 0;} the default target
     */
    public int getDefaultTarget() {
        return targets.get(size);
    }

    /**
     * Gets the list of all targets. This includes one extra element at the
     * end of the list, which holds the default target.
     *
     * @return {@code non-null;} the target list
     */
    public IntList getTargets() {
        return targets;
    }

    /**
     * Gets the list of all case values.
     *
     * @return {@code non-null;} the case value list
     */
    public IntList getValues() {
        return values;
    }

    /**
     * Sets the default target. It is only valid to call this method
     * when all the non-default elements have been set.
     *
     * @param target {@code >= 0;} the absolute (not relative) default target
     * address
     */
    public void setDefaultTarget(int target) {
        throwIfImmutable();

        if (target < 0) {
            throw new IllegalArgumentException("target < 0");
        }

        if (targets.size() != size) {
            throw new RuntimeException("non-default elements not all set");
        }

        targets.add(target);
    }

    /**
     * Adds the given item.
     *
     * @param value the test value
     * @param target {@code >= 0;} the absolute (not relative) target address
     */
    public void add(int value, int target) {
        throwIfImmutable();

        if (target < 0) {
            throw new IllegalArgumentException("target < 0");
        }

        values.add(value);
        targets.add(target);
    }

    /**
     * Shrinks this instance if possible, removing test elements that
     * refer to the default target. This is only valid after the instance
     * is fully populated, including the default target (naturally).
     */
    public void removeSuperfluousDefaults() {
        throwIfImmutable();

        int sz = size;

        if (sz != (targets.size() - 1)) {
            throw new IllegalArgumentException("incomplete instance");
        }

        int defaultTarget = targets.get(sz);
        int at = 0;

        for (int i = 0; i < sz; i++) {
            int target = targets.get(i);
            if (target != defaultTarget) {
                if (i != at) {
                    targets.set(at, target);
                    values.set(at, values.get(i));
                }
                at++;
            }
        }

        if (at != sz) {
            values.shrink(at);
            targets.set(at, defaultTarget);
            targets.shrink(at + 1);
            size = at;
        }
    }
}
