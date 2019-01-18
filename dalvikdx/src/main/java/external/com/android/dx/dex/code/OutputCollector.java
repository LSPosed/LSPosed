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

import external.com.android.dx.dex.DexOptions;
import java.util.ArrayList;

/**
 * Destination for {@link DalvInsn} instances being output. This class
 * receives and collects instructions in two pieces &mdash; a primary
 * list and a suffix (generally consisting of adjunct data referred to
 * by the primary list, such as switch case tables) &mdash; which it
 * merges and emits back out in the form of a {@link DalvInsnList}
 * instance.
 */
public final class OutputCollector {
    /**
     * {@code non-null;} the associated finisher (which holds the instruction
     * list in-progress)
     */
    private final OutputFinisher finisher;

    /**
     * {@code null-ok;} suffix for the output, or {@code null} if the suffix
     * has been appended to the main output (by {@link #appendSuffixToOutput})
     */
    private ArrayList<DalvInsn> suffix;

    /**
     * Constructs an instance.
     *
     * @param dexOptions {@code non-null;} options for dex output
     * @param initialCapacity {@code >= 0;} initial capacity of the output list
     * @param suffixInitialCapacity {@code >= 0;} initial capacity of the output
     * suffix
     * @param regCount {@code >= 0;} register count for the method
     * @param paramSize size, in register units, of all the parameters for this method
     */
    public OutputCollector(DexOptions dexOptions, int initialCapacity, int suffixInitialCapacity,
            int regCount, int paramSize) {
        this.finisher = new OutputFinisher(dexOptions, initialCapacity, regCount, paramSize);
        this.suffix = new ArrayList<DalvInsn>(suffixInitialCapacity);
    }

    /**
     * Adds an instruction to the output.
     *
     * @param insn {@code non-null;} the instruction to add
     */
    public void add(DalvInsn insn) {
        finisher.add(insn);
    }

    /**
     * Reverses a branch which is buried a given number of instructions
     * backward in the output. It is illegal to call this unless the
     * indicated instruction really is a reversible branch.
     *
     * @param which how many instructions back to find the branch;
     * {@code 0} is the most recently added instruction,
     * {@code 1} is the instruction before that, etc.
     * @param newTarget {@code non-null;} the new target for the reversed branch
     */
    public void reverseBranch(int which, CodeAddress newTarget) {
        finisher.reverseBranch(which, newTarget);
    }

    /**
     * Adds an instruction to the output suffix.
     *
     * @param insn {@code non-null;} the instruction to add
     */
    public void addSuffix(DalvInsn insn) {
        suffix.add(insn);
    }

    /**
     * Gets the results of all the calls on this instance, in the form of
     * an {@link OutputFinisher}.
     *
     * @return {@code non-null;} the output finisher
     * @throws UnsupportedOperationException if this method has
     * already been called
     */
    public OutputFinisher getFinisher() {
        if (suffix == null) {
            throw new UnsupportedOperationException("already processed");
        }

        appendSuffixToOutput();
        return finisher;
    }

    /**
     * Helper for {@link #getFinisher}, which appends the suffix to
     * the primary output.
     */
    private void appendSuffixToOutput() {
        int size = suffix.size();

        for (int i = 0; i < size; i++) {
            finisher.add(suffix.get(i));
        }

        suffix = null;
    }
}
