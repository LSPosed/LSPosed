/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.org.apache.commons.lang3;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


import external.org.apache.commons.lang3.builder.EqualsBuilder;
import external.org.apache.commons.lang3.builder.HashCodeBuilder;
import external.org.apache.commons.lang3.builder.ToStringBuilder;
import external.org.apache.commons.lang3.builder.ToStringStyle;
import external.org.apache.commons.lang3.mutable.MutableInt;

/**
 * <p>Operations on arrays, primitive arrays (like {@code int[]}) and
 * primitive wrapper arrays (like {@code Integer[]}).</p>
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will not be thrown for a {@code null}
 * array input. However, an Object array that contains a {@code null}
 * element may throw an exception. Each method documents its behaviour.</p>
 *
 * <p>#ThreadSafe#</p>
 * @since 2.0
 * @version $Id: ArrayUtils.java 1154216 2011-08-05 13:57:16Z mbenson $
 */
public class ArrayUtils {

    /**
     * An empty immutable {@code Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    /**
     * An empty immutable {@code Class} array.
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    /**
     * An empty immutable {@code String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /**
     * An empty immutable {@code long} array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    /**
     * An empty immutable {@code Long} array.
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    /**
     * An empty immutable {@code int} array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty immutable {@code Integer} array.
     */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    /**
     * An empty immutable {@code short} array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    /**
     * An empty immutable {@code Short} array.
     */
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
    /**
     * An empty immutable {@code byte} array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * An empty immutable {@code Byte} array.
     */
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
    /**
     * An empty immutable {@code double} array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    /**
     * An empty immutable {@code Double} array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
    /**
     * An empty immutable {@code float} array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    /**
     * An empty immutable {@code Float} array.
     */
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
    /**
     * An empty immutable {@code boolean} array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    /**
     * An empty immutable {@code Boolean} array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
    /**
     * An empty immutable {@code char} array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    /**
     * An empty immutable {@code Character} array.
     */
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];

    /**
     * The index value when an element is not found in a list or array: {@code -1}.
     * This value is returned by methods in this class and can also be used in comparisons with values returned by
     * various method from {@link java.util.List}.
     */
    public static final int INDEX_NOT_FOUND = -1;

    /**
     * <p>ArrayUtils instances should NOT be constructed in standard programming.
     * Instead, the class should be used as <code>ArrayUtils.clone(new int[] {2})</code>.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public ArrayUtils() {
      super();
    }


    // NOTE: Cannot use {@code} to enclose text which includes {}, but <code></code> is OK


    // Basic methods handling multi-dimensional arrays
    //-----------------------------------------------------------------------
    /**
     * <p>Outputs an array as a String, treating {@code null} as an empty array.</p>
     *
     * <p>Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.</p>
     *
     * <p>The format is that of Java source code, for example <code>{a,b}</code>.</p>
     *
     * @param array  the array to get a toString for, may be {@code null}
     * @return a String representation of the array, '{}' if null array input
     */
    public static String toString(Object array) {
        return toString(array, "{}");
    }

    /**
     * <p>Outputs an array as a String handling {@code null}s.</p>
     *
     * <p>Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.</p>
     *
     * <p>The format is that of Java source code, for example <code>{a,b}</code>.</p>
     *
     * @param array  the array to get a toString for, may be {@code null}
     * @param stringIfNull  the String to return if the array is {@code null}
     * @return a String representation of the array
     */
    public static String toString(Object array, String stringIfNull) {
        if (array == null) {
            return stringIfNull;
        }
        return new ToStringBuilder(array, ToStringStyle.SIMPLE_STYLE).append(array).toString();
    }

    /**
     * <p>Get a hash code for an array handling multi-dimensional arrays correctly.</p>
     *
     * <p>Multi-dimensional primitive arrays are also handled correctly by this method.</p>
     *
     * @param array  the array to get a hash code for, {@code null} returns zero
     * @return a hash code for the array
     */
    public static int hashCode(Object array) {
        return new HashCodeBuilder().append(array).toHashCode();
    }

    /**
     * <p>Compares two arrays, using equals(), handling multi-dimensional arrays
     * correctly.</p>
     *
     * <p>Multi-dimensional primitive arrays are also handled correctly by this method.</p>
     *
     * @param array1  the left hand array to compare, may be {@code null}
     * @param array2  the right hand array to compare, may be {@code null}
     * @return {@code true} if the arrays are equal
     */
    public static boolean isEquals(Object array1, Object array2) {
        return new EqualsBuilder().append(array1, array2).isEquals();
    }

    // To map
    //-----------------------------------------------------------------------
    /**
     * <p>Converts the given array into a {@link java.util.Map}. Each element of the array
     * must be either a {@link java.util.Map.Entry} or an Array, containing at least two
     * elements, where the first element is used as key and the second as
     * value.</p>
     *
     * <p>This method can be used to initialize:</p>
     * <pre>
     * // Create a Map mapping colors.
     * Map colorMap = MapUtils.toMap(new String[][] {{
     *     {"RED", "#FF0000"},
     *     {"GREEN", "#00FF00"},
     *     {"BLUE", "#0000FF"}});
     * </pre>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  an array whose elements are either a {@link java.util.Map.Entry} or
     *  an Array containing at least two elements, may be {@code null}
     * @return a {@code Map} that was created from the array
     * @throws IllegalArgumentException  if one element of this Array is
     *  itself an Array containing less then two elements
     * @throws IllegalArgumentException  if the array contains elements other
     *  than {@link java.util.Map.Entry} and an Array
     */
    public static Map<Object, Object> toMap(Object[] array) {
        if (array == null) {
            return null;
        }
        final Map<Object, Object> map = new HashMap<Object, Object>((int) (array.length * 1.5));
        for (int i = 0; i < array.length; i++) {
            Object object = array[i];
            if (object instanceof Map.Entry<?, ?>) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
                map.put(entry.getKey(), entry.getValue());
            } else if (object instanceof Object[]) {
                Object[] entry = (Object[]) object;
                if (entry.length < 2) {
                    throw new IllegalArgumentException("Array element " + i + ", '"
                        + object
                        + "', has a length less than 2");
                }
                map.put(entry[0], entry[1]);
            } else {
                throw new IllegalArgumentException("Array element " + i + ", '"
                        + object
                        + "', is neither of type Map.Entry nor an Array");
            }
        }
        return map;
    }

    // Generic array
    //-----------------------------------------------------------------------
    /**
     * <p>Create a type-safe generic array.</p>
     *
     * <p>The Java language does not allow an array to be created from a generic type:</p>
     *
     * <pre>
    public static &lt;T&gt; T[] createAnArray(int size) {
        return new T[size]; // compiler error here
    }
    public static &lt;T&gt; T[] createAnArray(int size) {
        return (T[])new Object[size]; // ClassCastException at runtime
    }
     * </pre>
     *
     * <p>Therefore new arrays of generic types can be created with this method.
     * For example, an array of Strings can be created:</p>
     *
     * <pre>
    String[] array = ArrayUtils.toArray("1", "2");
    String[] emptyArray = ArrayUtils.&lt;String&gt;toArray();
     * </pre>
     *
     * <p>The method is typically used in scenarios, where the caller itself uses generic types
     * that have to be combined into an array.</p>
     *
     * <p>Note, this method makes only sense to provide arguments of the same type so that the
     * compiler can deduce the type of the array itself. While it is possible to select the
     * type explicitly like in
     * <code>Number[] array = ArrayUtils.&lt;Number&gt;toArray(Integer.valueOf(42), Double.valueOf(Math.PI))</code>,
     * there is no real advantage when compared to
     * <code>new Number[] {Integer.valueOf(42), Double.valueOf(Math.PI)}</code>.</p>
     *
     * @param  <T>   the array's element type
     * @param  items  the varargs array items, null allowed
     * @return the array, not null unless a null array is passed in
     * @since  3.0
     */
    public static <T> T[] toArray(final T... items) {
        return items;
    }

    // Clone
    //-----------------------------------------------------------------------
    /**
     * <p>Shallow clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param <T> the component type of the array
     * @param array  the array to shallow clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static <T> T[] clone(T[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static long[] clone(long[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static int[] clone(int[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static short[] clone(short[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static char[] clone(char[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static byte[] clone(byte[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static double[] clone(double[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static float[] clone(float[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static boolean[] clone(boolean[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    // nullToEmpty
    //-----------------------------------------------------------------------
    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Object[] nullToEmpty(Object[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static String[] nullToEmpty(String[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static long[] nullToEmpty(long[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static int[] nullToEmpty(int[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static short[] nullToEmpty(short[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static char[] nullToEmpty(char[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static byte[] nullToEmpty(byte[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static double[] nullToEmpty(double[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static float[] nullToEmpty(float[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static boolean[] nullToEmpty(boolean[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Long[] nullToEmpty(Long[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Integer[] nullToEmpty(Integer[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Short[] nullToEmpty(Short[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Character[] nullToEmpty(Character[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Byte[] nullToEmpty(Byte[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Double[] nullToEmpty(Double[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Float[] nullToEmpty(Float[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.</p>
     *
     * <p>This method returns an empty array for a {@code null} input array.</p>
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.</p>
     *
     * @param array  the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Boolean[] nullToEmpty(Boolean[] array) {
        if (array == null || array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        return array;
    }

    // Subarrays
    //-----------------------------------------------------------------------
    /**
     * <p>Produces a new array containing the elements between
     * the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * <p>The component type of the subarray is always the same as
     * that of the input array. Thus, if the input is an array of type
     * {@code Date}, the following usage is envisaged:</p>
     *
     * <pre>
     * Date[] someDates = (Date[])ArrayUtils.subarray(allDates, 2, 5);
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static <T> T[] subarray(T[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        Class<?> type = array.getClass().getComponentType();
        if (newSize <= 0) {
            @SuppressWarnings("unchecked") // OK, because array is of type T
            final T[] emptyArray = (T[]) Array.newInstance(type, 0);
            return emptyArray;
        }
        @SuppressWarnings("unchecked") // OK, because array is of type T
        T[] subarray = (T[]) Array.newInstance(type, newSize);
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code long} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static long[] subarray(long[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_LONG_ARRAY;
        }

        long[] subarray = new long[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code int} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static int[] subarray(int[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_INT_ARRAY;
        }

        int[] subarray = new int[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code short} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static short[] subarray(short[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_SHORT_ARRAY;
        }

        short[] subarray = new short[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code char} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static char[] subarray(char[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_CHAR_ARRAY;
        }

        char[] subarray = new char[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code byte} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static byte[] subarray(byte[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] subarray = new byte[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code double} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static double[] subarray(double[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_DOUBLE_ARRAY;
        }

        double[] subarray = new double[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code float} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static float[] subarray(float[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_FLOAT_ARRAY;
        }

        float[] subarray = new float[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code boolean} array containing the elements
     * between the start and end indices.</p>
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.</p>
     *
     * @param array  the array
     * @param startIndexInclusive  the starting index. Undervalue (&lt;0)
     *      is promoted to 0, overvalue (&gt;array.length) results
     *      in an empty array.
     * @param endIndexExclusive  elements up to endIndex-1 are present in the
     *      returned subarray. Undervalue (&lt; startIndex) produces
     *      empty array, overvalue (&gt;array.length) is demoted to
     *      array length.
     * @return a new array containing the elements between
     *      the start and end indices.
     * @since 2.1
     */
    public static boolean[] subarray(boolean[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive > array.length) {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        boolean[] subarray = new boolean[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    // Is same length
    //-----------------------------------------------------------------------
    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.
     *
     * <p>Any multi-dimensional aspects of the arrays are ignored.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(Object[] array1, Object[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(long[] array1, long[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(int[] array1, int[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(short[] array1, short[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(char[] array1, char[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(byte[] array1, byte[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(double[] array1, double[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(float[] array1, float[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    /**
     * <p>Checks whether two arrays are the same length, treating
     * {@code null} arrays as length {@code 0}.</p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating
     *  {@code null} as an empty array
     */
    public static boolean isSameLength(boolean[] array1, boolean[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
            (array2 == null && array1 != null && array1.length > 0) ||
            (array1 != null && array2 != null && array1.length != array2.length)) {
                return false;
        }
        return true;
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Returns the length of the specified array.
     * This method can deal with {@code Object} arrays and with primitive arrays.</p>
     *
     * <p>If the input array is {@code null}, {@code 0} is returned.</p>
     *
     * <pre>
     * ArrayUtils.getLength(null)            = 0
     * ArrayUtils.getLength([])              = 0
     * ArrayUtils.getLength([null])          = 1
     * ArrayUtils.getLength([true, false])   = 2
     * ArrayUtils.getLength([1, 2, 3])       = 3
     * ArrayUtils.getLength(["a", "b", "c"]) = 3
     * </pre>
     *
     * @param array  the array to retrieve the length from, may be null
     * @return The length of the array, or {@code 0} if the array is {@code null}
     * @throws IllegalArgumentException if the object arguement is not an array.
     * @since 2.1
     */
    public static int getLength(Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }

    /**
     * <p>Checks whether two arrays are the same type taking into account
     * multi-dimensional arrays.</p>
     *
     * @param array1 the first array, must not be {@code null}
     * @param array2 the second array, must not be {@code null}
     * @return {@code true} if type of arrays matches
     * @throws IllegalArgumentException if either array is {@code null}
     */
    public static boolean isSameType(Object array1, Object array2) {
        if (array1 == null || array2 == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }
        return array1.getClass().getName().equals(array2.getClass().getName());
    }

    // Reverse
    //-----------------------------------------------------------------------
    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>There is no special handling for multi-dimensional arrays.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(Object[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        Object tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(long[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        long tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(int[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        int tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(short[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        short tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(char[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        char tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(double[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        double tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(float[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        float tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>Reverses the order of the given array.</p>
     *
     * <p>This method does nothing for a {@code null} input array.</p>
     *
     * @param array  the array to reverse, may be {@code null}
     */
    public static void reverse(boolean[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        boolean tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    // IndexOf search
    // ----------------------------------------------------------------------

    // Object IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given object in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param objectToFind  the object to find, may be {@code null}
     * @return the index of the object within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    /**
     * <p>Finds the index of the given object in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param objectToFind  the object to find, may be {@code null}
     * @param startIndex  the index to start searching at
     * @return the index of the object within the array starting at the index,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given object within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param objectToFind  the object to find, may be {@code null}
     * @return the last index of the object within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(Object[] array, Object objectToFind) {
        return lastIndexOf(array, objectToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given object in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than
     * the array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param objectToFind  the object to find, may be {@code null}
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the object within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i >= 0; i--) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i >= 0; i--) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the object is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param objectToFind  the object to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind) != INDEX_NOT_FOUND;
    }

    // long IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(long[] array, long valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(long[] array, long valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // int IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(int[] array, int valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(int[] array, int valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(int[] array, int valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(int[] array, int valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(int[] array, int valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // short IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(short[] array, short valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(short[] array, short valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(short[] array, short valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(short[] array, short valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(short[] array, short valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // char IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int indexOf(char[] array, char valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int indexOf(char[] array, char valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int lastIndexOf(char[] array, char valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int lastIndexOf(char[] array, char valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     * @since 2.1
     */
    public static boolean contains(char[] array, char valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // byte IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(byte[] array, byte valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(byte[] array, byte valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(byte[] array, byte valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(byte[] array, byte valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(byte[] array, byte valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // double IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value within a given tolerance in the array.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, double tolerance) {
        return indexOf(array, valueToFind, 0, tolerance);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, int startIndex, double tolerance) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        double min = valueToFind - tolerance;
        double max = valueToFind + tolerance;
        for (int i = startIndex; i < array.length; i++) {
            if (array[i] >= min && array[i] <= max) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value within a given tolerance in the array.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, double tolerance) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE, tolerance);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @param tolerance  search for value within plus/minus this amount
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, int startIndex, double tolerance) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        double min = valueToFind - tolerance;
        double max = valueToFind + tolerance;
        for (int i = startIndex; i >= 0; i--) {
            if (array[i] >= min && array[i] <= max) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(double[] array, double valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if a value falling within the given tolerance is in the
     * given array.  If the array contains a value within the inclusive range
     * defined by (value - tolerance) to (value + tolerance).</p>
     *
     * <p>The method returns {@code false} if a {@code null} array
     * is passed in.</p>
     *
     * @param array  the array to search
     * @param valueToFind  the value to find
     * @param tolerance  the array contains the tolerance of the search
     * @return true if value falling within tolerance is in array
     */
    public static boolean contains(double[] array, double valueToFind, double tolerance) {
        return indexOf(array, valueToFind, 0, tolerance) != INDEX_NOT_FOUND;
    }

    // float IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(float[] array, float valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(float[] array, float valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(float[] array, float valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(float[] array, float valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(float[] array, float valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // boolean IndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the index of the given value in the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(boolean[] array, boolean valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).</p>
     *
     * @param array  the array to search through for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null}
     *  array input
     */
    public static int indexOf(boolean[] array, boolean valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within the array.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) if
     * {@code null} array input.</p>
     *
     * @param array  the array to travers backwords looking for the object, may be {@code null}
     * @param valueToFind  the object to find
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(boolean[] array, boolean valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.</p>
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.</p>
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than
     * the array length will search from the end of the array.</p>
     *
     * @param array  the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind  the value to find
     * @param startIndex  the start index to travers backwards from
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(boolean[] array, boolean valueToFind, int startIndex) {
        if (ArrayUtils.isEmpty(array)) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            return INDEX_NOT_FOUND;
        } else if (startIndex >= array.length) {
            startIndex = array.length - 1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(boolean[] array, boolean valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    // Primitive/Object array converters
    // ----------------------------------------------------------------------

    // Character array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Characters to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Character} array, may be {@code null}
     * @return a {@code char} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static char[] toPrimitive(Character[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].charValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Character to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Character} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code char} array, {@code null} if null array input
     */
    public static char[] toPrimitive(Character[] array, char valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            Character b = array[i];
            result[i] = (b == null ? valueForNull : b.charValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive chars to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array a {@code char} array
     * @return a {@code Character} array, {@code null} if null array input
     */
    public static Character[] toObject(char[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        final Character[] result = new Character[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Character.valueOf(array[i]);
        }
        return result;
     }

    // Long array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Longs to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Long} array, may be {@code null}
     * @return a {@code long} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static long[] toPrimitive(Long[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].longValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Long to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Long} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code long} array, {@code null} if null array input
     */
    public static long[] toPrimitive(Long[] array, long valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            Long b = array[i];
            result[i] = (b == null ? valueForNull : b.longValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive longs to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code long} array
     * @return a {@code Long} array, {@code null} if null array input
     */
    public static Long[] toObject(long[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        final Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Long.valueOf(array[i]);
        }
        return result;
    }

    // Int array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Integers to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Integer} array, may be {@code null}
     * @return an {@code int} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static int[] toPrimitive(Integer[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].intValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Integer to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Integer} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return an {@code int} array, {@code null} if null array input
     */
    public static int[] toPrimitive(Integer[] array, int valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            Integer b = array[i];
            result[i] = (b == null ? valueForNull : b.intValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive ints to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  an {@code int} array
     * @return an {@code Integer} array, {@code null} if null array input
     */
    public static Integer[] toObject(int[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Integer.valueOf(array[i]);
        }
        return result;
    }

    // Short array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Shorts to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Short} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static short[] toPrimitive(Short[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].shortValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Short to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Short} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static short[] toPrimitive(Short[] array, short valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            Short b = array[i];
            result[i] = (b == null ? valueForNull : b.shortValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive shorts to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code short} array
     * @return a {@code Short} array, {@code null} if null array input
     */
    public static Short[] toObject(short[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        final Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Short.valueOf(array[i]);
        }
        return result;
    }

    // Byte array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Bytes to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Byte} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Bytes to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Byte} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static byte[] toPrimitive(Byte[] array, byte valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            Byte b = array[i];
            result[i] = (b == null ? valueForNull : b.byteValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive bytes to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code byte} array
     * @return a {@code Byte} array, {@code null} if null array input
     */
    public static Byte[] toObject(byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        final Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Byte.valueOf(array[i]);
        }
        return result;
    }

    // Double array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Doubles to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Double} array, may be {@code null}
     * @return a {@code double} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static double[] toPrimitive(Double[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].doubleValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Doubles to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Double} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code double} array, {@code null} if null array input
     */
    public static double[] toPrimitive(Double[] array, double valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            Double b = array[i];
            result[i] = (b == null ? valueForNull : b.doubleValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive doubles to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code double} array
     * @return a {@code Double} array, {@code null} if null array input
     */
    public static Double[] toObject(double[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        final Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Double.valueOf(array[i]);
        }
        return result;
    }

    //   Float array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Floats to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Float} array, may be {@code null}
     * @return a {@code float} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static float[] toPrimitive(Float[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].floatValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Floats to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Float} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code float} array, {@code null} if null array input
     */
    public static float[] toPrimitive(Float[] array, float valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            Float b = array[i];
            result[i] = (b == null ? valueForNull : b.floatValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive floats to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code float} array
     * @return a {@code Float} array, {@code null} if null array input
     */
    public static Float[] toObject(float[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        final Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Float.valueOf(array[i]);
        }
        return result;
    }

    // Boolean array converters
    // ----------------------------------------------------------------------
    /**
     * <p>Converts an array of object Booleans to primitives.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Boolean} array, may be {@code null}
     * @return a {@code boolean} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static boolean[] toPrimitive(Boolean[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].booleanValue();
        }
        return result;
    }

    /**
     * <p>Converts an array of object Booleans to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Boolean} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code boolean} array, {@code null} if null array input
     */
    public static boolean[] toPrimitive(Boolean[] array, boolean valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            Boolean b = array[i];
            result[i] = (b == null ? valueForNull : b.booleanValue());
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive booleans to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code boolean} array
     * @return a {@code Boolean} array, {@code null} if null array input
     */
    public static Boolean[] toObject(boolean[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        final Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (array[i] ? Boolean.TRUE : Boolean.FALSE);
        }
        return result;
    }

    // ----------------------------------------------------------------------
    /**
     * <p>Checks if an array of Objects is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive longs is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(long[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive ints is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive shorts is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(short[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive chars is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(char[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive bytes is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive doubles is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(double[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive floats is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(float[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if an array of primitive booleans is empty or {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is empty or {@code null}
     * @since 2.1
     */
    public static boolean isEmpty(boolean[] array) {
        return array == null || array.length == 0;
    }

    // ----------------------------------------------------------------------
    /**
     * <p>Checks if an array of Objects is not empty or not {@code null}.</p>
     *
     * @param <T> the component type of the array
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
     public static <T> boolean isNotEmpty(T[] array) {
         return (array != null && array.length != 0);
     }

    /**
     * <p>Checks if an array of primitive longs is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(long[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive ints is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(int[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive shorts is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(short[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive chars is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(char[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive bytes is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(byte[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive doubles is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(double[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive floats is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(float[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Checks if an array of primitive booleans is not empty or not {@code null}.</p>
     *
     * @param array  the array to test
     * @return {@code true} if the array is not empty or not {@code null}
     * @since 2.5
     */
    public static boolean isNotEmpty(boolean[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(null, null)     = null
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * ArrayUtils.addAll([null], [null]) = [null, null]
     * ArrayUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array1  the first array whose elements are added to the new array, may be {@code null}
     * @param array2  the second array whose elements are added to the new array, may be {@code null}
     * @return The new array, {@code null} if both arrays are {@code null}.
     *      The type of the new array is the type of the first array,
     *      unless the first array is null, in which case the type is the same as the second array.
     * @since 2.1
     * @throws IllegalArgumentException if the array types are incompatible
     */
    public static <T> T[] addAll(T[] array1, T... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        final Class<?> type1 = array1.getClass().getComponentType();
        @SuppressWarnings("unchecked") // OK, because array is of type T
        T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        try {
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        } catch (ArrayStoreException ase) {
            // Check if problem was due to incompatible types
            /*
             * We do this here, rather than before the copy because:
             * - it would be a wasted check most of the time
             * - safer, in case check turns out to be too strict
             */
            final Class<?> type2 = array2.getClass().getComponentType();
            if (!type1.isAssignableFrom(type2)){
                throw new IllegalArgumentException("Cannot store "+type2.getName()+" in an array of "
                        +type1.getName(), ase);
            }
            throw ase; // No, so rethrow original
        }
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new boolean[] array.
     * @since 2.1
     */
    public static boolean[] addAll(boolean[] array1, boolean... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        boolean[] joinedArray = new boolean[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new char[] array.
     * @since 2.1
     */
    public static char[] addAll(char[] array1, char... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        char[] joinedArray = new char[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new byte[] array.
     * @since 2.1
     */
    public static byte[] addAll(byte[] array1, byte... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new short[] array.
     * @since 2.1
     */
    public static short[] addAll(short[] array1, short... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        short[] joinedArray = new short[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new int[] array.
     * @since 2.1
     */
    public static int[] addAll(int[] array1, int... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        int[] joinedArray = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new long[] array.
     * @since 2.1
     */
    public static long[] addAll(long[] array1, long... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        long[] joinedArray = new long[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new float[] array.
     * @since 2.1
     */
    public static float[] addAll(float[] array1, float... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        float[] joinedArray = new float[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new double[] array.
     * @since 2.1
     */
    public static double[] addAll(double[] array1, double... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        double[] joinedArray = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element, unless the element itself is null,
     *  in which case the return type is Object[]</p>
     *
     * <pre>
     * ArrayUtils.add(null, null)      = [null]
     * ArrayUtils.add(null, "a")       = ["a"]
     * ArrayUtils.add(["a"], null)     = ["a", null]
     * ArrayUtils.add(["a"], "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array to "add" the element to, may be {@code null}
     * @param element  the object to add, may be {@code null}
     * @return A new array containing the existing elements plus the new element
     * The returned array type will be that of the input array (unless null),
     * in which case it will have the same type as the element.
     * If both are null, an IllegalArgumentException is thrown
     * @since 2.1
     * @throws IllegalArgumentException if both arguments are null
     */
    public static <T> T[] add(T[] array, T element) {
        Class<?> type;
        if (array != null){
            type = array.getClass();
        } else if (element != null) {
            type = element.getClass();
        } else {
            throw new IllegalArgumentException("Arguments cannot both be null");
        }
        @SuppressWarnings("unchecked") // type must be T
        T[] newArray = (T[]) copyArrayGrow1(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, true)          = [true]
     * ArrayUtils.add([true], false)       = [true, false]
     * ArrayUtils.add([true, false], true) = [true, false, true]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static boolean[] add(boolean[] array, boolean element) {
        boolean[] newArray = (boolean[])copyArrayGrow1(array, Boolean.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static byte[] add(byte[] array, byte element) {
        byte[] newArray = (byte[])copyArrayGrow1(array, Byte.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, '0')       = ['0']
     * ArrayUtils.add(['1'], '0')      = ['1', '0']
     * ArrayUtils.add(['1', '0'], '1') = ['1', '0', '1']
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static char[] add(char[] array, char element) {
        char[] newArray = (char[])copyArrayGrow1(array, Character.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static double[] add(double[] array, double element) {
        double[] newArray = (double[])copyArrayGrow1(array, Double.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static float[] add(float[] array, float element) {
        float[] newArray = (float[])copyArrayGrow1(array, Float.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static int[] add(int[] array, int element) {
        int[] newArray = (int[])copyArrayGrow1(array, Integer.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static long[] add(long[] array, long element) {
        long[] newArray = (long[])copyArrayGrow1(array, Long.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be {@code null}
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static short[] add(short[] array, short element) {
        short[] newArray = (short[])copyArrayGrow1(array, Short.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * Returns a copy of the given array of size 1 greater than the argument.
     * The last value of the array is left to the default value.
     *
     * @param array The array to copy, must not be {@code null}.
     * @param newArrayComponentType If {@code array} is {@code null}, create a
     * size 1 array of this type.
     * @return A new copy of the array of size 1 greater than the input.
     */
    private static Object copyArrayGrow1(Object array, Class<?> newArrayComponentType) {
        if (array != null) {
            int arrayLength = Array.getLength(array);
            Object newArray = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
            System.arraycopy(array, 0, newArray, 0, arrayLength);
            return newArray;
        }
        return Array.newInstance(newArrayComponentType, 1);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     * @throws IllegalArgumentException if both array and element are null
     */
    public static <T> T[] add(T[] array, int index, T element) {
        Class<?> clss = null;
        if (array != null) {
            clss = array.getClass().getComponentType();
        } else if (element != null) {
            clss = element.getClass();
        } else {
            throw new IllegalArgumentException("Array and element cannot both be null");
        }
        @SuppressWarnings("unchecked") // the add method creates an array of type clss, which is type T
        final T[] newArray = (T[]) add(array, index, element, clss);
        return newArray;
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, true)          = [true]
     * ArrayUtils.add([true], 0, false)       = [false, true]
     * ArrayUtils.add([false], 1, true)       = [false, true]
     * ArrayUtils.add([true, false], 1, true) = [true, true, false]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static boolean[] add(boolean[] array, int index, boolean element) {
        return (boolean[]) add(array, index, Boolean.valueOf(element), Boolean.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, 'a')            = ['a']
     * ArrayUtils.add(['a'], 0, 'b')           = ['b', 'a']
     * ArrayUtils.add(['a', 'b'], 0, 'c')      = ['c', 'a', 'b']
     * ArrayUtils.add(['a', 'b'], 1, 'k')      = ['a', 'k', 'b']
     * ArrayUtils.add(['a', 'b', 'c'], 1, 't') = ['a', 't', 'b', 'c']
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static char[] add(char[] array, int index, char element) {
        return (char[]) add(array, index, Character.valueOf(element), Character.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 3)      = [2, 6, 3]
     * ArrayUtils.add([2, 6], 0, 1)      = [1, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static byte[] add(byte[] array, int index, byte element) {
        return (byte[]) add(array, index, Byte.valueOf(element), Byte.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.add([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static short[] add(short[] array, int index, short element) {
        return (short[]) add(array, index, Short.valueOf(element), Short.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.add([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static int[] add(int[] array, int index, int element) {
        return (int[]) add(array, index, Integer.valueOf(element), Integer.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1L], 0, 2L)           = [2L, 1L]
     * ArrayUtils.add([2L, 6L], 2, 10L)      = [2L, 6L, 10L]
     * ArrayUtils.add([2L, 6L], 0, -4L)      = [-4L, 2L, 6L]
     * ArrayUtils.add([2L, 6L, 3L], 2, 1L)   = [2L, 6L, 1L, 3L]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static long[] add(long[] array, int index, long element) {
        return (long[]) add(array, index, Long.valueOf(element), Long.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1.1f], 0, 2.2f)               = [2.2f, 1.1f]
     * ArrayUtils.add([2.3f, 6.4f], 2, 10.5f)        = [2.3f, 6.4f, 10.5f]
     * ArrayUtils.add([2.6f, 6.7f], 0, -4.8f)        = [-4.8f, 2.6f, 6.7f]
     * ArrayUtils.add([2.9f, 6.0f, 0.3f], 2, 1.0f)   = [2.9f, 6.0f, 1.0f, 0.3f]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static float[] add(float[] array, int index, float element) {
        return (float[]) add(array, index, Float.valueOf(element), Float.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1.1], 0, 2.2)              = [2.2, 1.1]
     * ArrayUtils.add([2.3, 6.4], 2, 10.5)        = [2.3, 6.4, 10.5]
     * ArrayUtils.add([2.6, 6.7], 0, -4.8)        = [-4.8, 2.6, 6.7]
     * ArrayUtils.add([2.9, 6.0, 0.3], 2, 1.0)    = [2.9, 6.0, 1.0, 0.3]
     * </pre>
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static double[] add(double[] array, int index, double element) {
        return (double[]) add(array, index, Double.valueOf(element), Double.TYPE);
    }

    /**
     * Underlying implementation of add(array, index, element) methods.
     * The last parameter is the class, which may not equal element.getClass
     * for primitives.
     *
     * @param array  the array to add the element to, may be {@code null}
     * @param index  the position of the new object
     * @param element  the object to add
     * @param clss the type of the element being added
     * @return A new array containing the existing elements and the new element
     */
    private static Object add(Object array, int index, Object element, Class<?> clss) {
        if (array == null) {
            if (index != 0) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Length: 0");
            }
            Object joinedArray = Array.newInstance(clss, 1);
            Array.set(joinedArray, 0, element);
            return joinedArray;
        }
        int length = Array.getLength(array);
        if (index > length || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }
        Object result = Array.newInstance(clss, length + 1);
        System.arraycopy(array, 0, result, 0, index);
        Array.set(result, index, element);
        if (index < length) {
            System.arraycopy(array, index, result, index + 1, length - index);
        }
        return result;
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove(["a"], 0)           = []
     * ArrayUtils.remove(["a", "b"], 0)      = ["b"]
     * ArrayUtils.remove(["a", "b"], 1)      = ["a"]
     * ArrayUtils.remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    @SuppressWarnings("unchecked") // remove() always creates an array of the same type as its input
    public static <T> T[] remove(T[] array, int index) {
        return (T[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, "a")            = null
     * ArrayUtils.removeElement([], "a")              = []
     * ArrayUtils.removeElement(["a"], "b")           = ["a"]
     * ArrayUtils.removeElement(["a", "b"], "a")      = ["b"]
     * ArrayUtils.removeElement(["a", "b", "a"], "a") = ["b", "a"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static <T> T[] removeElement(T[] array, Object element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static boolean[] remove(boolean[] array, int index) {
        return (boolean[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, true)                = null
     * ArrayUtils.removeElement([], true)                  = []
     * ArrayUtils.removeElement([true], false)             = [true]
     * ArrayUtils.removeElement([true, false], false)      = [true]
     * ArrayUtils.removeElement([true, false, true], true) = [false, true]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static boolean[] removeElement(boolean[] array, boolean element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)          = []
     * ArrayUtils.remove([1, 0], 0)       = [0]
     * ArrayUtils.remove([1, 0], 1)       = [1]
     * ArrayUtils.remove([1, 0, 1], 1)    = [1, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static byte[] remove(byte[] array, int index) {
        return (byte[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)        = null
     * ArrayUtils.removeElement([], 1)          = []
     * ArrayUtils.removeElement([1], 0)         = [1]
     * ArrayUtils.removeElement([1, 0], 0)      = [1]
     * ArrayUtils.removeElement([1, 0, 1], 1)   = [0, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static byte[] removeElement(byte[] array, byte element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove(['a'], 0)           = []
     * ArrayUtils.remove(['a', 'b'], 0)      = ['b']
     * ArrayUtils.remove(['a', 'b'], 1)      = ['a']
     * ArrayUtils.remove(['a', 'b', 'c'], 1) = ['a', 'c']
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static char[] remove(char[] array, int index) {
        return (char[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 'a')            = null
     * ArrayUtils.removeElement([], 'a')              = []
     * ArrayUtils.removeElement(['a'], 'b')           = ['a']
     * ArrayUtils.removeElement(['a', 'b'], 'a')      = ['b']
     * ArrayUtils.removeElement(['a', 'b', 'a'], 'a') = ['b', 'a']
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static char[] removeElement(char[] array, char element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static double[] remove(double[] array, int index) {
        return (double[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static double[] removeElement(double[] array, double element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static float[] remove(float[] array, int index) {
        return (float[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static float[] removeElement(float[] array, float element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static int[] remove(int[] array, int index) {
        return (int[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static int[] removeElement(int[] array, int element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static long[] remove(long[] array, int index) {
        return (long[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static long[] removeElement(long[] array, long element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static short[] remove(short[] array, int index) {
        return (short[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static short[] removeElement(short[] array, short element) {
        int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * @param array  the array to remove the element from, may not be {@code null}
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    private static Object remove(Object array, int index) {
        int length = getLength(array);
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }

        Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll(["a", "b", "c"], 0, 2) = ["b"]
     * ArrayUtils.removeAll(["a", "b", "c"], 1, 2) = ["a"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    @SuppressWarnings("unchecked")
    // removeAll() always creates an array of the same type as its input
    public static <T> T[] removeAll(T[] array, int... indices) {
        return (T[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, "a", "b")            = null
     * ArrayUtils.removeElements([], "a", "b")              = []
     * ArrayUtils.removeElements(["a"], "b", "c")           = ["a"]
     * ArrayUtils.removeElements(["a", "b"], "a", "c")      = ["b"]
     * ArrayUtils.removeElements(["a", "b", "a"], "a")      = ["b", "a"]
     * ArrayUtils.removeElements(["a", "b", "a"], "a", "a") = ["b"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static <T> T[] removeElements(T[] array, T... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<T, MutableInt> occurrences = new HashMap<T, MutableInt>(values.length);
        for (T v : values) {
            MutableInt count = occurrences.get(v);
            if (count == null) {
                occurrences.put(v, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<T, MutableInt> e : occurrences.entrySet()) {
            T v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v, found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static byte[] removeAll(byte[] array, int... indices) {
        return (byte[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static byte[] removeElements(byte[] array, byte... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Byte, MutableInt> occurrences = new HashMap<Byte, MutableInt>(values.length);
        for (byte v : values) {
            Byte boxed = Byte.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Byte, MutableInt> e : occurrences.entrySet()) {
            Byte v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.byteValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static short[] removeAll(short[] array, int... indices) {
        return (short[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static short[] removeElements(short[] array, short... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Short, MutableInt> occurrences = new HashMap<Short, MutableInt>(values.length);
        for (short v : values) {
            Short boxed = Short.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Short, MutableInt> e : occurrences.entrySet()) {
            Short v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.shortValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static int[] removeAll(int[] array, int... indices) {
        return (int[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static int[] removeElements(int[] array, int... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Integer, MutableInt> occurrences = new HashMap<Integer, MutableInt>(values.length);
        for (int v : values) {
            Integer boxed = Integer.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Integer, MutableInt> e : occurrences.entrySet()) {
            Integer v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.intValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static char[] removeAll(char[] array, int... indices) {
        return (char[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static char[] removeElements(char[] array, char... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Character, MutableInt> occurrences = new HashMap<Character, MutableInt>(values.length);
        for (char v : values) {
            Character boxed = Character.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Character, MutableInt> e : occurrences.entrySet()) {
            Character v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.charValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static long[] removeAll(long[] array, int... indices) {
        return (long[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static long[] removeElements(long[] array, long... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Long, MutableInt> occurrences = new HashMap<Long, MutableInt>(values.length);
        for (long v : values) {
            Long boxed = Long.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Long, MutableInt> e : occurrences.entrySet()) {
            Long v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.longValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static float[] removeAll(float[] array, int... indices) {
        return (float[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static float[] removeElements(float[] array, float... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Float, MutableInt> occurrences = new HashMap<Float, MutableInt>(values.length);
        for (float v : values) {
            Float boxed = Float.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Float, MutableInt> e : occurrences.entrySet()) {
            Float v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.floatValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static double[] removeAll(double[] array, int... indices) {
        return (double[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static double[] removeElements(double[] array, double... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Double, MutableInt> occurrences = new HashMap<Double, MutableInt>(values.length);
        for (double v : values) {
            Double boxed = Double.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Double, MutableInt> e : occurrences.entrySet()) {
            Double v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.doubleValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.removeAll([true, false, true], 0, 2) = [false]
     * ArrayUtils.removeAll([true, false, true], 1, 2) = [true]
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     *         at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     * (index < 0 || index >= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static boolean[] removeAll(boolean[] array, int... indices) {
        return (boolean[]) removeAll((Object) array, clone(indices));
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.</p>
     *
     * <pre>
     * ArrayUtils.removeElements(null, true, false)               = null
     * ArrayUtils.removeElements([], true, false)                 = []
     * ArrayUtils.removeElements([true], false, false)            = [true]
     * ArrayUtils.removeElements([true, false], true, true)       = [false]
     * ArrayUtils.removeElements([true, false, true], true)       = [false, true]
     * ArrayUtils.removeElements([true, false, true], true, true) = [false]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     *         earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static boolean[] removeElements(boolean[] array, boolean... values) {
        if (isEmpty(array) || isEmpty(values)) {
            return clone(array);
        }
        HashMap<Boolean, MutableInt> occurrences = new HashMap<Boolean, MutableInt>(values.length);
        for (boolean v : values) {
            Boolean boxed = Boolean.valueOf(v);
            MutableInt count = occurrences.get(boxed);
            if (count == null) {
                occurrences.put(boxed, new MutableInt(1));
            } else {
                count.increment();
            }
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (Map.Entry<Boolean, MutableInt> e : occurrences.entrySet()) {
            Boolean v = e.getKey();
            int found = 0;
            for (int i = 0, ct = e.getValue().intValue(); i < ct; i++) {
                found = indexOf(array, v.booleanValue(), found);
                if (found < 0) {
                    break;
                }
                toRemove.add(found++);
            }
        }
        return removeAll(array, extractIndices(toRemove));
    }

    /**
     * Removes multiple array elements specified by index.
     * @param array source
     * @param indices to remove, WILL BE SORTED--so only clones of user-owned arrays!
     * @return new array of same type minus elements specified by unique values of {@code indices}
     * @since 3.0.1
     */
    private static Object removeAll(Object array, int... indices) {
        int length = getLength(array);
        int diff = 0;

        if (isNotEmpty(indices)) {
            Arrays.sort(indices);

            int i = indices.length;
            int prevIndex = length;
            while (--i >= 0) {
                int index = indices[i];
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
                }
                if (index >= prevIndex) {
                    continue;
                }
                diff++;
                prevIndex = index;
            }
        }
        Object result = Array.newInstance(array.getClass().getComponentType(), length - diff);
        if (diff < length) {
            int end = length;
            int dest = length - diff;
            for (int i = indices.length - 1; i >= 0; i--) {
                int index = indices[i];
                if (end - index > 1) {
                    int cp = end - index - 1;
                    dest -= cp;
                    System.arraycopy(array, index + 1, result, dest, cp);
                }
                end = index;
            }
            if (end > 0) {
                System.arraycopy(array, 0, result, 0, end);
            }
        }
        return result;
    }

    /**
     * Extract a set of Integer indices into an int[].
     * @param coll {@code HashSet} of {@code Integer}
     * @return int[]
     * @since 3.0.1
     */
    private static int[] extractIndices(HashSet<Integer> coll) {
        int[] result = new int[coll.size()];
        int i = 0;
        for (Integer index : coll) {
            result[i++] = index.intValue();
        }
        return result;
    }
}
