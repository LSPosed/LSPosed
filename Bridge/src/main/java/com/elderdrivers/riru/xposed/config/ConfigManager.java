package com.elderdrivers.riru.xposed.config;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.SELinuxHelper;
import de.robv.android.xposed.XposedInit;

import static de.robv.android.xposed.XposedInit.INSTALLER_PACKAGE_NAME;

public class ConfigManager {

    private static final String BLACK_LIST_PATH = XposedInit.INSTALLER_DATA_BASE_DIR + "conf/blacklist/";
    private static final String WHITE_LIST_PATH = XposedInit.INSTALLER_DATA_BASE_DIR + "conf/whitelist/";
    private static final String COMPAT_LIST_PATH = XposedInit.INSTALLER_DATA_BASE_DIR + "conf/compatlist/";
    private static final String USE_WHITE_LIST = XposedInit.INSTALLER_DATA_BASE_DIR + "conf/usewhitelist";
    private static final String DYNAMIC_MODULES = XposedInit.INSTALLER_DATA_BASE_DIR + "conf/dynamicmodules";
    private static final Set<String> WHITE_LIST = Collections.singleton(INSTALLER_PACKAGE_NAME);
    private static final boolean IS_DYNAMIC_MODULES;

    static {
        IS_DYNAMIC_MODULES = isFileExists(DYNAMIC_MODULES);
    }

    public static boolean isDynamicModulesMode() {
        return IS_DYNAMIC_MODULES;
    }

    public static boolean shouldUseWhitelist() {
        return isFileExists(USE_WHITE_LIST);
    }

    public static boolean shouldUseCompatMode(String packageName) {
        return isFileExists(COMPAT_LIST_PATH + packageName);
    }

    public static boolean shouldHook(String packageName) {
        if (WHITE_LIST.contains(packageName)) {
            return true;
        }
        if (shouldUseWhitelist()) {
            return isFileExists(WHITE_LIST_PATH + packageName);
        } else {
            return !isFileExists(BLACK_LIST_PATH + packageName);
        }
    }

    private static boolean isFileExists(String path) {
        return SELinuxHelper.getAppDataFileService().checkFileExists(path);
    }
}
