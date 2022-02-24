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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MemberUtilsX;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class NewXposedHelpers {
    private static final ConcurrentHashMap<MemberCacheKey.Field, Field> fieldCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey.Method, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MemberCacheKey.Constructor, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    private abstract static class MemberCacheKey {
        private final int hash;

        protected MemberCacheKey(int hash) {
            this.hash = hash;
        }

        @Override
        public abstract boolean equals(@Nullable Object obj);

        @Override
        public int hashCode() {
            return hash;
        }

        static final class Constructor extends MemberCacheKey {
            private final Class<?> clazz;
            private final Class<?>[] parameters;
            private final boolean isExact;

            public Constructor(Class<?> clazz, Class<?>[] parameters, boolean isExact) {
                super(hashCode(clazz, parameters, isExact));
                this.clazz = clazz;
                this.parameters = parameters;
                this.isExact = isExact;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Constructor that = (Constructor) o;
                return isExact == that.isExact && Objects.equals(clazz, that.clazz) && Arrays.equals(parameters, that.parameters);
            }

            @NonNull
            @Override
            public String toString() {
                var str = clazz.getName() + getParametersString(parameters);
                if (isExact) {
                    return str + "#exact";
                } else {
                    return str;
                }
            }

            private static int hashCode(Class<?> clazz, Class<?>[] parameters, boolean isExact) {
                int result = Objects.hash(clazz, isExact);
                result = 31 * result + Arrays.hashCode(parameters);
                return result;
            }
        }

        static final class Field extends MemberCacheKey {
            private final Class<?> clazz;
            private final String name;

            public Field(Class<?> clazz, String name) {
                super(hashCode(clazz, name));
                this.clazz = clazz;
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Field field = (Field) o;
                return Objects.equals(clazz, field.clazz) && Objects.equals(name, field.name);
            }

            @NonNull
            @Override
            public String toString() {
                return clazz.getName() + "#" + name;
            }

            private static int hashCode(Class<?> clazz, String name) {
                return Objects.hash(clazz, name);
            }
        }

        static final class Method extends MemberCacheKey {
            private final Class<?> clazz;
            private final String name;
            private final Class<?>[] parameters;
            private final boolean isExact;

            public Method(Class<?> clazz, String name, Class<?>[] parameters, boolean isExact) {
                super(hashCode(clazz, name, parameters, isExact));
                this.clazz = clazz;
                this.name = name;
                this.parameters = parameters;
                this.isExact = isExact;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Method method = (Method) o;
                return isExact == method.isExact && Objects.equals(clazz, method.clazz) && Objects.equals(name, method.name) && Arrays.equals(parameters, method.parameters);
            }

            @NonNull
            @Override
            public String toString() {
                var str = clazz.getName() + '#' + name + getParametersString(parameters);
                if (isExact) {
                    return str + "#exact";
                } else {
                    return str;
                }
            }

            private static int hashCode(Class<?> clazz, String name, Class<?>[] parameters, boolean isExact) {
                int result = Objects.hash(clazz, name, isExact);
                result = 31 * result + Arrays.hashCode(parameters);
                return result;
            }
        }
    }


    public static Field findField(Class<?> clazz, String fieldName) {
        var key = new MemberCacheKey.Field(clazz, fieldName);

        return fieldCache.computeIfAbsent(key, k -> {
            try {
                Field newField = findFieldRecursiveImpl(clazz, fieldName);
                newField.setAccessible(true);
                return newField;
            } catch (NoSuchFieldException e) {
                throw new NoSuchFieldError(key.toString());
            }
        });
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
        return methodCache.computeIfAbsent(
                new MemberCacheKey.Method(clazz, methodName, parameterTypes, true),
                key -> {
                    try {
                        Method method = key.clazz.getDeclaredMethod(key.name, key.parameters);
                        method.setAccessible(true);
                        return method;
                    } catch (NoSuchMethodException e) {
                        throw new NoSuchMethodError(key.toString());
                    }
                });
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return methodCache.computeIfAbsent(
                new MemberCacheKey.Method(clazz, methodName, parameterTypes, false),
                key -> {
                    // find the exact matching method first
                    try {
                        return findMethodExact(key.clazz, key.name, key.parameters);
                    } catch (NoSuchMethodError ignored) {
                    }

                    // then find the best match
                    Method bestMatch = null;
                    Class<?> clz = key.clazz;
                    boolean considerPrivateMethods = true;
                    do {
                        for (Method method : clz.getDeclaredMethods()) {
                            // don't consider private methods of superclasses
                            if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
                                continue;

                            // compare name and parameters
                            if (method.getName().equals(key.name) && ClassUtils.isAssignable(
                                    key.parameters,
                                    method.getParameterTypes(),
                                    true)) {
                                // get accessible version of method
                                if (bestMatch == null || MemberUtilsX.compareMethodFit(
                                        method,
                                        bestMatch,
                                        key.parameters) < 0) {
                                    bestMatch = method;
                                }
                            }
                        }
                        considerPrivateMethods = false;
                    } while ((clz = clz.getSuperclass()) != null);

                    if (bestMatch != null) {
                        bestMatch.setAccessible(true);
                        return bestMatch;
                    } else {
                        throw new NoSuchMethodError(key.toString());
                    }
                });


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
        return constructorCache.computeIfAbsent(
                new MemberCacheKey.Constructor(clazz, parameterTypes, true),
                key -> {
                    try {
                        Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
                        constructor.setAccessible(true);
                        return constructor;
                    } catch (NoSuchMethodException e) {
                        throw new NoSuchMethodError(key.toString());
                    }
                });
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        return constructorCache.computeIfAbsent(
                new MemberCacheKey.Constructor(clazz, parameterTypes, false),
                key -> {
                    // find the exact matching constructor first
                    try {
                        return findConstructorExact(key.clazz, key.parameters);
                    } catch (NoSuchMethodError ignored) {
                    }

                    // then find the best match
                    Constructor<?> bestMatch = null;
                    Constructor<?>[] constructors = key.clazz.getDeclaredConstructors();
                    for (Constructor<?> constructor : constructors) {
                        // compare name and parameters
                        if (ClassUtils.isAssignable(
                                key.parameters,
                                constructor.getParameterTypes(),
                                true)) {
                            // get accessible version of method
                            if (bestMatch == null || MemberUtilsX.compareConstructorFit(
                                    constructor,
                                    bestMatch,
                                    key.parameters) < 0) {
                                bestMatch = constructor;
                            }
                        }
                    }

                    if (bestMatch != null) {
                        bestMatch.setAccessible(true);
                        return bestMatch;
                    } else {
                        throw new NoSuchMethodError(key.toString());
                    }
                });
    }

}
