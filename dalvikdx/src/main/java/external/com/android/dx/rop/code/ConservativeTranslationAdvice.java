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
 * Implementation of {@link TranslationAdvice} which conservatively answers
 * {@code false} to all methods.
 */
public final class ConservativeTranslationAdvice
        implements TranslationAdvice {
    /** {@code non-null;} standard instance of this class */
    public static final ConservativeTranslationAdvice THE_ONE =
        new ConservativeTranslationAdvice();

    /**
     * This class is not publicly instantiable. Use {@link #THE_ONE}.
     */
    private ConservativeTranslationAdvice() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasConstantOperation(Rop opcode,
            RegisterSpec sourceA, RegisterSpec sourceB) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean requiresSourcesInOrder(Rop opcode,
            RegisterSpecList sources) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOptimalRegisterCount() {
        return Integer.MAX_VALUE;
    }
}
