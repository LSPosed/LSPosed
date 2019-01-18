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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;

/**
 * Common exception types.
 */
public final class Exceptions {
    /** {@code non-null;} the type {@code java.lang.ArithmeticException} */
    public static final Type TYPE_ArithmeticException =
        Type.intern("Ljava/lang/ArithmeticException;");

    /**
     * {@code non-null;} the type
     * {@code java.lang.ArrayIndexOutOfBoundsException}
     */
    public static final Type TYPE_ArrayIndexOutOfBoundsException =
        Type.intern("Ljava/lang/ArrayIndexOutOfBoundsException;");

    /** {@code non-null;} the type {@code java.lang.ArrayStoreException} */
    public static final Type TYPE_ArrayStoreException =
        Type.intern("Ljava/lang/ArrayStoreException;");

    /** {@code non-null;} the type {@code java.lang.ClassCastException} */
    public static final Type TYPE_ClassCastException =
        Type.intern("Ljava/lang/ClassCastException;");

    /** {@code non-null;} the type {@code java.lang.Error} */
    public static final Type TYPE_Error = Type.intern("Ljava/lang/Error;");

    /**
     * {@code non-null;} the type
     * {@code java.lang.IllegalMonitorStateException}
     */
    public static final Type TYPE_IllegalMonitorStateException =
        Type.intern("Ljava/lang/IllegalMonitorStateException;");

    /** {@code non-null;} the type {@code java.lang.NegativeArraySizeException} */
    public static final Type TYPE_NegativeArraySizeException =
        Type.intern("Ljava/lang/NegativeArraySizeException;");

    /** {@code non-null;} the type {@code java.lang.NullPointerException} */
    public static final Type TYPE_NullPointerException =
        Type.intern("Ljava/lang/NullPointerException;");

    /** {@code non-null;} the list {@code [java.lang.Error]} */
    public static final StdTypeList LIST_Error = StdTypeList.make(TYPE_Error);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.ArithmeticException] }
     */
    public static final StdTypeList LIST_Error_ArithmeticException =
        StdTypeList.make(TYPE_Error, TYPE_ArithmeticException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.ClassCastException]}
     */
    public static final StdTypeList LIST_Error_ClassCastException =
        StdTypeList.make(TYPE_Error, TYPE_ClassCastException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.NegativeArraySizeException]}
     */
    public static final StdTypeList LIST_Error_NegativeArraySizeException =
        StdTypeList.make(TYPE_Error, TYPE_NegativeArraySizeException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.NullPointerException]}
     */
    public static final StdTypeList LIST_Error_NullPointerException =
        StdTypeList.make(TYPE_Error, TYPE_NullPointerException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.NullPointerException,
     * java.lang.ArrayIndexOutOfBoundsException]}
     */
    public static final StdTypeList LIST_Error_Null_ArrayIndexOutOfBounds =
        StdTypeList.make(TYPE_Error,
                      TYPE_NullPointerException,
                      TYPE_ArrayIndexOutOfBoundsException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.NullPointerException,
     * java.lang.ArrayIndexOutOfBoundsException,
     * java.lang.ArrayStoreException]}
     */
    public static final StdTypeList LIST_Error_Null_ArrayIndex_ArrayStore =
        StdTypeList.make(TYPE_Error,
                      TYPE_NullPointerException,
                      TYPE_ArrayIndexOutOfBoundsException,
                      TYPE_ArrayStoreException);

    /**
     * {@code non-null;} the list {@code [java.lang.Error,
     * java.lang.NullPointerException,
     * java.lang.IllegalMonitorStateException]}
     */
    public static final StdTypeList
        LIST_Error_Null_IllegalMonitorStateException =
        StdTypeList.make(TYPE_Error,
                      TYPE_NullPointerException,
                      TYPE_IllegalMonitorStateException);

    /**
     * This class is uninstantiable.
     */
    private Exceptions() {
        // This space intentionally left blank.
    }
}
