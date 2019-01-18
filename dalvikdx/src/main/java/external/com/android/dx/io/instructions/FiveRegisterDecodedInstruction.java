/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx.io.instructions;

import external.com.android.dx.io.IndexType;

/**
 * A decoded Dalvik instruction which has five register arguments.
 */
public final class FiveRegisterDecodedInstruction extends DecodedInstruction {
    /** register argument "A" */
    private final int a;

    /** register argument "B" */
    private final int b;

    /** register argument "C" */
    private final int c;

    /** register argument "D" */
    private final int d;

    /** register argument "E" */
    private final int e;

    /**
     * Constructs an instance.
     */
    public FiveRegisterDecodedInstruction(InstructionCodec format, int opcode,
            int index, IndexType indexType, int target, long literal,
            int a, int b, int c, int d, int e) {
        super(format, opcode, index, indexType, target, literal);

        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    /** {@inheritDoc} */
    @Override
    public int getRegisterCount() {
        return 5;
    }

    /** {@inheritDoc} */
    @Override
    public int getA() {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public int getB() {
        return b;
    }

    /** {@inheritDoc} */
    @Override
    public int getC() {
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public int getD() {
        return d;
    }

    /** {@inheritDoc} */
    @Override
    public int getE() {
        return e;
    }

    /** {@inheritDoc} */
    @Override
    public DecodedInstruction withIndex(int newIndex) {
        return new FiveRegisterDecodedInstruction(
                getFormat(), getOpcode(), newIndex, getIndexType(),
                getTarget(), getLiteral(), a, b, c, d, e);
    }
}
