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

package external.com.android.dx.ssa;

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.ssa.back.InterferenceGraph;
import external.com.android.dx.util.BitIntSet;
import external.com.android.dx.util.IntSet;
import java.util.ArrayList;

/**
 * A register mapper that keeps track of the accumulated interference
 * information for the registers in the new namespace.
 *
 * Please note that this mapper requires that the old namespace does not
 * have variable register widths/categories, and the new namespace does.
 */
public class InterferenceRegisterMapper extends BasicRegisterMapper {
    /**
     * Array of interference sets. ArrayList is indexed by new namespace
     * and BitIntSet's are indexed by old namespace.  The list expands
     * as needed and missing items are assumed to interfere with nothing.
     *
     * Bit sets are always used here, unlike elsewhere, because the max
     * size of this matrix will be (countSsaRegs * countRopRegs), which may
     * grow to hundreds of K but not megabytes.
     */
    private final ArrayList<BitIntSet> newRegInterference;

    /** the interference graph for the old namespace */
    private final InterferenceGraph oldRegInterference;

    /**
     * Constructs an instance
     *
     * @param countOldRegisters number of registers in old namespace
     */
    public InterferenceRegisterMapper(InterferenceGraph oldRegInterference,
            int countOldRegisters) {
        super(countOldRegisters);

        newRegInterference = new ArrayList<BitIntSet>();
        this.oldRegInterference = oldRegInterference;
    }

    /** {@inheritDoc} */
    @Override
    public void addMapping(int oldReg, int newReg, int category) {
        super.addMapping(oldReg, newReg, category);

        addInterfence(newReg, oldReg);

        if (category == 2) {
            addInterfence(newReg + 1, oldReg);
        }
    }

    /**
     * Checks to see if old namespace reg {@code oldReg} interferes
     * with what currently maps to {@code newReg}.
     *
     * @param oldReg old namespace register
     * @param newReg new namespace register
     * @param category category of old namespace register
     * @return true if oldReg will interfere with newReg
     */
    public boolean interferes(int oldReg, int newReg, int category) {
        if (newReg >= newRegInterference.size()) {
            return false;
        } else {
            IntSet existing = newRegInterference.get(newReg);

            if (existing == null) {
                return false;
            } else if (category == 1) {
                return existing.has(oldReg);
            } else {
                return existing.has(oldReg)
                        || (interferes(oldReg, newReg+1, category-1));
            }
        }
    }

    /**
     * Checks to see if old namespace reg {@code oldReg} interferes
     * with what currently maps to {@code newReg}.
     *
     * @param oldSpec {@code non-null;} old namespace register
     * @param newReg new namespace register
     * @return true if oldReg will interfere with newReg
     */
    public boolean interferes(RegisterSpec oldSpec, int newReg) {
        return interferes(oldSpec.getReg(), newReg, oldSpec.getCategory());
    }

    /**
     * Adds a register's interference set to the interference list,
     * growing it if necessary.
     *
     * @param newReg register in new namespace
     * @param oldReg register in old namespace
     */
    private void addInterfence(int newReg, int oldReg) {
        newRegInterference.ensureCapacity(newReg + 1);

        while (newReg >= newRegInterference.size()) {
            newRegInterference.add(new BitIntSet(newReg +1));
        }

        oldRegInterference.mergeInterferenceSet(
                oldReg, newRegInterference.get(newReg));
    }

    /**
     * Checks to see if any of a set of old-namespace registers are
     * pinned to the specified new-namespace reg + category. Takes into
     * account the category of the old-namespace registers.
     *
     * @param oldSpecs {@code non-null;} set of old-namespace regs
     * @param newReg {@code >= 0;} new-namespace register
     * @param targetCategory {@code 1..2;} the number of adjacent new-namespace
     * registers (starting at ropReg) to consider
     * @return true if any of the old-namespace register have been mapped
     * to the new-namespace register + category
     */
    public boolean areAnyPinned(RegisterSpecList oldSpecs,
            int newReg, int targetCategory) {
        int sz = oldSpecs.size();

        for (int i = 0; i < sz; i++) {
            RegisterSpec oldSpec = oldSpecs.get(i);
            int r = oldToNew(oldSpec.getReg());

            /*
             * If oldSpec is a category-2 register, then check both newReg
             * and newReg - 1.
             */
            if (r == newReg
                || (oldSpec.getCategory() == 2 && (r + 1) == newReg)
                || (targetCategory == 2 && (r == newReg + 1))) {
                return true;
            }
        }

        return false;
    }
}
