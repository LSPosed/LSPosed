/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.hooker;

import static org.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;

import android.app.LoadedApk;
import android.os.IBinder;

import org.lsposed.lspd.util.Hookers;
import org.lsposed.lspd.util.InstallerVerifier;
import org.lsposed.lspd.util.MetaDataReader;
import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LoadedApkGetCLHooker extends XC_MethodHook {

    private final LoadedApk loadedApk;
    private final String packageName;
    private final String processName;
    private final boolean isFirstApplication;
    private Unhook unhook;

    public LoadedApkGetCLHooker(LoadedApk loadedApk, String packageName, String processName,
                                boolean isFirstApplication) {
        this.loadedApk = loadedApk;
        this.packageName = packageName;
        this.processName = processName;
        this.isFirstApplication = isFirstApplication;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) {

        try {

            Hookers.logD("LoadedApk#getClassLoader starts");

            LoadedApk loadedApk = (LoadedApk) param.thisObject;

            if (loadedApk != this.loadedApk) {
                return;
            }

            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            ClassLoader classLoader = (ClassLoader) param.getResult();
            Hookers.logD("LoadedApk#getClassLoader ends: " + mAppDir + " -> " + classLoader);

            if (classLoader == null) {
                return;
            }

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                    XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = this.packageName;
            lpparam.processName = this.processName;
            lpparam.classLoader = classLoader;
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = this.isFirstApplication;

            IBinder moduleBinder = serviceClient.requestModuleBinder();
            if (moduleBinder != null) {
                hookNewXSP(lpparam);
            }

            IBinder binder = loadedApk.getApplicationInfo() != null ? serviceClient.requestManagerBinder(loadedApk.getApplicationInfo().packageName) : null;
            if (binder != null) {
                if (InstallerVerifier.verifyInstallerSignature(loadedApk.getApplicationInfo())) {
                    XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader, binder);
                } else {
                    InstallerVerifier.hookXposedInstaller(classLoader);
                }
            } else {
                XC_LoadPackage.callAll(lpparam);
            }

        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk#getClassLoader", t);
        } finally {
            if (unhook != null) {
                unhook.unhook();
            }
        }
    }

    private void hookNewXSP(XC_LoadPackage.LoadPackageParam lpparam) {
        int xposedminversion = -1;
        boolean xposedsharedprefs = false;
        try {
            Map<String, Object> metaData = MetaDataReader.getMetaData(new File(lpparam.appInfo.sourceDir));
            Object minVersionRaw = metaData.get("xposedminversion");
            if (minVersionRaw instanceof Integer) {
                xposedminversion = (Integer) minVersionRaw;
            } else if (minVersionRaw instanceof String) {
                xposedminversion = MetaDataReader.extractIntPart((String) minVersionRaw);
            }
            xposedsharedprefs = metaData.containsKey("xposedsharedprefs");
        } catch (NumberFormatException | IOException e) {
            Hookers.logE("ApkParser fails", e);
        }

        if (xposedminversion > 92 || xposedsharedprefs) {
            Utils.logW("New modules detected, hook preferences");
            XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkMode", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (((int) param.args[0] & 1/*Context.MODE_WORLD_READABLE*/) != 0) {
                        param.setThrowable(null);
                    }
                }
            });
            XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "getPreferencesDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return new File(serviceClient.getPrefsPath(lpparam.packageName));
                }
            });
        }
    }

    public void setUnhook(Unhook unhook) {
        this.unhook = unhook;
    }
}
