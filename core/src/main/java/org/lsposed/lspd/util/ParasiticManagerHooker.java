package org.lsposed.lspd.util;

import static org.lsposed.lspd.config.ApplicationServiceClient.serviceClient;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.webkit.WebViewDelegate;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.ILSPManagerService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import hidden.HiddenApiBridge;

public class ParasiticManagerHooker {
    private static final String CHROMIUM_WEBVIEW_FACTORY_METHOD = "create";

    private static PackageInfo managerPkgInfo = null;
    private static int managerFd = -1;

    private synchronized static PackageInfo getManagerPkgInfo(ApplicationInfo appInfo) {
        if (managerPkgInfo == null) {
            Context ctx = ActivityThread.currentActivityThread().getSystemContext();
            var sourceDir = "/proc/self/fd/" + managerFd;
            managerPkgInfo = ctx.getPackageManager().getPackageArchiveInfo(sourceDir, PackageManager.GET_ACTIVITIES);
            var newAppInfo = managerPkgInfo.applicationInfo;
            newAppInfo.sourceDir = sourceDir;
            newAppInfo.publicSourceDir = sourceDir;
            newAppInfo.nativeLibraryDir = appInfo.nativeLibraryDir;
            newAppInfo.packageName = appInfo.packageName;
            newAppInfo.dataDir = HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(appInfo);
            newAppInfo.deviceProtectedDataDir = appInfo.deviceProtectedDataDir;
            newAppInfo.processName = appInfo.processName;
            HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(newAppInfo, HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(appInfo));
            newAppInfo.uid = appInfo.uid;
        }
        return managerPkgInfo;
    }

    private static void hookForManager(ILSPManagerService managerService) {
        var activityHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                for (var i = 0; i < param.args.length; ++i) {
                    if (param.args[i] instanceof ActivityInfo) {
                        var pkgInfo = getManagerPkgInfo(((ActivityInfo) param.args[i]).applicationInfo);
                        for (var activity : pkgInfo.activities) {
                            if ("org.lsposed.manager.ui.activity.MainActivity".equals(activity.name)) {
                                activity.applicationInfo = pkgInfo.applicationInfo;
                                param.args[i] = activity;
                            }
                        }
                    }
                    if (param.args[i] instanceof Intent) {
                        var intent = (Intent) param.args[i];
                        checkIntent(managerService, intent);
                        intent.setComponent(new ComponentName(BuildConfig.MANAGER_INJECTED_PKG_NAME, "org.lsposed.manager.ui.activity.MainActivity"));
                    }
                }
            }
        };
        var managerApkHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Hookers.logD("ActivityThread#handleBindApplication() starts");
                Object bindData = param.args[0];
                ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(bindData, "appInfo");
                XposedHelpers.setObjectField(bindData, "appInfo", getManagerPkgInfo(appInfo).applicationInfo);
            }
        };
        XposedHelpers.findAndHookMethod(ActivityThread.class,
                "handleBindApplication",
                "android.app.ActivityThread$AppBindData",
                managerApkHooker);

        XposedBridge.hookAllConstructors(ActivityThread.ActivityClientRecord.class, activityHooker);

        var unhooks = new XC_MethodHook.Unhook[]{null};
        unhooks[0] = XposedHelpers.findAndHookMethod(
                LoadedApk.class, "getClassLoader", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (XposedHelpers.getObjectField(param.thisObject, "mApplicationInfo") == getManagerPkgInfo(null).applicationInfo) {
                            InstallerVerifier.sendBinderToManager((ClassLoader) param.getResult(), managerService.asBinder());
                            unhooks[0].unhook();
                        }
                    }
                });

        XposedBridge.hookAllMethods(ActivityThread.class, "handleReceiver", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                for (var arg : param.args) {
                    if (arg instanceof BroadcastReceiver.PendingResult) {
                        ((BroadcastReceiver.PendingResult) arg).finish();
                    }
                }
                return null;
            }
        });
        XposedBridge.hookAllMethods(ActivityThread.class, "installProvider", new XC_MethodHook() {
            private Context originalContext = null;
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Hookers.logD("before install provider");
                Context ctx = null;
                ProviderInfo info = null;
                int ctxIdx = -1;
                for (var i = 0; i < param.args.length; ++i) {
                    var arg = param.args[i];
                    if (arg instanceof Context) {
                        ctx = (Context) arg;
                        ctxIdx = i;
                    } else if (arg instanceof ProviderInfo) info = (ProviderInfo) arg;
                }
                if (ctx != null && info != null) {
                    if (originalContext == null) {
                        info.applicationInfo.packageName = BuildConfig.MANAGER_INJECTED_PKG_NAME + ".origin";
                        var originalPkgInfo = ActivityThread.currentActivityThread().getPackageInfoNoCheck(info.applicationInfo, HiddenApiBridge.Resources_getCompatibilityInfo(ctx.getResources()));
                        XposedHelpers.setObjectField(originalPkgInfo, "mPackageName", BuildConfig.MANAGER_INJECTED_PKG_NAME);
                        originalContext = (Context) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ContextImpl", null), "createAppContext", ActivityThread.currentActivityThread(), originalPkgInfo);
                    }
                    param.args[ctxIdx] = originalContext;
                } else {
                    Hookers.logE("Failed to reload provider", new RuntimeException());
                }
            }
        });

        XposedHelpers.findAndHookMethod(ActivityThread.class, "deliverNewIntents", ActivityThread.ActivityClientRecord.class, List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[1] == null) return;
                for (var intent : (List<Intent>) param.args[1]) {
                    checkIntent(managerService, intent);
                }
            }
        });

        if (Process.myUid() == BuildConfig.MANAGER_INJECTED_UID) {
            XposedHelpers.findAndHookMethod(WebViewFactory.class, "getProvider", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
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
        }
    }

    private static void checkIntent(ILSPManagerService managerService, Intent intent) {
        if (managerService == null) return;
        if (intent.getCategories() == null || !intent.getCategories().contains("org.lsposed.manager.LAUNCH_MANAGER")) {
            Hookers.logD("Launching the original app, restarting");
            try {
                managerService.restartFor(intent);
            } catch (RemoteException e) {
                Hookers.logE("restart failed", e);
            } finally {
                Process.killProcess(Process.myPid());
            }
        }
    }


    static public boolean start() {
        try {
            List<IBinder> binder = new ArrayList<>(1);
            var managerParcelFd = serviceClient.requestInjectedManagerBinder(binder);
            if (binder.size() > 0 && binder.get(0) != null && managerParcelFd != null) {
                managerFd = managerParcelFd.detachFd();
                var managerService = ILSPManagerService.Stub.asInterface(binder.get(0));
                hookForManager(managerService);
                Utils.logD("injected manager");
                return true;
            } else {
                // Not manager
                return false;
            }
        } catch (Throwable e) {
            Utils.logE("failed to inject manager", e);
            return false;
        }
    }
}
