package com.elderdrivers.riru.edxp.util;

import android.os.Process;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class ProcessUtils {

    // Copied from UserHandle, indicates range of uids allocated for a user.
    public static final int PER_USER_RANGE = 100000;
    public static final int USER_SYSTEM = 0;

    public static String getCurrentProcessName(String prettyName) {
        if (!TextUtils.isEmpty(prettyName)) {
            return prettyName;
        }
        return getProcessName(Process.myPid());
    }

    /**
     * a common solution from https://stackoverflow.com/a/21389402
     */
    public static String getProcessName(int pid) {
        BufferedReader cmdlineReader = null;
        try {
            cmdlineReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(
                            "/proc/" + pid + "/cmdline"),
                    "iso-8859-1"));
            int c;
            StringBuilder processName = new StringBuilder();
            while ((c = cmdlineReader.read()) > 0) {
                processName.append((char) c);
            }
            return processName.toString();
        } catch (Throwable throwable) {
            Utils.logW("getProcessName: " + throwable.getMessage());
        } finally {
            try {
                if (cmdlineReader != null) {
                    cmdlineReader.close();
                }
            } catch (Throwable throwable) {
                Utils.logE("getProcessName: " + throwable.getMessage());
            }
        }
        return "";
    }

    public static boolean isLastPidAlive(File lastPidFile) {
        String lastPidInfo = FileUtils.readLine(lastPidFile);
        try {
            String[] split = lastPidInfo.split(":", 2);
            return checkProcessAlive(Integer.parseInt(split[0]), split[1]);
        } catch (Throwable throwable) {
            Utils.logW("error when check last pid " + lastPidFile + ": " + throwable.getMessage());
            return false;
        }
    }

    public static void saveLastPidInfo(File lastPidFile, int pid, String processName) {
        try {
            if (!lastPidFile.exists()) {
                lastPidFile.getParentFile().mkdirs();
                lastPidFile.createNewFile();
            }
        } catch (Throwable throwable) {
        }
        FileUtils.writeLine(lastPidFile, pid + ":" + processName);
    }

    public static boolean checkProcessAlive(int pid, String processName) {
        String existsPrcName = getProcessName(pid);
        Utils.logW("checking pid alive: " + pid + ", " + processName + ", processName=" + existsPrcName);
        return existsPrcName.equals(processName);
    }
}
