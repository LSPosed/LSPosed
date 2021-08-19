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

import android.app.ActivityThread;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemProperties;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ModuleLogger {
    private static DateTimeFormatter logDateFormat;
    private static ParcelFileDescriptor fd = null;

    public static void initLogger(ParcelFileDescriptor fileDescriptor) {
        if (fd == null && fileDescriptor != null) {
            fd = fileDescriptor;
            var zone = ZoneId.of(SystemProperties.get("persist.sys.timezone", "GMT"));
            logDateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zone);
        }
    }

    public static void log(String str, boolean isThrowable) {
        if (fd == null) {
            Utils.logE("Logger is not initialized");
            return;
        }
        StringBuilder sb = new StringBuilder();
        String processName = ActivityThread.currentProcessName();

        sb.append(logDateFormat.format(Instant.now()));
        sb.append(' ');
        sb.append(isThrowable ? "E" : "I");
        sb.append('/');
        sb.append(processName == null ? "android" : processName);
        sb.append('(');
        sb.append(Process.myPid());
        sb.append('-');
        sb.append(Process.myTid());
        sb.append(')');
        sb.append(": ");
        sb.append(str);
        sb.append('\n');
        try {
            var log = sb.toString();
            var writer = new FileWriter(fd.getFileDescriptor());
            writer.write(log, 0, log.length());
            writer.flush();
        } catch (IOException e) {
            Utils.logE("Unable to write to module log file", e);
        }
    }
}
