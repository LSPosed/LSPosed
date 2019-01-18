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
import external.com.android.dx.util.AnnotatedOutput;

/**
 * Base class for instructions which are of a fixed code size and
 * which use {@link InsnFormat} methods to write themselves. This
 * includes most &mdash; but not all &mdash; instructions.
 */
public abstract class FixedSizeInsn extends DalvInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * <p><b>Note:</b> In the unlikely event that an instruction takes
     * absolutely no registers (e.g., a {@code nop} or a
     * no-argument no-result * static method call), then the given
     * register list may be passed as {@link
     * RegisterSpecList#EMPTY}.</p>
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins or outs)
     */
    public FixedSizeInsn(Dop opcode, SourcePosition position,
                         RegisterSpecList registers) {
        super(opcode, position, registers);
    }

    /** {@inheritDoc} */
    @Override
    public final int codeSize() {
        return getOpcode().getFormat().codeSize();
    }

    /** {@inheritDoc} */
    @Override
    public final void writeTo(AnnotatedOutput out) {
        getOpcode().getFormat().writeTo(out, this);
    }

    /** {@inheritDoc} */
    @Override
    public final DalvInsn withRegisterOffset(int delta) {
        return withRegisters(getRegisters().withOffset(delta));
    }

    /** {@inheritDoc} */
    @Override
    protected final String listingString0(boolean noteIndices) {
        return getOpcode().getFormat().listingString(this, noteIndices);
    }
}
