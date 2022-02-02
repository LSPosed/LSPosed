package org.lsposed.lspd.service;

import org.lsposed.lspd.util.SignInfo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CLIValidator {

    private static final Set<Integer> pidSet = ConcurrentHashMap.newKeySet();

    private static String readProcessCmdLine(int pid) throws IOException {
        var inputStream = new FileInputStream("/proc/" + pid + "/cmdline");
        var cmdArr = new BufferedReader(new InputStreamReader(inputStream)).readLine().split("\u0000");
        return String.join(" ", cmdArr);
    }

    private static String getDaemonPath() throws IOException {
        var inputStream = Runtime.getRuntime().exec("magisk --path").getInputStream();
        var modulesRoot = new BufferedReader(new InputStreamReader(inputStream)).readLine();
        return modulesRoot + "/.magisk/modules/" + ConfigManager.getInstance().getApi().toLowerCase() + "_lsposed/daemon.apk";
    }

    // Not safe, this method is easily bypassed
    private static boolean validProcessName(int pid) throws IOException {
        var processCmdline = readProcessCmdLine(pid);
        var daemonPath = getDaemonPath();
        var cmdline = "app_process -Djava.class.path=" + daemonPath + " /system/bin org.lsposed.lspd.cli.Main";
        return processCmdline.startsWith(cmdline);
    }

    public static boolean basicValid(int pid, int uid) {
        // cli must be root
        if (uid != 0) {
            return false;
        }

        try {
            if (!validProcessName(pid)) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean applicationStageNameValid(int pid, String processName) {
        var infoArr = processName.split(":");
        if (infoArr.length != 2) {
            return false;
        }

        if (!infoArr[0].equals("lsp-cli")) {
            return false;
        }

        if(infoArr[1].equals(SignInfo.CLI_UUID)) {
            pidSet.add(pid);
            return true;
        }
        return false;
    }

    public static boolean managerStagePidValid(int pid) {
        return pidSet.contains(pid);
    }

    public static void notifyProcessDie(int pid) {
        pidSet.remove(pid);
    }

}
