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

package org.lsposed.lspd.nativebridge;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

public class Yahfa {

    public static native boolean backupAndHookNative(Executable target, Method hook, Method backup);

    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Executable findMethodNative(Class<?> targetClass, String methodName, String methodSig);

    public static native void init(int sdkVersion);

    public static native void recordHooked(Executable member);

    public static native boolean isHooked(Executable member);

    public static native Class<?> buildHooker(ClassLoader appClassLoader, char returnType, char[] params, String methodName);
}
