package com.swift.sandhook.utils;

import android.os.Build;
import android.system.Os;
import android.text.TextUtils;

import com.swift.sandhook.HookLog;
import com.swift.sandhook.SandHookConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

    public static final boolean IS_USING_PROTECTED_STORAGE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    /**
     * Delete a file or a directory and its children.
     *
     * @param file The directory to delete.
     * @throws IOException Exception when problem occurs during deleting the directory.
     */
    public static void delete(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                delete(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }

    public static String readLine(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (Throwable throwable) {
            return "";
        }
    }

    public static void writeLine(File file, String line) {
        try {
            file.createNewFile();
        } catch (IOException ex) {
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(line);
            writer.flush();
        } catch (Throwable throwable) {
            HookLog.e("error writing line to file " + file + ": " + throwable.getMessage());
        }
    }

    public static String getPackageName(String dataDir) {
        if (TextUtils.isEmpty(dataDir)) {
            HookLog.e("getPackageName using empty dataDir");
            return "";
        }
        int lastIndex = dataDir.lastIndexOf("/");
        if (lastIndex < 0) {
            return dataDir;
        }
        return dataDir.substring(lastIndex + 1);
    }

    public static void chmod(String path, int mode) throws Exception {
        if (SandHookConfig.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.chmod(path, mode);
                return;
            } catch (Exception e) {
                // ignore
            }
        }

        File file = new File(path);
        String cmd = "chmod ";
        if (file.isDirectory()) {
            cmd += " -R ";
        }
        String cmode = String.format("%o", mode);
        Runtime.getRuntime().exec(cmd + cmode + " " + path).waitFor();
    }

    public interface FileMode {
        int MODE_ISUID = 04000;
        int MODE_ISGID = 02000;
        int MODE_ISVTX = 01000;
        int MODE_IRUSR = 00400;
        int MODE_IWUSR = 00200;
        int MODE_IXUSR = 00100;
        int MODE_IRGRP = 00040;
        int MODE_IWGRP = 00020;
        int MODE_IXGRP = 00010;
        int MODE_IROTH = 00004;
        int MODE_IWOTH = 00002;
        int MODE_IXOTH = 00001;

        int MODE_755 = MODE_IRUSR | MODE_IWUSR | MODE_IXUSR
                | MODE_IRGRP | MODE_IXGRP
                | MODE_IROTH | MODE_IXOTH;
    }

    public static String getDataPathPrefix() {
        return IS_USING_PROTECTED_STORAGE ? "/data/user_de/0/" : "/data/data/";
    }
}
