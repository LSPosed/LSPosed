package io.github.lsposed.lspd.service;

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
