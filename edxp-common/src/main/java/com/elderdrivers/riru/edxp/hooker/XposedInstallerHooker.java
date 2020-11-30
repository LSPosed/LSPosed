package com.elderdrivers.riru.edxp.hooker;

import android.app.AndroidAppHelper;
import android.view.View;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XposedInstallerHooker {

    private static final String LEGACY_INSTALLER_PACKAGE_NAME = "de.robv.android.xposed.installer";

    public static void hookXposedInstaller(final ClassLoader classLoader) {
        try {
            final String xposedAppClass = LEGACY_INSTALLER_PACKAGE_NAME + ".XposedApp";
            final Class InstallZipUtil = XposedHelpers.findClass(LEGACY_INSTALLER_PACKAGE_NAME
                    + ".util.InstallZipUtil", classLoader);
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader, "getActiveXposedVersion", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Utils.logD("after getActiveXposedVersion...");
                    param.setResult(XposedBridge.getXposedVersion());
                }
            });
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader,
                    "reloadXposedProp", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Utils.logD("before reloadXposedProp...");
                            final String propFieldName = "mXposedProp";
                            final Object thisObject = param.thisObject;
                            if (thisObject == null) {
                                return;
                            }
                            if (XposedHelpers.getObjectField(thisObject, propFieldName) != null) {
                                param.setResult(null);
                                Utils.logD("reloadXposedProp already done, skip...");
                                return;
                            }
                            File file = new File(ConfigManager.getXposedPropPath());
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
            XposedHelpers.findAndHookMethod("org.meowcat.edxposed.manager.XposedApp", classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "BASE_DIR", ConfigManager.getBaseConfigPath() + "/");
                    XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "ENABLED_MODULES_LIST_FILE", ConfigManager.getConfigPath("enabled_modules.list"));
                }
            });
            XposedHelpers.findAndHookMethod("org.meowcat.edxposed.manager.util.ModuleUtil", classLoader, "updateModulesList", boolean.class, View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObject = param.thisObject;
                    synchronized (thisObject) {
                        XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "MODULES_LIST_FILE", ConfigManager.getConfigPath("modules.list"));
                    }
                }
            });
            XposedHelpers.findAndHookMethod("org.meowcat.edxposed.manager.StatusInstallerFragment", classLoader, "getCanonicalFile", File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File arg = (File)param.args[0];
                    if(arg.equals(new File(AndroidAppHelper.currentApplicationInfo().deviceProtectedDataDir))) {
                        param.args[0] = new File(ConfigManager.getBaseConfigPath());
                    }
                }
            });
        } catch (Throwable t) {
            Utils.logE("Could not hook Xposed Installer", t);
        }
    }
}
