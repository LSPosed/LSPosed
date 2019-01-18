/*
 * Copyright (C) 2017 The Android Open Source Project
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

package external.com.android.dx.rop.cst;

import external.com.android.dx.cf.code.BootstrapMethodArgumentsList;
import external.com.android.dx.rop.type.Prototype;

/**
 * Constant type to represent a call site.
 */
public final class CstCallSite extends CstArray {
    /**
     * Creates an instance of a {@code CstCallSite}.
     *
     * @param bootstrapHandle {@code non-null;} the bootstrap method handle to invoke
     * @param nat {@code non-null;} the name and type to be resolved by the bootstrap method handle
     * @param optionalArguments {@code null-ok;} optional arguments to provide to the bootstrap
     *     method
     * @return a new {@code CstCallSite} instance
     */
    public static CstCallSite make(CstMethodHandle bootstrapHandle, CstNat nat,
                                   BootstrapMethodArgumentsList optionalArguments) {
        if (bootstrapHandle == null) {
            throw new NullPointerException("bootstrapMethodHandle == null");
        } else if (nat == null) {
            throw new NullPointerException("nat == null");
        }

        List list = new List(3 + optionalArguments.size());
        list.set(0, bootstrapHandle);
        list.set(1, nat.getName());
        list.set(2, new CstProtoRef(Prototype.fromDescriptor(nat.getDescriptor().getString())));
        if (optionalArguments != null) {
            for (int i = 0; i < optionalArguments.size(); ++i) {
                list.set(i + 3, optionalArguments.get(i));
            }
        }
        list.setImmutable();
        return new CstCallSite(list);
    }

    /**
     * Constructs an instance.
     *
     * @param list {@code non-null;} the actual list of contents
     */
    private CstCallSite(List list) {
        super(list);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other instanceof CstCallSite) {
            return getList().equals(((CstCallSite) other).getList());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getList().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        return getList().compareTo(((CstCallSite) other).getList());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getList().toString("call site{", ", ", "}");
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "call site";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return getList().toHuman("{", ", ", "}");
    }
}
