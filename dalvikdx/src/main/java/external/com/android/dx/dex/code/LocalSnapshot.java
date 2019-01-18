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

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.RegisterSpecSet;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.ssa.RegisterMapper;

/**
 * Pseudo-instruction which is used to hold a snapshot of the
 * state of local variable name mappings that exists immediately after
 * the instance in an instruction array.
 */
public final class LocalSnapshot extends ZeroSizeInsn {
    /** {@code non-null;} local state associated with this instance */
    private final RegisterSpecSet locals;

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     * @param locals {@code non-null;} associated local variable state
     */
    public LocalSnapshot(SourcePosition position, RegisterSpecSet locals) {
        super(position);

        if (locals == null) {
            throw new NullPointerException("locals == null");
        }

        this.locals = locals;
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisterOffset(int delta) {
        return new LocalSnapshot(getPosition(), locals.withOffset(delta));
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new LocalSnapshot(getPosition(), locals);
    }

    /**
     * Gets the local state associated with this instance.
     *
     * @return {@code non-null;} the state
     */
    public RegisterSpecSet getLocals() {
        return locals;
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return locals.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        int sz = locals.size();
        int max = locals.getMaxSize();
        StringBuilder sb = new StringBuilder(100 + sz * 40);

        sb.append("local-snapshot");

        for (int i = 0; i < max; i++) {
            RegisterSpec spec = locals.get(i);
            if (spec != null) {
                sb.append("\n  ");
                sb.append(LocalStart.localString(spec));
            }
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withMapper(RegisterMapper mapper) {
      return new LocalSnapshot(getPosition(), mapper.map(locals));
    }
}
