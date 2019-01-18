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

import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.Type;

/**
 * Reference to a call site. Each instance of the invoke-custom bytecode uses a unique call site
 * reference. The call site reference becomes a call site id in the DEX file and multiple call
 * site id's can refer to the same call site data.
 */
public class CstCallSiteRef extends Constant {

    /** {@code non-null;} The invokedynamic constant from the classfile */
    private final CstInvokeDynamic invokeDynamic;

    /** A unique identifier to ensure the uniqueness of {@code CstCallSiteRef} instances. */
    private final int id;

    /**
     * Constructs an instance.
     *
     * @param invokeDynamic {@code non-null;} an instance of invokeDynamic for reference
     * @param id a distinguishing integer for instances referring to the same
     *      {@code CstInvokeDynamic} instance
     */
    CstCallSiteRef(CstInvokeDynamic invokeDynamic, int id) {
        if (invokeDynamic == null) {
            throw new NullPointerException("invokeDynamic == null");
        }
        this.invokeDynamic = invokeDynamic;
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "CallSiteRef";
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        CstCallSiteRef o = (CstCallSiteRef) other;
        int result = invokeDynamic.compareTo(o.invokeDynamic);
        if (result != 0) {
            return result;
        }
        return Integer.compare(id, o.id);
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return getCallSite().toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getCallSite().toString();
    }

    /**
     * Gets the prototype of the method handle resolved at the call site.
     *
     * @return the prototype associated with the call site invocation
     */
    public Prototype getPrototype() {
        return invokeDynamic.getPrototype();
    }

    /**
     * Gets the return type of the method handle resolved at the call site.
     *
     * @return the return type associated with the call site invocation
     */
    public Type getReturnType() {
        return invokeDynamic.getReturnType();
    }

    /**
     * Gets the {@code CstCallSite} that this instance refers to.
     *
     * @return {@code null-ok;} the call site associated with this instance
     */
    public CstCallSite getCallSite() {
        return invokeDynamic.getCallSite();
    }
}
