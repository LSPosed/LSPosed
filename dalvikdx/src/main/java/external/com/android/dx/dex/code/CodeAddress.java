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
 * Pseudo-instruction which is used to track an address within a code
 * array. Instances are used for such things as branch targets and
 * exception handler ranges. Its code size is zero, and so instances
 * do not in general directly wind up in any output (either
 * human-oriented or binary file).
 */
public final class CodeAddress extends ZeroSizeInsn {
    /** If this address should bind closely to the following real instruction */
    private final boolean bindsClosely;

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     */
    public CodeAddress(SourcePosition position) {
        this(position, false);
    }

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     * @param bindsClosely if the address should bind closely to the following
     *                     real instruction.
     */
    public CodeAddress(SourcePosition position, boolean bindsClosely) {
        super(position);
        this.bindsClosely = bindsClosely;
    }

    /** {@inheritDoc} */
    @Override
    public final DalvInsn withRegisters(RegisterSpecList registers) {
        return new CodeAddress(getPosition());
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        return "code-address";
    }

    /**
     * Gets whether this address binds closely to the following "real"
     * (non-zero-length) instruction.
     *
     * When a prefix is added to an instruction (for example, to move a value
     * from a high register to a low register), this determines whether this
     * {@code CodeAddress} will point to the prefix, or to the instruction
     * itself.
     *
     * If bindsClosely is true, the address will point to the instruction
     * itself, otherwise it will point to the prefix (if any)
     *
     * @return true if this address binds closely to the next real instruction
     */
    public boolean getBindsClosely() {
        return bindsClosely;
    }
}
