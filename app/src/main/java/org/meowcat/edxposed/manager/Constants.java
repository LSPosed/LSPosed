package org.meowcat.edxposed.manager;

import android.util.Log;

public class Constants {
    public static int getXposedApiVersion() {
        try {
            Log.e(App.TAG, "getXposedApiVersion: Xposed is not active");
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getXposedVersion() {
        try {
            Log.e(App.TAG, "getXposedVersion: Xposed is not active");
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static int getXposedVersionCode() {
        try {
            Log.e(App.TAG, "getXposedVersionCode: Xposed is not active");
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getXposedVariant() {
        try {
            Log.e(App.TAG, "getXposedVariant: Xposed is not active");
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getEnabledModulesListFile() {
        try {
            return getBaseDir() + "conf/enabled_modules.list";
        } catch (Exception e) {
            return null;
        }
    }

    public static String getModulesListFile() {
        try {
            return getBaseDir() + "conf/modules.list";
        } catch (Exception e) {
            return null;
        }
    }

    public static String getBaseDir() {
        try {
            return App.getInstance().getApplicationInfo().deviceProtectedDataDir + "/";
        } catch (Exception e) {
            return null;
        }
    }
}
