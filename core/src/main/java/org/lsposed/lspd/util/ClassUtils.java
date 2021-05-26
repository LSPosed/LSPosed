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

package org.lsposed.lspd.util;

import android.os.Build;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class ClassUtils {
    private static int getClassStatus(Class<?> clazz, boolean isUnsigned) {
        if (clazz == null) {
            return 0;
        }
        int status = XposedHelpers.getIntField(clazz, "status");
        if (isUnsigned) {
            status = (int) (Integer.toUnsignedLong(status) >> (32 - 4));
        }
        return status;
    }


    /**
     * 5.0-8.0: kInitialized = 10 int
     * 8.1:     kInitialized = 11 int
     * 9.0+:    kInitialized = 14 uint8_t
     * 11.0+:   kInitialized = 14 uint8_t
     *          kVisiblyInitialized = 15 uint8_t
     */
    private static boolean isInitialized(Class<?> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getClassStatus(clazz, true) >= 14;
        } else {
            return getClassStatus(clazz, false) == 11;
        }
    }

    public static boolean shouldDelayHook(Member hookMethod) {
        if (!(hookMethod instanceof Method)) {
            return false;
        }
        Class<?> declaringClass = hookMethod.getDeclaringClass();
        return Modifier.isStatic(hookMethod.getModifiers())
                && !ClassUtils.isInitialized(declaringClass);
    }

}
