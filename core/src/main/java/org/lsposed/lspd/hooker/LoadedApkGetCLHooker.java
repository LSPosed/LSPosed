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

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;

import org.lsposed.lspd.impl.LSPosedContext;
import org.lsposed.lspd.util.Hookers;
import org.lsposed.lspd.util.MetaDataReader;
import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModuleInterface;

@SuppressLint("BlockedPrivateApi")
public class LoadedApkGetCLHooker extends XC_MethodHook {
    private final static Field defaultClassLoaderField;

    static {
        Field field = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                field = LoadedApk.class.getDeclaredField("mDefaultClassLoader");
                field.setAccessible(true);
            } catch (Throwable ignored) {
            }
        }
        defaultClassLoaderField = field;
    }

    private final LoadedApk loadedApk;
    private final Unhook unhook;

    public LoadedApkGetCLHooker(LoadedApk loadedApk) {
        this.loadedApk = loadedApk;
        unhook = XposedHelpers.findAndHookMethod(LoadedApk.class, "getClassLoader", this);
    }

    @Override
    protected void afterHookedMethod(MethodHookParam<?> param) {
        LoadedApk loadedApk = (LoadedApk) param.thisObject;

        if (loadedApk != this.loadedApk) {
            return;
        }

        try {
            Hookers.logD("LoadedApk#getClassLoader starts");

            String packageName = ActivityThread.currentPackageName();
            String processName = ActivityThread.currentProcessName();
            boolean isFirstPackage = packageName != null && processName != null && packageName.equals(loadedApk.getPackageName());
            if (!isFirstPackage) {
                packageName = loadedApk.getPackageName();
                processName = ActivityThread.currentPackageName();
            } else if (packageName.equals("android")) {
                packageName = "system";
            }

            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            ClassLoader classLoader = (ClassLoader) param.getResult();
            Hookers.logD("LoadedApk#getClassLoader ends: " + mAppDir + " -> " + classLoader);

            if (classLoader == null) {
                return;
            }

            if (!isFirstPackage && !XposedHelpers.getBooleanField(loadedApk, "mIncludeCode")) {
                Hookers.logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            if (!isFirstPackage && !XposedInit.getLoadedModules().getOrDefault(packageName, Optional.of("")).isPresent()) {
                return;
            }

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                    XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = packageName;
            lpparam.processName = processName;
            lpparam.classLoader = classLoader;
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = isFirstPackage;

            if (isFirstPackage && XposedInit.getLoadedModules().getOrDefault(packageName, Optional.empty()).isPresent()) {
                hookNewXSP(lpparam);
            }

            Hookers.logD("Call handleLoadedPackage: packageName=" + lpparam.packageName + " processName=" + lpparam.processName + " isFirstPackage=" + isFirstPackage + " classLoader=" + lpparam.classLoader + " appInfo=" + lpparam.appInfo);
            XC_LoadPackage.callAll(lpparam);

            LSPosedContext.callOnPackageLoaded(new XposedModuleInterface.PackageLoadedParam() {
                @NonNull
                @Override
                public String getPackageName() {
                    return loadedApk.getPackageName();
                }

                @NonNull
                @Override
                public ApplicationInfo getAppInfo() {
                    return loadedApk.getApplicationInfo();
                }

                @NonNull
                @Override
                public ClassLoader getDefaultClassLoader() {
                    try {
                        return (ClassLoader) defaultClassLoaderField.get(loadedApk);
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @NonNull
                @Override
                public ClassLoader getClassLoader() {
                    return classLoader;
                }

                @Override
                public boolean isFirstPackage() {
                    return isFirstPackage;
                }
            });
        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk#getClassLoader", t);
        } finally {
            unhook.unhook();
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
}
