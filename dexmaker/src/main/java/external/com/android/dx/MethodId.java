/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.type.Prototype;
import java.util.List;

/**
 * Identifies a method or constructor.
 *
 * @param <D> the type declaring this field
 * @param <R> the return type of this method
 */
public final class MethodId<D, R> {
    final TypeId<D> declaringType;
    final TypeId<R> returnType;
    final String name;
    final TypeList parameters;

    /** cached converted state */
    final CstNat nat;
    final CstMethodRef constant;

    MethodId(TypeId<D> declaringType, TypeId<R> returnType, String name, TypeList parameters) {
        if (declaringType == null || returnType == null || name == null || parameters == null) {
            throw new NullPointerException();
        }
        this.declaringType = declaringType;
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.nat = new CstNat(new CstString(name), new CstString(descriptor(false)));
        this.constant = new CstMethodRef(declaringType.constant, nat);
    }

    public TypeId<D> getDeclaringType() {
        return declaringType;
    }

    public TypeId<R> getReturnType() {
        return returnType;
    }

    /**
     * Returns true if this method is a constructor for its declaring class.
     */
    public boolean isConstructor() {
        return name.equals("<init>");
    }

    /**
     * Returns true if this method is the static initializer for its declaring class.
     */
    public boolean isStaticInitializer() {
        return name.equals("<clinit>");
    }

    /**
     * Returns the method's name. This is "&lt;init&gt;" if this is a constructor
     * or "&lt;clinit&gt;" if a static initializer
     */
    public String getName() {
        return name;
    }

    public List<TypeId<?>> getParameters() {
        return parameters.asList();
    }

    /**
     * Returns a descriptor like "(Ljava/lang/Class;[I)Ljava/lang/Object;".
     */
    String descriptor(boolean includeThis) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        if (includeThis) {
            result.append(declaringType.name);
        }
        for (TypeId t : parameters.types) {
            result.append(t.name);
        }
        result.append(")");
        result.append(returnType.name);
        return result.toString();
    }

    Prototype prototype(boolean includeThis) {
        return Prototype.intern(descriptor(includeThis));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MethodId
                && ((MethodId<?, ?>) o).declaringType.equals(declaringType)
                && ((MethodId<?, ?>) o).name.equals(name)
                && ((MethodId<?, ?>) o).parameters.equals(parameters)
                && ((MethodId<?, ?>) o).returnType.equals(returnType);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + declaringType.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + returnType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return declaringType + "." + name + "(" + parameters + ")";
    }
}
