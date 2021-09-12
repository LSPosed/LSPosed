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

import static org.lsposed.lspd.config.ApplicationServiceClient.serviceClient;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Instrumentation;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.XResources;
import android.os.IBinder;
import android.util.AndroidRuntimeException;
import android.webkit.WebViewDelegate;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;

import org.lsposed.lspd.service.ActivityController;
import org.lsposed.lspd.service.BridgeService;
import org.lsposed.lspd.service.PackageService;
import org.lsposed.lspd.util.Hookers;
import org.lsposed.lspd.util.Utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;

// normal process initialization (for new Activity, Service, BroadcastReceiver etc.)
public class HandleBindAppHooker extends XC_MethodHook {
    private static final String CHROMIUM_WEBVIEW_FACTORY_METHOD = "create";

    String appDataDir;
    PackageInfo managerPkgInfo = null;

    public HandleBindAppHooker(String appDataDir) {
        this.appDataDir = appDataDir;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        try {
            Hookers.logD("ActivityThread#handleBindApplication() starts");
            ActivityThread activityThread = (ActivityThread) param.thisObject;
            Object bindData = param.args[0];
            final ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(bindData, "appInfo");
            // save app process name here for later use
            String appProcessName = (String) XposedHelpers.getObjectField(bindData, "processName");
            String reportedPackageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;

            IBinder managerBinder = null;

            if (reportedPackageName.equals(ActivityController.MANAGER_INJECTED_PKG_NAME)) {
                List<IBinder> binder = new ArrayList<>(1);
                var managerFd = serviceClient.requestInjectedManagerBinder(binder);
                if (binder.size() > 0 && binder.get(0) != null && managerFd != null) {
                    Context ctx = ActivityThread.currentActivityThread().getSystemContext();
                    var sourceDir = "/proc/self/fd/" + managerFd.detachFd();
                    managerPkgInfo = ctx.getPackageManager().getPackageArchiveInfo(sourceDir, PackageManager.GET_ACTIVITIES);
                    managerBinder = binder.get(0);
                    var newAppInfo = managerPkgInfo.applicationInfo;
                    newAppInfo.sourceDir = sourceDir;
                    newAppInfo.nativeLibraryDir = appInfo.nativeLibraryDir;
                    newAppInfo.packageName = reportedPackageName;
                    newAppInfo.dataDir = appInfo.dataDir;
                    newAppInfo.uid = appInfo.uid;
                    XposedHelpers.setObjectField(bindData, "appInfo", newAppInfo);
                    XposedHelpers.setObjectField(bindData, "providers", new ArrayList<>());
                    Utils.logE("injected manager");
                } else {
                    Utils.logE("failed to inject manager");
                }
            }

            // Note: packageName="android" -> system_server process, ams pms etc;
            //       packageName="system"  -> android pkg, system dialogues.
            Utils.logD("processName=" + appProcessName +
                    ", packageName=" + reportedPackageName + ", appDataDir=" + appDataDir);

            CompatibilityInfo compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(bindData, "compatInfo");
            if (appInfo.sourceDir == null) {
                return;
            }
            XposedHelpers.setObjectField(activityThread, "mBoundApplication", bindData);
            XposedInit.loadedPackagesInProcess.add(reportedPackageName);
            LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);

            XResources.setPackageNameForResDir(appInfo.packageName, loadedApk.getResDir());

            String processName = (String) XposedHelpers.getObjectField(bindData, "processName");

            LoadedApkGetCLHooker hook = new LoadedApkGetCLHooker(loadedApk, reportedPackageName,
                    processName, true, managerBinder);
            hook.setUnhook(XposedHelpers.findAndHookMethod(
                    LoadedApk.class, "getClassLoader", hook));

        } catch (Throwable t) {
            Hookers.logE("error when hooking bindApp", t);
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (managerPkgInfo == null) return;
        try {
            var hooker = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    for (var i = 0; i < param.args.length; ++i) {
                        if (param.args[i] instanceof ActivityInfo) {
                            for (var activity : managerPkgInfo.activities) {
                                if ("org.lsposed.manager.ui.activity.MainActivity".equals(activity.name)) {
                                    param.args[i] = activity;
                                }
                            }
                        }
                        if (param.args[i] instanceof Intent) {
                            ((Intent) param.args[i]).setComponent(ComponentName.unflattenFromString("org.lsposed.manager/.ui.activity.MainActivity"));
                        }
                    }
                }
            };
            XposedBridge.hookAllConstructors(ActivityThread.ActivityClientRecord.class, hooker);

            XposedHelpers.findAndHookMethod(WebViewFactory.class, "getProvider", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    var sProviderInstance = XposedHelpers.getStaticObjectField(WebViewFactory.class, "sProviderInstance");
                    if (sProviderInstance != null) return sProviderInstance;
                    //noinspection unchecked
                    var providerClass = (Class<WebViewFactoryProvider>) XposedHelpers.callStaticMethod(WebViewFactory.class, "getProviderClass");
                    Method staticFactory = null;
                    try {
                        staticFactory = providerClass.getMethod(
                                CHROMIUM_WEBVIEW_FACTORY_METHOD, WebViewDelegate.class);
                    } catch (Exception e) {
                        Hookers.logE("error instantiating provider with static factory method", e);
                    }

                    try {
                        var webViewDelegateConstructor = WebViewDelegate.class.getDeclaredConstructor();
                        webViewDelegateConstructor.setAccessible(true);
                        sProviderInstance = staticFactory.invoke(null, webViewDelegateConstructor.newInstance());
                        XposedHelpers.setStaticObjectField(WebViewFactory.class, "sProviderInstance", sProviderInstance);
                        Hookers.logD("Loaded provider: " + sProviderInstance);
                        return sProviderInstance;
                    } catch (Exception e) {
                        Hookers.logE("error instantiating provider", e);
                        throw new AndroidRuntimeException(e);
                    }
                }
            });

        } catch (Throwable e) {
            Hookers.logE("hook instrument", e);
        }
    }
}
