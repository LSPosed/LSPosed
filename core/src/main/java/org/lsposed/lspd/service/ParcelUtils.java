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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import android.os.Build;
import android.os.Parcel;

import java.lang.reflect.Method;

public class ParcelUtils {

    public static String readInterfaceDescriptor(Parcel parcel) {
        parcel.readInt();
        if (Build.VERSION.SDK_INT >= 29) {
            parcel.readInt();
        }
        if (Build.VERSION.SDK_INT >= 30) {
            parcel.readInt();
        }
        return parcel.readString();
    }

    private static Method obtainMethod;

    public static Parcel fromNativePointer(long ptr) {
        if (ptr == 0) return null;

        if (obtainMethod == null) {
            try {
                //noinspection JavaReflectionMemberAccess
                obtainMethod = Parcel.class.getDeclaredMethod("obtain", long.class);
                obtainMethod.setAccessible(true);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return (Parcel) obtainMethod.invoke(null, ptr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
