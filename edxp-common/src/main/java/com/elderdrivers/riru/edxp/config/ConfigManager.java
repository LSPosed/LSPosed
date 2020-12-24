package com.elderdrivers.riru.edxp.config;

import java.util.HashMap;

import de.robv.android.xposed.SELinuxHelper;

public class ConfigManager {

    public static String appDataDir = "";
    public static String niceName = "";
    public static String appProcessName = "";

    private static final HashMap<String, Boolean> compatModeCache = new HashMap<>();

    public static boolean shouldUseCompatMode(String packageName) {
        Boolean result;
        if (compatModeCache.containsKey(packageName)
                && (result = compatModeCache.get(packageName)) != null) {
            return result;
        }
        result = isFileExists(getConfigPath("compatlist/" + packageName));
        compatModeCache.put(packageName, result);
        return result;
    }

    private static boolean isFileExists(String path) {
        return SELinuxHelper.getAppDataFileService().checkFileExists(path);
    }

    public static native boolean isBlackWhiteListEnabled();

    public static native boolean isNoModuleLogEnabled();

    public static native boolean isResourcesHookEnabled();

    public static native boolean isDeoptBootImageEnabled();

    public static native String getInstallerPackageName();

    public static native String getXposedPropPath();

    public static native String getLibSandHookName();

    public static native String getConfigPath(String suffix);

    public static native String getPrefsPath(String suffix);

    public static native String getCachePath(String suffix);

    public static native String getBaseConfigPath();

    public static native String getDataPathPrefix();

    public static native String getModulesList();
}
