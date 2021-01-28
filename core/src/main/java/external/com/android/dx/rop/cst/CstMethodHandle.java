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

import external.com.android.dx.rop.type.Type;

/**
 * Constants of type {@code MethodHandle}.
 */
public final class CstMethodHandle extends TypedConstant {

    public static final int METHOD_HANDLE_TYPE_STATIC_PUT = 0;
    public static final int METHOD_HANDLE_TYPE_STATIC_GET = 1;
    public static final int METHOD_HANDLE_TYPE_INSTANCE_PUT = 2;
    public static final int METHOD_HANDLE_TYPE_INSTANCE_GET = 3;

    public static final int METHOD_HANDLE_TYPE_INVOKE_STATIC = 4;
    public static final int METHOD_HANDLE_TYPE_INVOKE_INSTANCE = 5;
    public static final int METHOD_HANDLE_TYPE_INVOKE_CONSTRUCTOR = 6;
    public static final int METHOD_HANDLE_TYPE_INVOKE_DIRECT = 7;
    public static final int METHOD_HANDLE_TYPE_INVOKE_INTERFACE = 8;

    private static final String [] TYPE_NAMES = {
        "static-put", "static-get", "instance-put", "instance-get",
        "invoke-static", "invoke-instance", "invoke-constructor", "invoke-direct",
        "invoke-interface"
    };

    /** The type of MethodHandle */
    private final int type;

    /** {@code non-null;} the referenced constant */
    private final Constant ref;

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param type the type of this handle
     * @param ref {@code non-null;} the referenced field or method constant
     * @return {@code non-null;} the appropriate instance
     */
    public static CstMethodHandle make(int type, Constant ref) {
        if (isAccessor(type)) {
            if (!(ref instanceof CstFieldRef)) {
                throw new IllegalArgumentException("ref has wrong type: " + ref.getClass());
            }
        } else if (isInvocation(type)) {
            if (!(ref instanceof CstBaseMethodRef)) {
                throw new IllegalArgumentException("ref has wrong type: " + ref.getClass());
            }
        } else {
            throw new IllegalArgumentException("type is out of range: " + type);
        }
        return new CstMethodHandle(type, ref);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param type the type of this handle
     * @param ref the actual referenced constant
     */
    private CstMethodHandle(int type, Constant ref) {
        this.type = type;
        this.ref = ref;
    }

    /**
     * Gets the actual constant.
     *
     * @return the value
     */
    public Constant getRef() {
        return ref;
    }

    /**
     * Gets the type of this method handle.
     *
     * @return the type
     */
    public int getMethodHandleType() {
        return type;
    }

    /**
     * Reports whether the method handle type is a field accessor.
     *
     * @param type the method handle type
     * @return true if the method handle type is a field accessor, false otherwise
     */
    public static boolean isAccessor(int type) {
        switch (type) {
            case METHOD_HANDLE_TYPE_STATIC_PUT:
            case METHOD_HANDLE_TYPE_STATIC_GET:
            case METHOD_HANDLE_TYPE_INSTANCE_PUT:
            case METHOD_HANDLE_TYPE_INSTANCE_GET:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reports whether the method handle is a field accessor.
     *
     * @return true if the method handle is a field accessor, false otherwise
     */
    public boolean isAccessor() {
        return isAccessor(type);
    }

    /**
     * Reports whether the method handle type is a method invocation.
     *
     * @param type the method handle type
     * @return true if the method handle type is a method invocation, false otherwise
     */
    public static boolean isInvocation(int type) {
        switch (type) {
            case METHOD_HANDLE_TYPE_INVOKE_STATIC:
            case METHOD_HANDLE_TYPE_INVOKE_INSTANCE:
            case METHOD_HANDLE_TYPE_INVOKE_CONSTRUCTOR:
            case METHOD_HANDLE_TYPE_INVOKE_DIRECT:
            case METHOD_HANDLE_TYPE_INVOKE_INTERFACE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reports whether the method handle is a method invocation.
     *
     * @return true if the method handle is a method invocation, false otherwise
     */
    public boolean isInvocation() {
        return isInvocation(type);
    }

    /**
     * Gets a human readable name for a method handle type.
     *
     * @param type the method handle type
     * @return the string representation of the type
     */
    public static String getMethodHandleTypeName(final int type) {
        return TYPE_NAMES[type];
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        CstMethodHandle otherHandle = (CstMethodHandle) other;
        if (getMethodHandleType() == otherHandle.getMethodHandleType()) {
            return getRef().compareTo(otherHandle.getRef());
        } else {
            return Integer.compare(getMethodHandleType(), otherHandle.getMethodHandleType());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "method-handle{" + toHuman() + "}";
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "method handle";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return getMethodHandleTypeName(type)+ "," + ref.toString();
    }

    @Override
    public Type getType() {
        return Type.METHOD_HANDLE;
    }
}
