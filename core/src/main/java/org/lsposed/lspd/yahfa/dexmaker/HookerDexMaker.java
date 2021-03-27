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

package org.lsposed.lspd.yahfa.dexmaker;

import org.lsposed.lspd.core.yahfa.HookMain;
import org.lsposed.lspd.nativebridge.Yahfa;
import org.lsposed.lspd.util.Logger;
import org.lsposed.lspd.util.ProxyClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.LspHooker;
import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("rawtypes")
public class HookerDexMaker {

    public static final String METHOD_NAME_BACKUP = "backup";
    public static final String METHOD_NAME_SETUP = "setup";

    private Class<?> mReturnType;
    private Class<?>[] mActualParameterTypes;

    private Executable mMember;
    private XposedBridge.AdditionalHookInfo mHookInfo;
    private ClassLoader mAppClassLoader;
    private LspHooker mHooker;

    private static Class<?>[] getParameterTypes(Executable method, boolean isStatic) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (isStatic) {
            return parameterTypes;
        }
        int parameterSize = parameterTypes.length;
        int targetParameterSize = parameterSize + 1;
        Class<?>[] newParameterTypes = new Class<?>[targetParameterSize];
        int offset = 1;
        newParameterTypes[0] = method.getDeclaringClass();
        System.arraycopy(parameterTypes, 0, newParameterTypes, offset, parameterTypes.length);
        return newParameterTypes;
    }

    public void start(Executable member, XposedBridge.AdditionalHookInfo hookInfo,
                      ClassLoader appClassLoader) throws Exception {
        if (member instanceof Method) {
            Method method = (Method) member;
            mReturnType = method.getReturnType();
            mActualParameterTypes = getParameterTypes(method, Modifier.isStatic(method.getModifiers()));
        } else if (member instanceof Constructor) {
            Constructor constructor = (Constructor) member;
            mReturnType = void.class;
            mActualParameterTypes = getParameterTypes(constructor, false);
        }
        mMember = member;
        mHookInfo = hookInfo;
        if (appClassLoader == null
                || appClassLoader.getClass().getName().equals("java.lang.BootClassLoader")) {
            mAppClassLoader = getClass().getClassLoader();
        } else {
            mAppClassLoader = appClassLoader;
            mAppClassLoader = new ProxyClassLoader(mAppClassLoader, getClass().getClassLoader());
        }

        long startTime = System.nanoTime();
        doMake(member instanceof Constructor ? "constructor" : member.getName());
        long endTime = System.nanoTime();
        Logger.d("Hook time: " + (endTime - startTime) / 1e6 + "ms");
    }

    private void doMake(String methodName) throws Exception {
        Class<?> hookClass = Yahfa.buildHooker(mAppClassLoader, mReturnType, mActualParameterTypes, methodName);
        // Execute our newly-generated code in-process.
        Method backupMethod = hookClass.getMethod(METHOD_NAME_BACKUP, mActualParameterTypes);
        mHooker = new LspHooker(mHookInfo, mMember, backupMethod);
        hookClass.getMethod(METHOD_NAME_SETUP, LspHooker.class).invoke(null, mHooker);
        Method hookMethod = hookClass.getMethod(methodName, mActualParameterTypes);
        HookMain.backupAndHook(mMember, hookMethod, backupMethod);
    }

    public LspHooker getHooker() {
        return mHooker;
    }
}
