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

import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.util.AnnotatedOutput;

/**
 * Pseudo-instruction which either turns into a {@code nop} or
 * nothingness, in order to make the subsequent instruction have an
 * even address. This is used to align (subsequent) instructions that
 * require it.
 */
public final class OddSpacer extends VariableSizeInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     */
    public OddSpacer(SourcePosition position) {
        super(position, RegisterSpecList.EMPTY);
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return (getAddress() & 1);
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out) {
        if (codeSize() != 0) {
            out.writeShort(InsnFormat.codeUnit(Opcodes.NOP, 0));
        }
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new OddSpacer(getPosition());
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        if (codeSize() == 0) {
            return null;
        }

        return "nop // spacer";
    }
}
