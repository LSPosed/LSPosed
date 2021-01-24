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

import external.com.android.dx.util.FixedSizeList;

/**
 * Standard implementation of {@link TypeList}.
 */
public final class StdTypeList
        extends FixedSizeList implements TypeList {
    /** {@code non-null;} no-element instance */
    public static final StdTypeList EMPTY = new StdTypeList(0);

    /** {@code non-null;} the list {@code [int]} */
    public static final StdTypeList INT = StdTypeList.make(Type.INT);

    /** {@code non-null;} the list {@code [long]} */
    public static final StdTypeList LONG = StdTypeList.make(Type.LONG);

    /** {@code non-null;} the list {@code [float]} */
    public static final StdTypeList FLOAT = StdTypeList.make(Type.FLOAT);

    /** {@code non-null;} the list {@code [double]} */
    public static final StdTypeList DOUBLE = StdTypeList.make(Type.DOUBLE);

    /** {@code non-null;} the list {@code [Object]} */
    public static final StdTypeList OBJECT = StdTypeList.make(Type.OBJECT);

    /** {@code non-null;} the list {@code [ReturnAddress]} */
    public static final StdTypeList RETURN_ADDRESS
            = StdTypeList.make(Type.RETURN_ADDRESS);

    /** {@code non-null;} the list {@code [Throwable]} */
    public static final StdTypeList THROWABLE =
        StdTypeList.make(Type.THROWABLE);

    /** {@code non-null;} the list {@code [int, int]} */
    public static final StdTypeList INT_INT =
        StdTypeList.make(Type.INT, Type.INT);

    /** {@code non-null;} the list {@code [long, long]} */
    public static final StdTypeList LONG_LONG =
        StdTypeList.make(Type.LONG, Type.LONG);

    /** {@code non-null;} the list {@code [float, float]} */
    public static final StdTypeList FLOAT_FLOAT =
        StdTypeList.make(Type.FLOAT, Type.FLOAT);

    /** {@code non-null;} the list {@code [double, double]} */
    public static final StdTypeList DOUBLE_DOUBLE =
        StdTypeList.make(Type.DOUBLE, Type.DOUBLE);

    /** {@code non-null;} the list {@code [Object, Object]} */
    public static final StdTypeList OBJECT_OBJECT =
        StdTypeList.make(Type.OBJECT, Type.OBJECT);

    /** {@code non-null;} the list {@code [int, Object]} */
    public static final StdTypeList INT_OBJECT =
        StdTypeList.make(Type.INT, Type.OBJECT);

    /** {@code non-null;} the list {@code [long, Object]} */
    public static final StdTypeList LONG_OBJECT =
        StdTypeList.make(Type.LONG, Type.OBJECT);

    /** {@code non-null;} the list {@code [float, Object]} */
    public static final StdTypeList FLOAT_OBJECT =
        StdTypeList.make(Type.FLOAT, Type.OBJECT);

    /** {@code non-null;} the list {@code [double, Object]} */
    public static final StdTypeList DOUBLE_OBJECT =
        StdTypeList.make(Type.DOUBLE, Type.OBJECT);

    /** {@code non-null;} the list {@code [long, int]} */
    public static final StdTypeList LONG_INT =
        StdTypeList.make(Type.LONG, Type.INT);

    /** {@code non-null;} the list {@code [int[], int]} */
    public static final StdTypeList INTARR_INT =
        StdTypeList.make(Type.INT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [long[], int]} */
    public static final StdTypeList LONGARR_INT =
        StdTypeList.make(Type.LONG_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [float[], int]} */
    public static final StdTypeList FLOATARR_INT =
        StdTypeList.make(Type.FLOAT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [double[], int]} */
    public static final StdTypeList DOUBLEARR_INT =
        StdTypeList.make(Type.DOUBLE_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [Object[], int]} */
    public static final StdTypeList OBJECTARR_INT =
        StdTypeList.make(Type.OBJECT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [boolean[], int]} */
    public static final StdTypeList BOOLEANARR_INT =
        StdTypeList.make(Type.BOOLEAN_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [byte[], int]} */
    public static final StdTypeList BYTEARR_INT =
        StdTypeList.make(Type.BYTE_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [char[], int]} */
    public static final StdTypeList CHARARR_INT =
        StdTypeList.make(Type.CHAR_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [short[], int]} */
    public static final StdTypeList SHORTARR_INT =
        StdTypeList.make(Type.SHORT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [int, int[], int]} */
    public static final StdTypeList INT_INTARR_INT =
        StdTypeList.make(Type.INT, Type.INT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [long, long[], int]} */
    public static final StdTypeList LONG_LONGARR_INT =
        StdTypeList.make(Type.LONG, Type.LONG_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [float, float[], int]} */
    public static final StdTypeList FLOAT_FLOATARR_INT =
        StdTypeList.make(Type.FLOAT, Type.FLOAT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [double, double[], int]} */
    public static final StdTypeList DOUBLE_DOUBLEARR_INT =
        StdTypeList.make(Type.DOUBLE, Type.DOUBLE_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [Object, Object[], int]} */
    public static final StdTypeList OBJECT_OBJECTARR_INT =
        StdTypeList.make(Type.OBJECT, Type.OBJECT_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [int, boolean[], int]} */
    public static final StdTypeList INT_BOOLEANARR_INT =
        StdTypeList.make(Type.INT, Type.BOOLEAN_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [int, byte[], int]} */
    public static final StdTypeList INT_BYTEARR_INT =
        StdTypeList.make(Type.INT, Type.BYTE_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [int, char[], int]} */
    public static final StdTypeList INT_CHARARR_INT =
        StdTypeList.make(Type.INT, Type.CHAR_ARRAY, Type.INT);

    /** {@code non-null;} the list {@code [int, short[], int]} */
    public static final StdTypeList INT_SHORTARR_INT =
        StdTypeList.make(Type.INT, Type.SHORT_ARRAY, Type.INT);

    /**
     * Makes a single-element instance.
     *
     * @param type {@code non-null;} the element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static StdTypeList make(Type type) {
        StdTypeList result = new StdTypeList(1);
        result.set(0, type);
        return result;
    }

    /**
     * Makes a two-element instance.
     *
     * @param type0 {@code non-null;} the first element
     * @param type1 {@code non-null;} the second element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static StdTypeList make(Type type0, Type type1) {
        StdTypeList result = new StdTypeList(2);
        result.set(0, type0);
        result.set(1, type1);
        return result;
    }

    /**
     * Makes a three-element instance.
     *
     * @param type0 {@code non-null;} the first element
     * @param type1 {@code non-null;} the second element
     * @param type2 {@code non-null;} the third element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static StdTypeList make(Type type0, Type type1, Type type2) {
        StdTypeList result = new StdTypeList(3);
        result.set(0, type0);
        result.set(1, type1);
        result.set(2, type2);
        return result;
    }

    /**
     * Makes a four-element instance.
     *
     * @param type0 {@code non-null;} the first element
     * @param type1 {@code non-null;} the second element
     * @param type2 {@code non-null;} the third element
     * @param type3 {@code non-null;} the fourth element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static StdTypeList make(Type type0, Type type1, Type type2,
                                   Type type3) {
        StdTypeList result = new StdTypeList(4);
        result.set(0, type0);
        result.set(1, type1);
        result.set(2, type2);
        result.set(3, type3);
        return result;
    }

    /**
     * Returns the given list as a comma-separated list of human forms. This
     * is a static method so as to work on arbitrary {@link TypeList}
     * instances.
     *
     * @param list {@code non-null;} the list to convert
     * @return {@code non-null;} the human form
     */
    public static String toHuman(TypeList list) {
        int size = list.size();

        if (size == 0) {
            return "<empty>";
        }

        StringBuilder sb = new StringBuilder(100);

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(list.getType(i).toHuman());
        }

        return sb.toString();
    }

    /**
     * Returns a hashcode of the contents of the given list. This
     * is a static method so as to work on arbitrary {@link TypeList}
     * instances.
     *
     * @param list {@code non-null;} the list to inspect
     * @return {@code non-null;} the hash code
     */
    public static int hashContents(TypeList list) {
        int size = list.size();
        int hash = 0;

        for (int i = 0; i < size; i++) {
            hash = (hash * 31) + list.getType(i).hashCode();
        }

        return hash;
    }

    /**
     * Compares the contents of the given two instances for equality. This
     * is a static method so as to work on arbitrary {@link TypeList}
     * instances.
     *
     * @param list1 {@code non-null;} one list to compare
     * @param list2 {@code non-null;} another list to compare
     * @return whether the two lists contain corresponding equal elements
     */
    public static boolean equalContents(TypeList list1, TypeList list2) {
        int size = list1.size();

        if (list2.size() != size) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (! list1.getType(i).equals(list2.getType(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares the contents of the given two instances for ordering. This
     * is a static method so as to work on arbitrary {@link TypeList}
     * instances.
     *
     * @param list1 {@code non-null;} one list to compare
     * @param list2 {@code non-null;} another list to compare
     * @return the order of the two lists
     */
    public static int compareContents(TypeList list1, TypeList list2) {
        int size1 = list1.size();
        int size2 = list2.size();
        int size = Math.min(size1, size2);

        for (int i = 0; i < size; i++) {
            int comparison = list1.getType(i).compareTo(list2.getType(i));
            if (comparison != 0) {
                return comparison;
            }
        }

        if (size1 == size2) {
            return 0;
        } else if (size1 < size2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     */
    public StdTypeList(int size) {
        super(size);
    }

    /** {@inheritDoc} */
    @Override
    public Type getType(int n) {
        return get(n);
    }

    /** {@inheritDoc} */
    @Override
    public int getWordCount() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            result += get(i).getCategory();
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public TypeList withAddedType(Type type) {
        int sz = size();
        StdTypeList result = new StdTypeList(sz + 1);

        for (int i = 0; i < sz; i++) {
            result.set0(i, get0(i));
        }

        result.set(sz, type);
        result.setImmutable();
        return result;
    }

    /**
     * Gets the indicated element. It is an error to call this with the
     * index for an element which was never set; if you do that, this
     * will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which element
     * @return {@code non-null;} the indicated element
     */
    public Type get(int n) {
        return (Type) get0(n);
    }

    /**
     * Sets the type at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param type {@code non-null;} the type to store
     */
    public void set(int n, Type type) {
        set0(n, type);
    }

    /**
     * Returns a new instance, which is the same as this instance,
     * except that it has an additional type prepended to the
     * original.
     *
     * @param type {@code non-null;} the new first element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public StdTypeList withFirst(Type type) {
        int sz = size();
        StdTypeList result = new StdTypeList(sz + 1);

        result.set0(0, type);
        for (int i = 0; i < sz; i++) {
            result.set0(i + 1, getOrNull0(i));
        }

        return result;
    }
}
