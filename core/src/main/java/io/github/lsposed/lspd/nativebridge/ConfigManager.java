package io.github.lsposed.lspd.nativebridge;

public class ConfigManager {

    public static String appDataDir = "";
    public static String niceName = "";
    public static String appProcessName = "";

    public static native boolean isNoModuleLogEnabled();

    public static native boolean isResourcesHookEnabled();

    public static native String getInstallerPackageName();

    public static native String getPrefsPath(String suffix);

    public static native String getCachePath(String suffix);

    public static native String getBaseConfigPath();

    public static native String getDataPathPrefix();

    public static native String getModulesList();

    public static native boolean isPermissive();
}
