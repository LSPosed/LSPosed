/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.lspd.util;

import android.util.Log;

public class Logger {
    private static final String TAG = "LSPosed";

    public static int e(String msg) {
        return Log.e(TAG, msg);
    }

    public static int e(String msg, Throwable e) {
        return Log.e(TAG, msg, e);
    }

    public static int e(Throwable e) {
        return Log.e(TAG, e.getMessage(), e);
    }

    public static int d(String msg) {
        return Log.d(TAG, msg);
    }

    public static int w(String msg) {
        return Log.w(TAG, msg);
    }
}
