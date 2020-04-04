package org.meowcat.edxposed.manager.xposed;
import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static org.meowcat.edxposed.manager.BuildConfig.APPLICATION_ID;

public class Enhancement implements IXposedHookLoadPackage {

    private static final String LEGACY_INSTALLER = "de.robv.android.xposed.installer";

    private static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                XposedBridge.log("Failed to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {
            // Hook PM to pretend to have legacy Xposed Installer installed
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        if (param.args[0].equals(LEGACY_INSTALLER)) {
                            param.args[0] = APPLICATION_ID;
                        }
                    }

                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        if (param.args[0].equals(LEGACY_INSTALLER)) {
                            param.args[0] = APPLICATION_ID;
                        }
                    }
                }
            });
            // Hook AM to remove restrict of EdXposed Manager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appRestrictedInBackgroundLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appServicesRestrictedInBackgroundLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "getAppStartModeLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
            }
        } else if (lpparam.packageName.equals(APPLICATION_ID)) {
            // Make sure Xposed work
            XposedHelpers.findAndHookMethod("org.meowcat.edxposed.manager.StatusInstallerFragment", lpparam.classLoader, "isEnhancementEnabled", XC_MethodReplacement.returnConstant(true));
        }
    }

}