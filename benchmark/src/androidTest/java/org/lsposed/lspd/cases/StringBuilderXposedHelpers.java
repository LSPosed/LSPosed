/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.cases;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MemberUtilsX;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

@SuppressWarnings("StringBufferReplaceableByString")
public final class StringBuilderXposedHelpers {
    private static final HashMap<String, Field> fieldCache = new HashMap<>();
    private static final HashMap<String, Method> methodCache = new HashMap<>();
    private static final HashMap<String, Constructor<?>> constructorCache = new HashMap<>();

    public static Field findField(Class<?> clazz, String fieldName) {
        var clazzString = clazz.getName();
        String fullFieldName = new StringBuilder(clazzString.length() + fieldName.length() + 1)
                .append(clazzString)
                .append('#')
                .append(fieldName)
                .toString();

        if (fieldCache.containsKey(fullFieldName)) {
            Field field = fieldCache.get(fullFieldName);
            if (field == null)
                throw new NoSuchFieldError(fullFieldName);
            return field;
        }

        try {
            Field field = findFieldRecursiveImpl(clazz, fieldName);
            field.setAccessible(true);
            fieldCache.put(fullFieldName, field);
            return field;
        } catch (NoSuchFieldException e) {
            fieldCache.put(fullFieldName, null);
            throw new NoSuchFieldError(fullFieldName);
        }
    }

    private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            while (true) {
                clazz = clazz.getSuperclass();
                if (clazz == null || clazz.equals(Object.class))
                    break;

                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw e;
        }
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        var clazzString = clazz.getName();
        var paramsString = getParametersString(parameterTypes);
        String fullMethodName = new StringBuilder(clazzString.length() + methodName.length() + paramsString.length() + 7)
                .append(clazzString)
                .append('#')
                .append(methodName)
                .append(paramsString)
                .append("#exact")
                .toString();

        if (methodCache.containsKey(fullMethodName)) {
            Method method = methodCache.get(fullMethodName);
            if (method == null)
                throw new NoSuchMethodError(fullMethodName);
            return method;
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            methodCache.put(fullMethodName, method);
            return method;
        } catch (NoSuchMethodException e) {
            methodCache.put(fullMethodName, null);
            throw new NoSuchMethodError(fullMethodName);
        }
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        var clazzString = clazz.getName();
        var paramsString = getParametersString(parameterTypes);
        String fullMethodName = new StringBuilder(clazzString.length() + methodName.length() + paramsString.length() + 11)
                .append(clazzString)
                .append('#')
                .append(methodName)
                .append(paramsString)
                .append("#bestmatch")
                .toString();

        if (methodCache.containsKey(fullMethodName)) {
            Method method = methodCache.get(fullMethodName);
            if (method == null)
                throw new NoSuchMethodError(fullMethodName);
            return method;
        }

        try {
            Method method = findMethodExact(clazz, methodName, parameterTypes);
            methodCache.put(fullMethodName, method);
            return method;
        } catch (NoSuchMethodError ignored) {
        }

        Method bestMatch = null;
        Class<?> clz = clazz;
        boolean considerPrivateMethods = true;
        do {
            for (Method method : clz.getDeclaredMethods()) {
                // don't consider private methods of superclasses
                if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
                    continue;

                // compare name and parameters
                if (method.getName().equals(methodName) && ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true)) {
                    // get accessible version of method
                    if (bestMatch == null || MemberUtilsX.compareMethodFit(
                            method,
                            bestMatch,
                            parameterTypes) < 0) {
                        bestMatch = method;
                    }
                }
            }
            considerPrivateMethods = false;
        } while ((clz = clz.getSuperclass()) != null);

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            methodCache.put(fullMethodName, bestMatch);
            return bestMatch;
        } else {
            NoSuchMethodError e = new NoSuchMethodError(fullMethodName);
            methodCache.put(fullMethodName, null);
            throw e;
        }
    }

    private static String getParametersString(Class<?>... clazzes) {
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Class<?> clazz : clazzes) {
            if (first)
                first = false;
            else
                sb.append(",");

            if (clazz != null)
                sb.append(clazz.getCanonicalName());
            else
                sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        var clazzString = clazz.getName();
        var paramsString = getParametersString(parameterTypes);
        String fullConstructorName = new StringBuilder(clazzString.length() + paramsString.length() + 6)
                .append(clazzString)
                .append(paramsString)
                .append("#exact")
                .toString();

        if (constructorCache.containsKey(fullConstructorName)) {
            Constructor<?> constructor = constructorCache.get(fullConstructorName);
            if (constructor == null)
                throw new NoSuchMethodError(fullConstructorName);
            return constructor;
        }

        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            constructorCache.put(fullConstructorName, constructor);
            return constructor;
        } catch (NoSuchMethodException e) {
            constructorCache.put(fullConstructorName, null);
            throw new NoSuchMethodError(fullConstructorName);
        }
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        var clazzString = clazz.getName();
        var paramsString = getParametersString(parameterTypes);
        String fullConstructorName = new StringBuilder(clazzString.length() + paramsString.length() + 10)
                .append(clazzString)
                .append(paramsString)
                .append("#bestmatch")
                .toString();

        if (constructorCache.containsKey(fullConstructorName)) {
            Constructor<?> constructor = constructorCache.get(fullConstructorName);
            if (constructor == null)
                throw new NoSuchMethodError(fullConstructorName);
            return constructor;
        }

        try {
            Constructor<?> constructor = findConstructorExact(clazz, parameterTypes);
            constructorCache.put(fullConstructorName, constructor);
            return constructor;
        } catch (NoSuchMethodError ignored) {
        }

        Constructor<?> bestMatch = null;
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            // compare name and parameters
            if (ClassUtils.isAssignable(parameterTypes, constructor.getParameterTypes(), true)) {
                // get accessible version of method
                if (bestMatch == null || MemberUtilsX.compareConstructorFit(
                        constructor,
                        bestMatch,
                        parameterTypes) < 0) {
                    bestMatch = constructor;
                }
            }
        }

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            constructorCache.put(fullConstructorName, bestMatch);
            return bestMatch;
        } else {
            NoSuchMethodError e = new NoSuchMethodError(fullConstructorName);
            constructorCache.put(fullConstructorName, null);
            throw e;
        }
    }

}
