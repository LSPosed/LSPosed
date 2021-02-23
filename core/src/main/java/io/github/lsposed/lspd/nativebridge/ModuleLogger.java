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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.ActivityThread;
import android.os.ParcelFileDescriptor;
import android.os.Process;

import androidx.annotation.Keep;

import io.github.lsposed.lspd.util.Utils;

@Keep
public class ModuleLogger {
    static SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss", Locale.getDefault());
    static int fd = -1;

    public static void initLogger(ParcelFileDescriptor fileDescriptor) {
        if (fd == -1 && fileDescriptor!= null) {
            fd = fileDescriptor.detachFd();
        }
    }

    private static native void nativeLog(int fd, String logStr);

    public static void log(String str) {
        if (fd == -1) {
            Utils.logE("Logger is not initialized");
            return;
        };
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
        nativeLog(fd, sb.toString());
    }
}
