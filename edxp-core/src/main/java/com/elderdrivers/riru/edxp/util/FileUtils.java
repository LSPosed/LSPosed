package com.elderdrivers.riru.edxp.util;

import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.ConfigManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

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
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException ignored) {
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(line);
            writer.flush();
        } catch (Throwable throwable) {
            Utils.logE("error writing line to file " + file + ": " + throwable.getMessage());
        }
    }

    public static String getPackageName(String dataDir) {
        if (TextUtils.isEmpty(dataDir)) {
            Utils.logE("getPackageName using empty dataDir");
            return "";
        }
        int lastIndex = dataDir.lastIndexOf("/");
        if (lastIndex < 0) {
            return dataDir;
        }
        return dataDir.substring(lastIndex + 1);
    }

    public static String getDataPathPrefix() {
        return ConfigManager.getDataPathPrefix() + "/";
    }
}
