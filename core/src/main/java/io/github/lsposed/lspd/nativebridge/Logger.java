package io.github.lsposed.lspd.nativebridge;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        try {
            sb.append((String) Class.forName("android.app.ActivityThread").getDeclaredMethod("currentProcessName").invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            sb.append("?");
        }
        sb.append(' ');
        sb.append("LSPosedBridge: ");
        sb.append(str);
        sb.append('\n');
        nativeLog(sb.toString());
    }
}
