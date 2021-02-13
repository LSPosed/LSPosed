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

import io.github.lsposed.lspd.BuildConfig;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;


public class Utils {

    public static final String LOG_TAG = "LSPosed";

    public static void logD(Object msg) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, msg.toString());
    }

    public static void logD(String msg, Throwable throwable) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, msg, throwable);
    }

    public static void logW(String msg) {
        Log.w(LOG_TAG, msg);
    }

    public static void logW(String msg, Throwable throwable) {
        Log.w(LOG_TAG, msg, throwable);
    }

    public static void logI(String msg) {
        Log.i(LOG_TAG, msg);
    }

    public static void logI(String msg, Throwable throwable) {
        Log.i(LOG_TAG, msg, throwable);
    }

    public static void logE(String msg) {
        Log.e(LOG_TAG, msg);
    }

    public static void logE(String msg, Throwable throwable) {
        Log.e(LOG_TAG, msg, throwable);
    }

    @ApiSensitive(Level.LOW)
    public static String getSysProp(String key) {
        try {
            Class sysProps = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            if (sysProps != null) {
                return (String) XposedHelpers.callStaticMethod(sysProps, "get", key);
            }
        } catch (Throwable throwable) {
            Utils.logE("error when get sys prop", throwable);
        }
        return "";
    }
}
