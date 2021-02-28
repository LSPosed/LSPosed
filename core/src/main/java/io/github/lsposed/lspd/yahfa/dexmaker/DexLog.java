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

import android.util.Log;

import io.github.lsposed.lspd.BuildConfig;

public class DexLog {

    public static final String TAG = "LSPosed";

    public static int v(String s) {
        return Log.v(TAG, s);
    }

    public static int i(String s) {
        return Log.i(TAG, s);
    }

    public static int d(String s) {
        if (BuildConfig.DEBUG) {
            return Log.d(TAG, s);
        }
        return 0;
    }

    public static int w(String s) {
        return Log.w(TAG, s);
    }

    public static int e(String s) {
        return Log.e(TAG, s);
    }

    public static int e(String s, Throwable t) {
        return Log.e(TAG, s, t);
    }
}
