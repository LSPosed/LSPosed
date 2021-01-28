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

package external.com.android.dx.rop.type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Representation of a method descriptor. Instances of this class are
 * generally interned and may be usefully compared with each other
 * using {@code ==}.
 */
public final class Prototype implements Comparable<Prototype> {
    /**
     * Intern table for instances.
     *
     * <p>The initial capacity is based on a medium-size project.
     */
    private static final ConcurrentMap<String, Prototype> internTable =
            new ConcurrentHashMap<>(10_000, 0.75f);

    /** {@code non-null;} method descriptor */
    private final String descriptor;

    /** {@code non-null;} return type */
    private final Type returnType;

    /** {@code non-null;} list of parameter types */
    private final StdTypeList parameterTypes;

    /** {@code null-ok;} list of parameter frame types, if calculated */
    private StdTypeList parameterFrameTypes;

    /**
     * Returns the unique instance corresponding to the
     * given method descriptor. See vmspec-2 sec4.3.3 for details on the
     * field descriptor syntax.
     *
     * @param descriptor {@code non-null;} the descriptor
     * @return {@code non-null;} the corresponding instance
     * @throws IllegalArgumentException thrown if the descriptor has
     * invalid syntax
     */
    public static Prototype intern(String descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }

        Prototype result = internTable.get(descriptor);
        if (result != null) {
            return result;
        }

        result = fromDescriptor(descriptor);
        return putIntern(result);
    }

    /**
     * Returns a prototype for a method descriptor.
     *
     * The {@code Prototype} returned will be the interned value if present,
     * or a new instance otherwise. If a new instance is created, it is not
     * placed in the intern table.
     *
     * @param descriptor {@code non-null;} the descriptor
     * @return {@code non-null;} the corresponding instance
     * @throws IllegalArgumentException thrown if the descriptor has
     * invalid syntax
     */
    public static Prototype fromDescriptor(String descriptor) {
        Prototype result = internTable.get(descriptor);
        if (result != null) {
            return result;
        }

        Type[] params = makeParameterArray(descriptor);
        int paramCount = 0;
        int at = 1;

        for (;;) {
            int startAt = at;
            char c = descriptor.charAt(at);
            if (c == ')') {
                at++;
                break;
            }

            // Skip array markers.
            while (c == '[') {
                at++;
                c = descriptor.charAt(at);
            }

            if (c == 'L') {
                // It looks like the start of a class name; find the end.
                int endAt = descriptor.indexOf(';', at);
                if (endAt == -1) {
                    throw new IllegalArgumentException("bad descriptor");
                }
                at = endAt + 1;
            } else {
                at++;
            }

            params[paramCount] =
                Type.intern(descriptor.substring(startAt, at));
            paramCount++;
        }

        Type returnType = Type.internReturnType(descriptor.substring(at));
        StdTypeList parameterTypes = new StdTypeList(paramCount);

        for (int i = 0; i < paramCount; i++) {
            parameterTypes.set(i, params[i]);
        }

        return new Prototype(descriptor, returnType, parameterTypes);
    }

    public static void clearInternTable() {
        internTable.clear();
    }

    /**
     * Helper for {@link #intern} which returns an empty array to
     * populate with parsed parameter types, and which also ensures
     * that there is a '(' at the start of the descriptor and a
     * single ')' somewhere before the end.
     *
     * @param descriptor {@code non-null;} the descriptor string
     * @return {@code non-null;} array large enough to hold all parsed parameter
     * types, but which is likely actually larger than needed
     */
    private static Type[] makeParameterArray(String descriptor) {
        int length = descriptor.length();

        if (descriptor.charAt(0) != '(') {
            throw new IllegalArgumentException("bad descriptor");
        }

        /*
         * This is a cheesy way to establish an upper bound on the
         * number of parameters: Just count capital letters.
         */
        int closeAt = 0;
        int maxParams = 0;
        for (int i = 1; i < length; i++) {
            char c = descriptor.charAt(i);
            if (c == ')') {
                closeAt = i;
                break;
            }
            if ((c >= 'A') && (c <= 'Z')) {
                maxParams++;
            }
        }

        if ((closeAt == 0) || (closeAt == (length - 1))) {
            throw new IllegalArgumentException("bad descriptor");
        }

        if (descriptor.indexOf(')', closeAt + 1) != -1) {
            throw new IllegalArgumentException("bad descriptor");
        }

        return new Type[maxParams];
    }

    /**
     * Interns an instance, adding to the descriptor as necessary based
     * on the given definer, name, and flags. For example, an init
     * method has an uninitialized object of type {@code definer}
     * as its first argument.
     *
     * @param descriptor {@code non-null;} the descriptor string
     * @param definer {@code non-null;} class the method is defined on
     * @param isStatic whether this is a static method
     * @param isInit whether this is an init method
     * @return {@code non-null;} the interned instance
     */
    public static Prototype intern(String descriptor, Type definer,
            boolean isStatic, boolean isInit) {
        Prototype base = intern(descriptor);

        if (isStatic) {
            return base;
        }

        if (isInit) {
            definer = definer.asUninitialized(Integer.MAX_VALUE);
        }

        return base.withFirstParameter(definer);
    }

    /**
     * Interns an instance which consists of the given number of
     * {@code int}s along with the given return type
     *
     * @param returnType {@code non-null;} the return type
     * @param count {@code > 0;} the number of elements in the prototype
     * @return {@code non-null;} the interned instance
     */
    public static Prototype internInts(Type returnType, int count) {
        // Make the descriptor...

        StringBuilder sb = new StringBuilder(100);

        sb.append('(');

        for (int i = 0; i < count; i++) {
            sb.append('I');
        }

        sb.append(')');
        sb.append(returnType.getDescriptor());

        // ...and intern it.
        return intern(sb.toString());
    }

    /**
     * Constructs an instance. This is a private constructor; use one
     * of the public static methods to get instances.
     *
     * @param descriptor {@code non-null;} the descriptor string
     */
    private Prototype(String descriptor, Type returnType,
            StdTypeList parameterTypes) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }

        if (returnType == null) {
            throw new NullPointerException("returnType == null");
        }

        if (parameterTypes == null) {
            throw new NullPointerException("parameterTypes == null");
        }

        this.descriptor = descriptor;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterFrameTypes = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            /*
             * Since externally-visible instances are interned, this
             * check helps weed out some easy cases.
             */
            return true;
        }

        if (!(other instanceof Prototype)) {
            return false;
        }

        return descriptor.equals(((Prototype) other).descriptor);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Prototype other) {
        if (this == other) {
            return 0;
        }

        /*
         * The return type is the major order, and then args in order,
         * and then the shorter list comes first (similar to string
         * sorting).
         */

        int result = returnType.compareTo(other.returnType);

        if (result != 0) {
            return result;
        }

        int thisSize = parameterTypes.size();
        int otherSize = other.parameterTypes.size();
        int size = Math.min(thisSize, otherSize);

        for (int i = 0; i < size; i++) {
            Type thisType = parameterTypes.get(i);
            Type otherType = other.parameterTypes.get(i);

            result = thisType.compareTo(otherType);

            if (result != 0) {
                return result;
            }
        }

        if (thisSize < otherSize) {
            return -1;
        } else if (thisSize > otherSize) {
            return 1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return descriptor;
    }

    /**
     * Gets the descriptor string.
     *
     * @return {@code non-null;} the descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Gets the return type.
     *
     * @return {@code non-null;} the return type
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Gets the list of parameter types.
     *
     * @return {@code non-null;} the list of parameter types
     */
    public StdTypeList getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Gets the list of frame types corresponding to the list of parameter
     * types. The difference between the two lists (if any) is that all
     * "intlike" types (see {@link Type#isIntlike}) are replaced by
     * {@link Type#INT}.
     *
     * @return {@code non-null;} the list of parameter frame types
     */
    public StdTypeList getParameterFrameTypes() {
        if (parameterFrameTypes == null) {
            int sz = parameterTypes.size();
            StdTypeList list = new StdTypeList(sz);
            boolean any = false;
            for (int i = 0; i < sz; i++) {
                Type one = parameterTypes.get(i);
                if (one.isIntlike()) {
                    any = true;
                    one = Type.INT;
                }
                list.set(i, one);
            }
            parameterFrameTypes = any ? list : parameterTypes;
        }

        return parameterFrameTypes;
    }

    /**
     * Returns a new interned instance, which is the same as this instance,
     * except that it has an additional parameter prepended to the original's
     * argument list.
     *
     * @param param {@code non-null;} the new first parameter
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public Prototype withFirstParameter(Type param) {
        String newDesc = "(" + param.getDescriptor() + descriptor.substring(1);
        StdTypeList newParams = parameterTypes.withFirst(param);

        newParams.setImmutable();

        Prototype result =
            new Prototype(newDesc, returnType, newParams);

        return putIntern(result);
    }

    /**
     * Puts the given instance in the intern table if it's not already
     * there. If a conflicting value is already in the table, then leave it.
     * Return the interned value.
     *
     * @param desc {@code non-null;} instance to make interned
     * @return {@code non-null;} the actual interned object
     */
    private static Prototype putIntern(Prototype desc) {
        Prototype result = internTable.putIfAbsent(desc.getDescriptor(), desc);
        return result != null ? result : desc;
    }
}
