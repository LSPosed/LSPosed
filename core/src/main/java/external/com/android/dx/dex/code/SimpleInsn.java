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

import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;

/**
 * Instruction which has no extra info beyond the basics provided for in
 * the base class.
 */
public final class SimpleInsn extends FixedSizeInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins or outs)
     */
    public SimpleInsn(Dop opcode, SourcePosition position,
                      RegisterSpecList registers) {
        super(opcode, position, registers);
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withOpcode(Dop opcode) {
        return new SimpleInsn(opcode, getPosition(), getRegisters());
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new SimpleInsn(getOpcode(), getPosition(), registers);
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return null;
    }
}
