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

import external.com.android.dx.rop.cst.CstType;
import java.util.HashMap;
import java.util.Map;

/**
 * A primitive type, interface or class.
 *
 * <p><strong>Warning:</strong> Use care when dealing with boxed primitive
 * types. Java's lack of support for parameterized primitive types means that
 * a primitive type like {@code int} and its boxed type {@code
 * java.lang.Integer} have the same type parameter: {@code TypeId<Integer>}.
 * These types are different and it will be a runtime error if the boxed type
 * {@code java.lang.Integer} is used where the primitive type {@code int} is
 * expected.
 */
public final class TypeId<T> {
    /** The {@code boolean} primitive type. */
    public static final TypeId<Boolean> BOOLEAN = new TypeId<>(external.com.android.dx.rop.type.Type.BOOLEAN);

    /** The {@code byte} primitive type. */
    public static final TypeId<Byte> BYTE = new TypeId<>(external.com.android.dx.rop.type.Type.BYTE);

    /** The {@code char} primitive type. */
    public static final TypeId<Character> CHAR = new TypeId<>(external.com.android.dx.rop.type.Type.CHAR);

    /** The {@code double} primitive type. */
    public static final TypeId<Double> DOUBLE = new TypeId<>(external.com.android.dx.rop.type.Type.DOUBLE);

    /** The {@code float} primitive type. */
    public static final TypeId<Float> FLOAT = new TypeId<>(external.com.android.dx.rop.type.Type.FLOAT);

    /** The {@code int} primitive type. */
    public static final TypeId<Integer> INT = new TypeId<>(external.com.android.dx.rop.type.Type.INT);

    /** The {@code long} primitive type. */
    public static final TypeId<Long> LONG = new TypeId<>(external.com.android.dx.rop.type.Type.LONG);

    /** The {@code short} primitive type. */
    public static final TypeId<Short> SHORT = new TypeId<>(external.com.android.dx.rop.type.Type.SHORT);

    /** The {@code void} primitive type. Only used as a return type. */
    public static final TypeId<Void> VOID = new TypeId<>(external.com.android.dx.rop.type.Type.VOID);

    /** The {@code Object} type. */
    public static final TypeId<Object> OBJECT = new TypeId<>(external.com.android.dx.rop.type.Type.OBJECT);

    /** The {@code String} type. */
    public static final TypeId<String> STRING = new TypeId<>(external.com.android.dx.rop.type.Type.STRING);

    private static final Map<Class<?>, TypeId<?>> PRIMITIVE_TO_TYPE = new HashMap<>();
    static {
        PRIMITIVE_TO_TYPE.put(boolean.class, BOOLEAN);
        PRIMITIVE_TO_TYPE.put(byte.class, BYTE);
        PRIMITIVE_TO_TYPE.put(char.class, CHAR);
        PRIMITIVE_TO_TYPE.put(double.class, DOUBLE);
        PRIMITIVE_TO_TYPE.put(float.class, FLOAT);
        PRIMITIVE_TO_TYPE.put(int.class, INT);
        PRIMITIVE_TO_TYPE.put(long.class, LONG);
        PRIMITIVE_TO_TYPE.put(short.class, SHORT);
        PRIMITIVE_TO_TYPE.put(void.class, VOID);
    }

    final String name;

    /** cached converted values */
    final external.com.android.dx.rop.type.Type ropType;
    final CstType constant;

    TypeId(external.com.android.dx.rop.type.Type ropType) {
        this(ropType.getDescriptor(), ropType);
    }

    TypeId(String name, external.com.android.dx.rop.type.Type ropType) {
        if (name == null || ropType == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.ropType = ropType;
        this.constant = CstType.intern(ropType);
    }

    /**
     * @param name a descriptor like "Ljava/lang/Class;".
     */
    public static <T> TypeId<T> get(String name) {
        return new TypeId<>(name, external.com.android.dx.rop.type.Type.internReturnType(name));
    }

    public static <T> TypeId<T> get(Class<T> type) {
        if (type.isPrimitive()) {
            // guarded by equals
            @SuppressWarnings("unchecked")
            TypeId<T> result = (TypeId<T>) PRIMITIVE_TO_TYPE.get(type);
            return result;
        }
        String name = type.getName().replace('.', '/');
        return get(type.isArray() ? name : 'L' + name + ';');
    }

    public <V> FieldId<T, V> getField(TypeId<V> type, String name) {
        return new FieldId<>(this, type, name);
    }

    public MethodId<T, Void> getConstructor(TypeId<?>... parameters) {
        return new MethodId<>(this, VOID, "<init>", new TypeList(parameters));
    }

    public MethodId<T, Void> getStaticInitializer() {
        return new MethodId<>(this, VOID, "<clinit>", new TypeList(new TypeId[0]));
    }

    public <R> MethodId<T, R> getMethod(TypeId<R> returnType, String name, TypeId<?>... parameters) {
        return new MethodId<>(this, returnType, name, new TypeList(parameters));
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TypeId
                && ((TypeId) o).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
