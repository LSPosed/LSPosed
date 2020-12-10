/*
 * Copyright 2014-2015 Marvin Wißfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swift.sandhook.utils;

import android.util.Log;

import com.swift.sandhook.HookLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
            HookLog.e("get object address error", e);
            return -1;
        }
    }
}
