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

package external.com.android.dx.ssa;

import external.com.android.dx.util.BitIntSet;
import external.com.android.dx.util.IntSet;
import external.com.android.dx.util.ListIntSet;


/**
 * Makes int sets for various parts of the optimizer.
 */
public final class SetFactory {

    /**
     * BitIntSet/ListIntSet threshold for dominance frontier sets. These
     * sets are kept per basic block until phi placement and tend to be,
     * like the CFG itself, very sparse at large sizes.
     *
     * A value of 3072 here is somewhere around 1.125mb of total bitset size.
     */
    private static final int DOMFRONT_SET_THRESHOLD_SIZE = 3072;

    /**
     * BitIntSet/ListIntSet threshold for interference graph sets. These
     * sets are kept per register until register allocation is done.
     *
     * A value of 3072 here is somewhere around 1.125mb of total bitset size.
     */
    private static final int INTERFERENCE_SET_THRESHOLD_SIZE = 3072;

    /**
     * BitIntSet/ListIntSet threshold for the live in/out sets kept by
     * {@link SsaBasicBlock}. These are sets of SSA registers kept per basic
     * block during register allocation.
     *
     * The total size of a bitset for this would be the count of blocks
     * times the size of registers. The threshold value here is merely
     * the register count, which is typically on the order of the block
     * count as well.
     */
    private static final int LIVENESS_SET_THRESHOLD_SIZE = 3072;


    /**
     * Make IntSet for the dominance-frontier sets.
     *
     * @param szBlocks {@code >=0;} count of basic blocks in method
     * @return {@code non-null;} appropriate set
     */
    /*package*/ static IntSet makeDomFrontSet(int szBlocks) {
        return szBlocks <= DOMFRONT_SET_THRESHOLD_SIZE
                ? new BitIntSet(szBlocks)
                : new ListIntSet();
    }

    /**
     * Make IntSet for the interference graph sets. Public because
     * InterferenceGraph is in another package.
     *
     * @param countRegs {@code >=0;} count of SSA registers used in method
     * @return {@code non-null;} appropriate set
     */
    public static IntSet makeInterferenceSet(int countRegs) {
        return countRegs <= INTERFERENCE_SET_THRESHOLD_SIZE
                ? new BitIntSet(countRegs)
                : new ListIntSet();
    }

    /**
     * Make IntSet for register live in/out sets.
     *
     * @param countRegs {@code >=0;} count of SSA registers used in method
     * @return {@code non-null;} appropriate set
     */
    /*package*/ static IntSet makeLivenessSet(int countRegs) {
        return countRegs <= LIVENESS_SET_THRESHOLD_SIZE
                ? new BitIntSet(countRegs)
                : new ListIntSet();
    }
}
