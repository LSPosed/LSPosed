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

package io.github.lsposed.lspd.yahfa.dexmaker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class MethodInfo {

    public String className;
    public String classDesc;
    public String methodName;
    public String methodSig;
    public Method method;
    public Constructor constructor;
    public boolean isConstructor;
    public ClassLoader classLoader;

    public MethodInfo(Member member) {
        if (member instanceof Method) {
            method = (Method) member;
            isConstructor = false;
            classLoader = member.getDeclaringClass().getClassLoader();
            generateMethodInfo();
        } else if (member instanceof Constructor) {
            constructor = (Constructor) member;
            isConstructor = true;
            classLoader = member.getDeclaringClass().getClassLoader();
            generateConstructorInfo();
        } else {
            throw new IllegalArgumentException("member should be Method or Constructor");
        }
    }

    private void generateConstructorInfo() {
        methodName = "<init>";
        className = constructor.getDeclaringClass().getName();
        generateCommonInfo(constructor.getParameterTypes(), void.class);
    }

    private void generateMethodInfo() {
        methodName = method.getName();
        className = method.getDeclaringClass().getName();
        generateCommonInfo(method.getParameterTypes(), method.getReturnType());
    }

    private void generateCommonInfo(Class[] parameterTypes, Class returnType) {
        classDesc = "L" + className.replace(".", "/") + ";";
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (Class parameterType : parameterTypes) {
            builder.append(getDescStr(parameterType));
        }
        builder.append(")");
        builder.append(getDescStr(returnType));
        methodSig = builder.toString();
    }

    public static String getDescStr(Class clazz) {
        if (clazz.equals(boolean.class)) {
            return "Z";
        } else if (clazz.equals(byte.class)) {
            return "B";
        } else if (clazz.equals(char.class)) {
            return "C";
        } else if (clazz.equals(double.class)) {
            return "D";
        } else if (clazz.equals(float.class)) {
            return "F";
        } else if (clazz.equals(int.class)) {
            return "I";
        } else if (clazz.equals(long.class)) {
            return "J";
        } else if (clazz.equals(short.class)) {
            return "S";
        } else if (clazz.equals(void.class)) {
            return "V";
        } else {
            String prefix = clazz.isArray() ? "" : "L";
            String suffix = clazz.isArray() ? "" : ";";
            return prefix + clazz.getName().replace(".", "/") + suffix;
        }
    }

}
