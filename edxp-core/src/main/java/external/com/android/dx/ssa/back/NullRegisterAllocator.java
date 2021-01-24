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

package external.com.android.dx.ssa.back;

import external.com.android.dx.ssa.BasicRegisterMapper;
import external.com.android.dx.ssa.RegisterMapper;
import external.com.android.dx.ssa.SsaMethod;

/**
 * A register allocator that maps SSA register n to Rop register 2*n,
 * essentially preserving the original mapping and remaining agnostic
 * about normal or wide categories. Used for debugging.
 */
public class NullRegisterAllocator extends RegisterAllocator {
    /** {@inheritDoc} */
    public NullRegisterAllocator(SsaMethod ssaMeth,
            InterferenceGraph interference) {
        super(ssaMeth, interference);
    }

    /** {@inheritDoc} */
    @Override
    public boolean wantsParamsMovedHigh() {
        // We're not smart enough for this.
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public RegisterMapper allocateRegisters() {
        int oldRegCount = ssaMeth.getRegCount();

        BasicRegisterMapper mapper = new BasicRegisterMapper(oldRegCount);

        for (int i = 0; i < oldRegCount; i++) {
            mapper.addMapping(i, i*2, 2);
        }

        return mapper;
    }
}
