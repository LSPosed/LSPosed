package io.github.lsposed.lspd.hooker;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.github.lsposed.lspd.BuildConfig;
import io.github.lsposed.lspd.config.ConfigManager;
import io.github.lsposed.lspd.config.LSPdConfigGlobal;
import io.github.lsposed.lspd.core.EdxpImpl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.util.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(final ClassLoader classLoader) {
        String variant_ = "None";
        switch (Main.getEdxpVariant()) {
            case EdxpImpl.YAHFA:
                variant_ = "YAHFA";
                break;
            case EdxpImpl.SANDHOOK:
                variant_ = "SandHook";
                break;
            case EdxpImpl.NONE:
            default:
                variant_ = "Unknown";
                break;
        }
        final String variant = variant_;

        // LSPosed Manager R
        try {
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getXposedApiVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return XposedBridge.getXposedVersion();
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getXposedVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return BuildConfig.VERSION_NAME;
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getXposedVersionCode", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return BuildConfig.VERSION_CODE;
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getXposedApiVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return XposedBridge.getXposedVersion();
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getXposedVariant", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return variant;
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.Constants", classLoader, "getBaseDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return ConfigManager.getBaseConfigPath() + "/";
                }
            });
            Utils.logD("Hooked LSPosed Manager R");
            return;
        } catch (Throwable t) {
            Utils.logW("Could not hook LSPosed Manager R", t);
        }

        // LSPosed Manager and Xposed Installer
        try {
            final String xposedAppClass = "de.robv.android.xposed.installer.XposedApp";
            final Class<?> InstallZipUtil = XposedHelpers.findClass("de.robv.android.xposed.installer.util.InstallZipUtil", classLoader);
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader, "getActiveXposedVersion",
                    XC_MethodReplacement.returnConstant(XposedBridge.getXposedVersion())
            );
            XposedHelpers.findAndHookMethod(xposedAppClass, classLoader,
                    "reloadXposedProp", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
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
                            //version=92.0-$version ($backend)
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("version=");
                            stringBuilder.append(XposedBridge.getXposedVersion());
                            stringBuilder.append(".0-");
                            stringBuilder.append(BuildConfig.VERSION_NAME);
                            stringBuilder.append(" (");
                            String variant = "None";
                            stringBuilder.append(variant);
                            stringBuilder.append(")");
                            try (ByteArrayInputStream is = new ByteArrayInputStream(stringBuilder.toString().getBytes())) {
                                Object props = XposedHelpers.callStaticMethod(InstallZipUtil,
                                        "parseXposedProp", is);
                                synchronized (thisObject) {
                                    XposedHelpers.setObjectField(thisObject, propFieldName, props);
                                }
                                Utils.logD("reloadXposedProp done...");
                                param.setResult(null);
                            } catch (IOException e) {
                                Utils.logE("Could not reloadXposedProp", e);
                            }
                        }
                    });
        } catch (Throwable t) {
            Utils.logW("Could not hook Xposed Installer or LSPosed Manager", t);
            return;
        }

        // LSPosed Manager
        deoptMethod(classLoader, "io.github.lsposed.manager.ModulesFragment", "onActivityCreated", Bundle.class);
        deoptMethod(classLoader, "io.github.lsposed.manager.ModulesFragment", "showMenu", Context.class, View.class, ApplicationInfo.class);
        deoptMethod(classLoader, "io.github.lsposed.manager.StatusInstallerFragment", "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class);
        deoptMethod(classLoader, "io.github.lsposed.manager.util.ModuleUtil", "updateModulesList", boolean.class, View.class);
        try {
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.XposedApp", classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "BASE_DIR", ConfigManager.getBaseConfigPath() + "/");
                    XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "ENABLED_MODULES_LIST_FILE", ConfigManager.getConfigPath("enabled_modules.list"));
                }
            });
            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.util.ModuleUtil", classLoader, "updateModulesList", boolean.class, View.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObject = param.thisObject;
                    synchronized (thisObject) {
                        XposedHelpers.setStaticObjectField(param.thisObject.getClass(), "MODULES_LIST_FILE", ConfigManager.getConfigPath("modules.list"));
                    }
                }
            });

            XposedHelpers.findAndHookMethod("io.github.lsposed.manager.StatusInstallerFragment", classLoader, "getCanonicalFile", File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File arg = (File) param.args[0];
                    if (arg.equals(new File(AndroidAppHelper.currentApplicationInfo().deviceProtectedDataDir))) {
                        param.args[0] = new File(ConfigManager.getBaseConfigPath());
                    }
                }
            });
            Utils.logD("Hooked LSPosed Manager");
        } catch (Throwable t) {
            Utils.logD("Hooked Xposed Installer");
        }
    }

    private static void deoptMethod(ClassLoader cl, String className, String methodName, Class<?>... params) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, cl);
            if (clazz == null) {
                Utils.logE("Class " + className + " not found when deoptimizing LSPosed Manager");
                return;
            }

            Object method = XposedHelpers.findMethodExact(clazz, methodName, params);
            LSPdConfigGlobal.getHookProvider().deoptMethodNative(method);
        } catch (Throwable t) {
            Utils.logE("Error when deoptimizing " + className + ":" + methodName, t);
        }

    }
}
