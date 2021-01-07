package com.elderdrivers.riru.edxp._hooker.impl;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.ContextImpl;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.XResources;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.util.Hookers;
import com.elderdrivers.riru.edxp.util.MetaDataReader;
import com.elderdrivers.riru.edxp.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;

// normal process initialization (for new Activity, Service, BroadcastReceiver etc.)
public class HandleBindApp extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        try {
            Hookers.logD("ActivityThread#handleBindApplication() starts");
            ActivityThread activityThread = (ActivityThread) param.thisObject;
            Object bindData = param.args[0];
            final ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(bindData, "appInfo");
            // save app process name here for later use
            ConfigManager.appProcessName = (String) XposedHelpers.getObjectField(bindData, "processName");
            String reportedPackageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;
            Utils.logD("processName=" + ConfigManager.appProcessName +
                    ", packageName=" + reportedPackageName + ", appDataDir=" + ConfigManager.appDataDir);

            ComponentName instrumentationName = (ComponentName) XposedHelpers.getObjectField(bindData, "instrumentationName");
            if (instrumentationName != null) {
                Hookers.logD("Instrumentation detected, disabling framework for");
                XposedBridge.disableHooks = true;
                return;
            }
            CompatibilityInfo compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(bindData, "compatInfo");
            if (appInfo.sourceDir == null) {
                return;
            }
            XposedHelpers.setObjectField(activityThread, "mBoundApplication", bindData);
            XposedInit.loadedPackagesInProcess.add(reportedPackageName);
            LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);

            XResources.setPackageNameForResDir(appInfo.packageName, loadedApk.getResDir());

            String processName = (String) XposedHelpers.getObjectField(bindData, "processName");


            boolean isModule = false;
            int xposedminversion = -1;
            boolean xposedsharedprefs = false;
            boolean xposedmigrateprefs = false;
            try {
                Map<String, Object> metaData = MetaDataReader.getMetaData(new File(appInfo.sourceDir));
                isModule = metaData.containsKey("xposedmodule");
                if (isModule) {
                    Object minVersionRaw = metaData.get("xposedminversion");
                    if (minVersionRaw instanceof Integer) {
                        xposedminversion = (Integer) minVersionRaw;
                    } else if (minVersionRaw instanceof String) {
                        xposedminversion = MetaDataReader.extractIntPart((String) minVersionRaw);
                    }
                    xposedsharedprefs = metaData.containsKey("xposedsharedprefs");
                    xposedmigrateprefs = metaData.containsKey("xposedmigrateprefs");
                }
            } catch (NumberFormatException | IOException e) {
                Hookers.logE("ApkParser fails", e);
            }

            if (isModule && (xposedminversion > 92 || xposedsharedprefs)) {
                Utils.logW("New modules detected, hook preferences");
                XposedHelpers.findAndHookMethod(ContextImpl.class, "checkMode", int.class, new XC_MethodHook() {
                    @SuppressWarnings("deprecation")
                    @SuppressLint("WorldReadableFiles")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (((int) param.args[0] & Context.MODE_WORLD_READABLE) != 0) {
                            param.setThrowable(null);
                        }
                    }
                });
                final boolean migratePrefs = xposedmigrateprefs;
                XposedHelpers.findAndHookMethod(ContextImpl.class, "getPreferencesDir", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        File newDir = new File(ConfigManager.getPrefsPath(appInfo.packageName));
                        if (migratePrefs) {
                            File oldDir = (File) param.getResult();
                            for (File oldFile : oldDir.listFiles()) {
                                Path oldPath = oldFile.toPath();
                                if (!Files.isSymbolicLink(oldPath)) {
                                    Utils.logD("Migrating prefs file: " + oldFile.getAbsolutePath());
                                    Path newPath = new File(newDir, oldFile.getName()).toPath();
                                    try {
                                        Files.move(oldPath, newPath);
                                        try {
                                            Files.createSymbolicLink(oldPath, newPath);
                                        } catch (IOException e) {
                                            Utils.logD("Symlink creation failed", e);
                                        }
                                    } catch (IOException e) {
                                        Utils.logD("File move operation failed", e);
                                    }
                                }
                            }
                        }
                        param.setResult(newDir);
                    }
                });
            }
            LoadedApkGetCL hook = new LoadedApkGetCL(loadedApk, reportedPackageName,
                    processName, true);
            hook.setUnhook(XposedHelpers.findAndHookMethod(
                    LoadedApk.class, "getClassLoader", hook));

        } catch (Throwable t) {
            Hookers.logE("error when hooking bindApp", t);
        }
    }
}
