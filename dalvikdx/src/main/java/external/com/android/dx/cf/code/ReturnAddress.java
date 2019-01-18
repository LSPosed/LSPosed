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

package external.com.android.dx.cf.code;

import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.Hex;

/**
 * Representation of a subroutine return address. In Java verification,
 * somewhat counterintuitively, the salient bit of information you need to
 * know about a return address is the <i>start address</i> of the subroutine
 * being returned from, not the address being returned <i>to</i>, so that's
 * what instances of this class hang onto.
 */
public final class ReturnAddress implements TypeBearer {
    /** {@code >= 0;} the start address of the subroutine being returned from */
    private final int subroutineAddress;

    /**
     * Constructs an instance.
     *
     * @param subroutineAddress {@code >= 0;} the start address of the
     * subroutine being returned from
     */
    public ReturnAddress(int subroutineAddress) {
        if (subroutineAddress < 0) {
            throw new IllegalArgumentException("subroutineAddress < 0");
        }

        this.subroutineAddress = subroutineAddress;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ("<addr:" + Hex.u2(subroutineAddress) + ">");
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return toString();
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.RETURN_ADDRESS;
    }

    /** {@inheritDoc} */
    @Override
    public TypeBearer getFrameType() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public int getBasicType() {
        return Type.RETURN_ADDRESS.getBasicType();
    }

    /** {@inheritDoc} */
    @Override
    public int getBasicFrameType() {
        return Type.RETURN_ADDRESS.getBasicFrameType();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConstant() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReturnAddress)) {
            return false;
        }

        return subroutineAddress == ((ReturnAddress) other).subroutineAddress;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return subroutineAddress;
    }

    /**
     * Gets the subroutine address.
     *
     * @return {@code >= 0;} the subroutine address
     */
    public int getSubroutineAddress() {
        return subroutineAddress;
    }
}
