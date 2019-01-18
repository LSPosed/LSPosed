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
 * Pseudo-instruction base class for variable-sized instructions.
 */
public abstract class VariableSizeInsn extends DalvInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} source registers
     */
    public VariableSizeInsn(SourcePosition position,
                            RegisterSpecList registers) {
        super(Dops.SPECIAL_FORMAT, position, registers);
    }

    /** {@inheritDoc} */
    @Override
    public final DalvInsn withOpcode(Dop opcode) {
        throw new RuntimeException("unsupported");
    }

    /** {@inheritDoc} */
    @Override
    public final DalvInsn withRegisterOffset(int delta) {
        return withRegisters(getRegisters().withOffset(delta));
    }
}
