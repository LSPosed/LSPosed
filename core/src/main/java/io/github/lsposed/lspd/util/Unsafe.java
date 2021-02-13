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
package io.github.lsposed.lspd.util;


import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public final class Unsafe {
    private static final String TAG = "Unsafe";

    private static Object unsafe;
    private static Class unsafeClass;

    private static Method arrayBaseOffsetMethod,
            arrayIndexScaleMethod,
            getIntMethod,
            getLongMethod;

    private volatile static boolean supported = false;

    private static Class objectArrayClass = Object[].class;

    static {
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = theUnsafe.get(null);
        } catch (Exception e) {
            try {
                final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
                theUnsafe.setAccessible(true);
                unsafe = theUnsafe.get(null);
            } catch (Exception e2) {
                Log.w(TAG, "Unsafe not found o.O");
            }
        }
        if (unsafe != null) {
            try {
                arrayBaseOffsetMethod = unsafeClass.getDeclaredMethod("arrayBaseOffset", Class.class);
                arrayIndexScaleMethod = unsafeClass.getDeclaredMethod("arrayIndexScale", Class.class);
                getIntMethod = unsafeClass.getDeclaredMethod("getInt", Object.class, long.class);
                getLongMethod = unsafeClass.getDeclaredMethod("getLong", Object.class, long.class);
                supported = true;
            } catch (Exception e) {
            }
        }
    }

    public static boolean support() {
        return supported;
    }

    private Unsafe() {
    }

    @SuppressWarnings("unchecked")
    public static int arrayBaseOffset(Class cls) {
        try {
            return (int) arrayBaseOffsetMethod.invoke(unsafe, cls);
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static int arrayIndexScale(Class cls) {
        try {
            return (int) arrayIndexScaleMethod.invoke(unsafe, cls);
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static int getInt(Object array, long offset) {
        try {
            return (int) getIntMethod.invoke(unsafe, array, offset);
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static long getLong(Object array, long offset) {
        try {
            return (long) getLongMethod.invoke(unsafe, array, offset);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getObjectAddress(Object obj) {
        try {
            Object[] array = new Object[]{obj};
            if (arrayIndexScale(objectArrayClass) == 8) {
                return getLong(array, arrayBaseOffset(objectArrayClass));
            } else {
                return 0xffffffffL & getInt(array, arrayBaseOffset(objectArrayClass));
            }
        } catch (Exception e) {
            Utils.logE("get object address error", e);
            return -1;
        }
    }
}
