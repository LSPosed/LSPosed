package com.elderdrivers.riru.edxp.whale.entry.hooker;

import com.elderdrivers.riru.edxp.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.elderdrivers.riru.edxp.config.InstallerChooser.LEGACY_INSTALLER_PACKAGE_NAME;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(ClassLoader classLoader) {
        try {
            final String xposedAppClass = LEGACY_INSTALLER_PACKAGE_NAME + ".XposedApp";
            final Class InstallZipUtil = XposedHelpers.findClass(LEGACY_INSTALLER_PACKAGE_NAME
                    + ".util.InstallZipUtil", classLoader);
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader, "getActiveXposedVersion",
                    XC_MethodReplacement.returnConstant(XposedBridge.getXposedVersion()));
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader,
                    "reloadXposedProp", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Utils.logD("before reloadXposedProp...");
                            final String propFieldName = "mXposedProp";
                            final Object thisObject = param.thisObject;
                            if (XposedHelpers.getObjectField(thisObject, propFieldName) != null) {
                                param.setResult(null);
                                Utils.logD("reloadXposedProp already done, skip...");
                                return;
                            }
                            File file = new File("/system/framework/edconfig.jar");
                            FileInputStream is = null;
                            try {
                                is = new FileInputStream(file);
                                Object props = XposedHelpers.callStaticMethod(InstallZipUtil,
                                        "parseXposedProp", is);
                                synchronized (thisObject) {
                                    XposedHelpers.setObjectField(thisObject, propFieldName, props);
                                }
                                Utils.logD("reloadXposedProp done...");
                                param.setResult(null);
                            } catch (IOException e) {
                                Utils.logE("Could not read " + file.getPath(), e);
                            } finally {
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            Utils.logE("Could not hook Xposed Installer", t);
        }
    }
}
