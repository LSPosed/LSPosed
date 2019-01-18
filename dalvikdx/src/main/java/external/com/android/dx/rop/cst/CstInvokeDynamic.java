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
import java.util.ArrayList;
import java.util.List;

/**
 * Constants of type {@code InvokeDynamic}. These are present in the class file.
 */
public final class CstInvokeDynamic extends Constant {

    /** The index of the bootstrap method in the bootstrap method table */
    private final int bootstrapMethodIndex;

    /** {@code non-null;} the name and type */
    private final CstNat nat;

    /** {@code non-null;} the prototype derived from {@code nat} */
    private final Prototype prototype;

    /** {@code null-ok;} the class declaring invoke dynamic instance */
    private CstType declaringClass;

    /** {@code null-ok;} the associated call site */
    private CstCallSite callSite;

    /** {@code non-null;} list of references to this {@code CstInvokeDynamic} constant */
    private final List<CstCallSiteRef> references;

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param bootstrapMethodIndex The index of the bootstrap method in the bootstrap method table
     * @param nat the name and type
     * @return {@code non-null;} the appropriate instance
     */
    public static CstInvokeDynamic make(int bootstrapMethodIndex, CstNat nat) {
        return new CstInvokeDynamic(bootstrapMethodIndex, nat);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param bootstrapMethodIndex The index of the bootstrap method in the bootstrap method table
     * @param nat the name and type
     */
    private CstInvokeDynamic(int bootstrapMethodIndex, CstNat nat) {
        this.bootstrapMethodIndex = bootstrapMethodIndex;
        this.nat = nat;
        this.prototype = Prototype.fromDescriptor(nat.getDescriptor().toHuman());
        this.references = new ArrayList<>();
    }

    /**
     * Creates a {@code CstCallSiteRef} that refers to this instance.
     *
     * @return {@code non-null;} a reference to this instance
     */
    public CstCallSiteRef addReference() {
        CstCallSiteRef ref = new CstCallSiteRef(this, references.size());
        references.add(ref);
        return ref;
    }

    /**
     * Gets the list of references to this instance.
     *
     * @return {@code non-null;} the list of references to this instance
     */
    public List<CstCallSiteRef> getReferences() {
        return references;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "InvokeDynamic";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        String klass = (declaringClass != null) ? declaringClass.toHuman() : "Unknown";
        return "InvokeDynamic(" + klass + ":" + bootstrapMethodIndex + ", " + nat.toHuman() + ")";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        CstInvokeDynamic otherInvoke = (CstInvokeDynamic) other;
        int result = Integer.compare(bootstrapMethodIndex, otherInvoke.getBootstrapMethodIndex());
        if (result != 0) {
            return result;
        }

        result = nat.compareTo(otherInvoke.getNat());
        if (result != 0) {
            return result;
        }

        result = declaringClass.compareTo(otherInvoke.getDeclaringClass());
        if (result != 0) {
            return result;
        }

        return callSite.compareTo(otherInvoke.getCallSite());
    }

    /**
     * Gets the bootstrap method index.
     *
     * @return the bootstrap method index
     */
    public int getBootstrapMethodIndex() {
        return bootstrapMethodIndex;
    }

    /**
     * Gets the {@code CstNat} value.
     *
     * @return the name and type
     */
    public CstNat getNat() {
        return nat;
    }

    /**
     * Gets the {@code Prototype} of the {@code invokedynamic} call site.
     *
     * @return the {@code invokedynamic} call site prototype
     */
    public Prototype getPrototype() {
        return prototype;
    }

    /**
     * Gets the return type.
     *
     * @return {@code non-null;} the return type
     */
    public Type getReturnType() {
        return prototype.getReturnType();
    }

    /**
     * Sets the declaring class of this instance.
     *
     * This is a set-once property.
     *
     * @param declaringClass {@code non-null;} the declaring class
     */
    public void setDeclaringClass(CstType declaringClass) {
        if (this.declaringClass != null) {
            throw new IllegalArgumentException("already added declaring class");
        } else if (declaringClass == null) {
            throw new NullPointerException("declaringClass == null");
        }
        this.declaringClass = declaringClass;
    }

    /**
     * Gets the declaring class of this instance.
     *
     * @return the declaring class
     */
    public CstType getDeclaringClass() {
        return declaringClass;
    }

    /**
     * Sets the call site associated with this instance.
     *
     * This is set-once property.
     *
     * @param callSite {@code non-null;} the call site created for this instance
     */
    public void setCallSite(CstCallSite callSite) {
        if (this.callSite != null) {
            throw new IllegalArgumentException("already added call site");
        } else if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        this.callSite = callSite;
    }

    /**
     * Gets the call site associated with this instance.
     *
     * @return the call site associated with this instance
     */
    public CstCallSite getCallSite() {
        return callSite;
    }
}
