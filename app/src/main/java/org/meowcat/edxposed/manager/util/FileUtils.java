package org.meowcat.edxposed.manager.util;

public class FileUtils {

    public static final int S_IRUSR = 256;
    public static final int S_IWUSR = 128;
    public static final int S_IRGRP = 32;
    public static final int S_IWGRP = 16;
    public static final int S_IROTH = 4;
    public static final int S_IWOTH = 2;

    public static void setPermissions(String dir, int permissions) {
        android.os.FileUtils.setPermissions(dir, permissions, -1, -1);
    }
}
