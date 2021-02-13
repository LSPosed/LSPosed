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

package io.github.lsposed.lspd.nativebridge;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.os.Process;

public class Logger {
    static SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss", Locale.getDefault());

    public static native void nativeLog(String str);

    public static void log(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(logDateFormat.format(new Date()));
        sb.append(' ');
        sb.append(Process.myPid());
        sb.append('-');
        sb.append(Process.myTid());
        sb.append('/');
        String processName = ActivityThread.currentProcessName();
        if (processName == null) sb.append("?");
        else sb.append(processName);
        sb.append(' ');
        sb.append("LSPosedBridge: ");
        sb.append(str);
        sb.append('\n');
        nativeLog(sb.toString());
    }
}
