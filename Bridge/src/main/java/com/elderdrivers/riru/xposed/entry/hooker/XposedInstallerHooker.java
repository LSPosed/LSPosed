package com.elderdrivers.riru.xposed.entry.hooker;

import com.elderdrivers.riru.xposed.util.Utils;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedInit.INSTALLER_LEGACY_PACKAGE_NAME;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(ClassLoader classLoader) {
        try {
            final String xposedAppClass = INSTALLER_LEGACY_PACKAGE_NAME + ".XposedApp";
            findAndHookMethod(xposedAppClass, classLoader, "getActiveXposedVersion",
                    XC_MethodReplacement.returnConstant(XposedBridge.getXposedVersion()));
        } catch (Throwable t) {
            Utils.logE("Could not hook Xposed Installer", t);
        }
    }
}
