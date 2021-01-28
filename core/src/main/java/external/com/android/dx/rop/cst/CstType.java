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

package external.com.android.dx.rop.cst;

import external.com.android.dx.rop.type.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Constants that represent an arbitrary type (reference or primitive).
 */
public final class CstType extends TypedConstant {

    /**
     * Intern table for instances.
     *
     * <p>The initial capacity is based on a medium-size project.
     */
    private static final ConcurrentMap<Type, CstType> interns =
            new ConcurrentHashMap<>(1_000, 0.75f);

    /** {@code non-null;} instance corresponding to the class {@code Object} */
    public static final CstType OBJECT = new CstType(Type.OBJECT);

    /** {@code non-null;} instance corresponding to the class {@code Boolean} */
    public static final CstType BOOLEAN = new CstType(Type.BOOLEAN_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Byte} */
    public static final CstType BYTE = new CstType(Type.BYTE_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Character} */
    public static final CstType CHARACTER = new CstType(Type.CHARACTER_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Double} */
    public static final CstType DOUBLE = new CstType(Type.DOUBLE_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Float} */
    public static final CstType FLOAT = new CstType(Type.FLOAT_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Long} */
    public static final CstType LONG = new CstType(Type.LONG_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Integer} */
    public static final CstType INTEGER = new CstType(Type.INTEGER_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Short} */
    public static final CstType SHORT = new CstType(Type.SHORT_CLASS);

    /** {@code non-null;} instance corresponding to the class {@code Void} */
    public static final CstType VOID = new CstType(Type.VOID_CLASS);

    /** {@code non-null;} instance corresponding to the type {@code boolean[]} */
    public static final CstType BOOLEAN_ARRAY = new CstType(Type.BOOLEAN_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code byte[]} */
    public static final CstType BYTE_ARRAY = new CstType(Type.BYTE_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code char[]} */
    public static final CstType CHAR_ARRAY = new CstType(Type.CHAR_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code double[]} */
    public static final CstType DOUBLE_ARRAY = new CstType(Type.DOUBLE_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code float[]} */
    public static final CstType FLOAT_ARRAY = new CstType(Type.FLOAT_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code long[]} */
    public static final CstType LONG_ARRAY = new CstType(Type.LONG_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code int[]} */
    public static final CstType INT_ARRAY = new CstType(Type.INT_ARRAY);

    /** {@code non-null;} instance corresponding to the type {@code short[]} */
    public static final CstType SHORT_ARRAY = new CstType(Type.SHORT_ARRAY);

    /**
     * {@code non-null;} instance corresponding to the type {@code java.lang.invoke.MethodHandle}
     */
    public static final CstType METHOD_HANDLE = new CstType(Type.METHOD_HANDLE);

    /**
     * {@code non-null;} instance corresponding to the type {@code java.lang.invoke.VarHandle}
     */
    public static final CstType VAR_HANDLE = new CstType(Type.VAR_HANDLE);

    static {
        initInterns();
    }

    private static void initInterns() {
        internInitial(OBJECT);
        internInitial(BOOLEAN);
        internInitial(BYTE);
        internInitial(CHARACTER);
        internInitial(DOUBLE);
        internInitial(FLOAT);
        internInitial(LONG);
        internInitial(INTEGER);
        internInitial(SHORT);
        internInitial(VOID);
        internInitial(BOOLEAN_ARRAY);
        internInitial(BYTE_ARRAY);
        internInitial(CHAR_ARRAY);
        internInitial(DOUBLE_ARRAY);
        internInitial(FLOAT_ARRAY);
        internInitial(LONG_ARRAY);
        internInitial(INT_ARRAY);
        internInitial(SHORT_ARRAY);
        internInitial(METHOD_HANDLE);
    }

    private static void internInitial(CstType cst) {
        if (interns.putIfAbsent(cst.getClassType(), cst) != null) {
            throw new IllegalStateException("Attempted re-init of " + cst);
        }
    }


    /** {@code non-null;} the underlying type */
    private final Type type;

    /**
     * {@code null-ok;} the type descriptor corresponding to this instance, if
     * calculated
     */
    private CstString descriptor;

    /**
     * Returns an instance of this class that represents the wrapper
     * class corresponding to a given primitive type. For example, if
     * given {@link Type#INT}, this method returns the class reference
     * {@code java.lang.Integer}.
     *
     * @param primitiveType {@code non-null;} the primitive type
     * @return {@code non-null;} the corresponding wrapper class
     */
    public static CstType forBoxedPrimitiveType(Type primitiveType) {
        switch (primitiveType.getBasicType()) {
            case Type.BT_BOOLEAN: return BOOLEAN;
            case Type.BT_BYTE:    return BYTE;
            case Type.BT_CHAR:    return CHARACTER;
            case Type.BT_DOUBLE:  return DOUBLE;
            case Type.BT_FLOAT:   return FLOAT;
            case Type.BT_INT:     return INTEGER;
            case Type.BT_LONG:    return LONG;
            case Type.BT_SHORT:   return SHORT;
            case Type.BT_VOID:    return VOID;
        }

        throw new IllegalArgumentException("not primitive: " + primitiveType);
    }

    /**
     * Returns an interned instance of this class for the given type.
     *
     * @param type {@code non-null;} the underlying type
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static CstType intern(Type type) {
        CstType cst = new CstType(type);
        CstType result = interns.putIfAbsent(type, cst);
        return result != null ? result : cst;
    }

    /**
     * Constructs an instance.
     *
     * @param type {@code non-null;} the underlying type
     */
    public CstType(Type type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }

        if (type == Type.KNOWN_NULL) {
            throw new UnsupportedOperationException(
                    "KNOWN_NULL is not representable");
        }

        this.type = type;
        this.descriptor = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CstType)) {
            return false;
        }

        return type == ((CstType) other).type;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        String thisDescriptor = type.getDescriptor();
        String otherDescriptor = ((CstType) other).type.getDescriptor();
        return thisDescriptor.compareTo(otherDescriptor);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "type{" + toHuman() + '}';
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.CLASS;
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "type";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return type.toHuman();
    }

    /**
     * Gets the underlying type (as opposed to the type corresponding
     * to this instance as a constant, which is always
     * {@code Class}).
     *
     * @return {@code non-null;} the type corresponding to the name
     */
    public Type getClassType() {
        return type;
    }

    /**
     * Gets the type descriptor for this instance.
     *
     * @return {@code non-null;} the descriptor
     */
    public CstString getDescriptor() {
        if (descriptor == null) {
            descriptor = new CstString(type.getDescriptor());
        }

        return descriptor;
    }

    /**
     * Returns a human readable package name for this type, like "java.util".
     * If this is an array type, this returns the package name of the array's
     * component type. If this is a primitive type, this returns "default".
     */
    public String getPackageName() {
        // descriptor is a string like "[[Ljava/util/String;"
        String descriptor = getDescriptor().getString();
        int lastSlash = descriptor.lastIndexOf('/');
        int lastLeftSquare = descriptor.lastIndexOf('['); // -1 unless this is an array
        if (lastSlash == -1) {
            return "default";
        } else {
            // +2 to skip the '[' and the 'L' prefix
            return descriptor.substring(lastLeftSquare + 2, lastSlash).replace('/', '.');
        }
    }

    public static void clearInternTable() {
        interns.clear();
        initInterns();
    }

}
