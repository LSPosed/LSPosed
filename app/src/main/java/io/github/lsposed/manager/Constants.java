package io.github.lsposed.manager;

import android.widget.Toast;

public class Constants {
    public static int getXposedApiVersion() {
        try {
            return -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static String getXposedVersion() {
        try {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getXposedVersionCode() {
        try {
            return -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static String getXposedVariant() {
        try {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getEnabledModulesListFile() {
        return getBaseDir() + "conf/enabled_modules.list";
    }

    public static String getModulesListFile() {
        return getBaseDir() + "conf/modules.list";
    }

    public static String getConfDir() {
        return getBaseDir() + "conf/";
    }

    public static String getBaseDir() {
        try {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getLogDir() {
        try {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getMiscDir() {
        try {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isPermissive() {
        try {
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void showErrorToast(int type) {
        Toast.makeText(App.getInstance(), R.string.app_destroyed, Toast.LENGTH_LONG).show();
    }
}
