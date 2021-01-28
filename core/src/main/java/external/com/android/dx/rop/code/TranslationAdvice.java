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

package external.com.android.dx.rop.code;

/**
 * Interface for "advice" passed from the late stage of translation back
 * to the early stage. This allows for the final target architecture to
 * exert its influence early in the translation process without having
 * the early stage code be explicitly tied to the target.
 */
public interface TranslationAdvice {
    /**
     * Returns an indication of whether the target can directly represent an
     * instruction with the given opcode operating on the given arguments,
     * where the last source argument is used as a constant. (That is, the
     * last argument must have a type which indicates it is a known constant.)
     * The instruction associated must have exactly two sources.
     *
     * @param opcode {@code non-null;} the opcode
     * @param sourceA {@code non-null;} the first source
     * @param sourceB {@code non-null;} the second source
     * @return {@code true} iff the target can represent the operation
     * using a constant for the last argument
     */
    public boolean hasConstantOperation(Rop opcode,
            RegisterSpec sourceA, RegisterSpec sourceB);

    /**
     * Returns true if the translation target requires the sources of the
     * specified opcode to be in order and contiguous (eg, for an invoke-range)
     *
     * @param opcode {@code non-null;} opcode
     * @param sources {@code non-null;} source list
     * @return {@code true} iff the target requires the sources to be
     * in order and contiguous.
     */
    public boolean requiresSourcesInOrder(Rop opcode, RegisterSpecList sources);

    /**
     * Gets the maximum register width that can be represented optimally.
     * For example, Dex bytecode does not have instruction forms that take
     * register numbers larger than 15 for all instructions so
     * DexTranslationAdvice returns 15 here.
     *
     * @return register count noted above
     */
    public int getMaxOptimalRegisterCount();
}
