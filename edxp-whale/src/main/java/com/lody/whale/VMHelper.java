package com.lody.whale;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author Lody
 */
class VMHelper {

    // Holds a mapping from Java type names to native type codes.
    private static final HashMap<Class<?>, String> PRIMITIVE_TO_SIGNATURE;

    static {
        PRIMITIVE_TO_SIGNATURE = new HashMap<>(9);
        PRIMITIVE_TO_SIGNATURE.put(byte.class, "B");
        PRIMITIVE_TO_SIGNATURE.put(char.class, "C");
        PRIMITIVE_TO_SIGNATURE.put(short.class, "S");
        PRIMITIVE_TO_SIGNATURE.put(int.class, "I");
        PRIMITIVE_TO_SIGNATURE.put(long.class, "J");
        PRIMITIVE_TO_SIGNATURE.put(float.class, "F");
        PRIMITIVE_TO_SIGNATURE.put(double.class, "D");
        PRIMITIVE_TO_SIGNATURE.put(void.class, "V");
        PRIMITIVE_TO_SIGNATURE.put(boolean.class, "Z");
    }

    /**
     * Returns the internal name of {@code clazz} (also known as the
     * descriptor).
     */
    private static String getSignature(final Class<?> clazz) {
        final String primitiveSignature = PRIMITIVE_TO_SIGNATURE.get(clazz);
        if (primitiveSignature != null) {
            return primitiveSignature;
        } else if (clazz.isArray()) {
            return "[" + getSignature(clazz.getComponentType());
        } else {
            return "L" + clazz.getName().replace('.', '/') + ";";
        }
    }

    /**
     * Returns the native type codes of {@code clazz}.
     */
    private static String getShortyType(final Class<?> clazz) {
        final String primitiveSignature = PRIMITIVE_TO_SIGNATURE.get(clazz);
        if (primitiveSignature != null) {
            return primitiveSignature;
        }
        return "L";
    }

    // @SuppressWarnings("ConstantConditions")
    private static String getSignature(final Class<?> retType,
                                       final Class<?>[] parameterTypes) {
        final StringBuilder result = new StringBuilder();

        result.append('(');
        for (final Class<?> parameterType : parameterTypes) {
            result.append(getSignature(parameterType));
        }
        result.append(")");
        result.append(getSignature(retType));

        return result.toString();
    }

    private static String getShorty(final Class<?> retType,
                                    final Class<?>[] parameterTypes) {
        final StringBuilder result = new StringBuilder();

        result.append(getShortyType(retType));
        for (final Class<?> parameterType : parameterTypes) {
            result.append(getShortyType(parameterType));
        }

        return result.toString();
    }

    static String getSignature(final Member m) {
        if (m instanceof Method) {
            final Method md = (Method) m;
            return getSignature(md.getReturnType(), md.getParameterTypes());
        }
        if (m instanceof Constructor) {
            final Constructor<?> c = (Constructor<?>) m;
            return getSignature(void.class, c.getParameterTypes());
        }
        return null;
    }

    static String getShorty(final Member m) {
        if (m instanceof Method) {
            final Method md = (Method) m;
            return getShorty(md.getReturnType(), md.getParameterTypes());
        }
        if (m instanceof Constructor) {
            final Constructor<?> c = (Constructor<?>) m;
            return getShorty(void.class, c.getParameterTypes());
        }
        return null;
    }
}
