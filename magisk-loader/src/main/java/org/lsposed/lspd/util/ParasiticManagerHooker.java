package org.lsposed.lspd.util;

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;

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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.webkit.WebViewDelegate;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.ILSPManagerService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import hidden.HiddenApiBridge;

public class ParasiticManagerHooker {
    private static final String CHROMIUM_WEBVIEW_FACTORY_METHOD = "create";

    private static PackageInfo managerPkgInfo = null;
    private static int managerFd = -1;
    private final static Map<String, Bundle> states = new ConcurrentHashMap<>();
    private final static Map<String, PersistableBundle> persistentStates = new ConcurrentHashMap<>();

    private synchronized static PackageInfo getManagerPkgInfo(ApplicationInfo appInfo) {
        if (managerPkgInfo == null && appInfo != null) {
            try {
                Context ctx = ActivityThread.currentActivityThread().getSystemContext();
                var sourceDir = "/proc/self/fd/" + managerFd;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    var dstDir = appInfo.dataDir + "/cache/lsposed.apk";
                    try (var inStream = new FileInputStream(sourceDir); var outStream = new FileOutputStream(dstDir)) {
                        FileChannel inChannel = inStream.getChannel();
                        FileChannel outChannel = outStream.getChannel();
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                        sourceDir = dstDir;
                    } catch (Throwable e) {
                        Hookers.logE("copy apk", e);
                    }
                }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HiddenApiBridge.ApplicationInfo_overlayPaths(newAppInfo, HiddenApiBridge.ApplicationInfo_overlayPaths(appInfo));
                }
                HiddenApiBridge.ApplicationInfo_resourceDirs(newAppInfo, HiddenApiBridge.ApplicationInfo_resourceDirs(appInfo));
                newAppInfo.uid = appInfo.uid;
            } catch (Throwable e) {
                Utils.logE("get manager pkginfo", e);
            }
        }
        return managerPkgInfo;
    }

    private static void sendBinderToManager(final ClassLoader classLoader, IBinder binder) {
        try {
            var clazz = XposedHelpers.findClass("org.lsposed.manager.Constants", classLoader);
            var ok = (boolean) XposedHelpers.callStaticMethod(clazz, "setBinder",
                    new Class[]{IBinder.class}, binder);
            if (ok) return;
            throw new RuntimeException("setBinder: " + false);
        } catch (Throwable t) {
            Utils.logW("Could not send binder to LSPosed Manager", t);
        }
    }

    private static void hookForManager(ILSPManagerService managerService) {
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

        var unhooks = new XC_MethodHook.Unhook[]{null};
        unhooks[0] = XposedHelpers.findAndHookMethod(
                LoadedApk.class, "getClassLoader", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        var pkgInfo = getManagerPkgInfo(null);
                        if (pkgInfo != null && XposedHelpers.getObjectField(param.thisObject, "mApplicationInfo") == pkgInfo.applicationInfo) {
                            sendBinderToManager((ClassLoader) param.getResult(), managerService.asBinder());
                            unhooks[0].unhook();
                        }
                    }
                });

        var activityClientRecordClass = XposedHelpers.findClass("android.app.ActivityThread$ActivityClientRecord", ActivityThread.class.getClassLoader());
        var activityHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                for (var i = 0; i < param.args.length; ++i) {
                    if (param.args[i] instanceof ActivityInfo) {
                        var aInfo = (ActivityInfo) param.args[i];
                        var pkgInfo = getManagerPkgInfo(aInfo.applicationInfo);
                        if (pkgInfo == null) return;
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
                        intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), "org.lsposed.manager.ui.activity.MainActivity"));
                    }
                }
                if (param.method.getName().equals("scheduleLaunchActivity")) {
                    ActivityInfo aInfo = null;
                    var parameters = ((Method) param.method).getParameterTypes();
                    for (var i = 0; i < parameters.length; ++i) {
                        if (parameters[i] == ActivityInfo.class) {
                            aInfo = (ActivityInfo) param.args[i];
                            Hookers.logD("loading state of " + aInfo.name);
                        } else if (parameters[i] == Bundle.class && aInfo != null) {
                            final int idx = i;
                            states.computeIfPresent(aInfo.name, (k, v) -> {
                                param.args[idx] = v;
                                return v;
                            });
                        } else if (parameters[i] == PersistableBundle.class && aInfo != null) {
                            final int idx = i;
                            persistentStates.computeIfPresent(aInfo.name, (k, v) -> {
                                param.args[idx] = v;
                                return v;
                            });
                        }
                    }

                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (var i = 0; i < param.args.length && activityClientRecordClass.isInstance(param.thisObject); ++i) {
                    if (param.args[i] instanceof ActivityInfo) {
                        var aInfo = (ActivityInfo) param.args[i];
                        Hookers.logD("loading state of " + aInfo.name);
                        states.computeIfPresent(aInfo.name, (k, v) -> {
                            XposedHelpers.setObjectField(param.thisObject, "state", v);
                            return v;
                        });
                        persistentStates.computeIfPresent(aInfo.name, (k, v) -> {
                            XposedHelpers.setObjectField(param.thisObject, "persistentState", v);
                            return v;
                        });
                    }
                }
            }
        };
        XposedBridge.hookAllConstructors(activityClientRecordClass, activityHooker);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.ActivityThread$ApplicationThread", ActivityThread.class.getClassLoader()), "scheduleLaunchActivity", activityHooker);
        }

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
            protected void beforeHookedMethod(MethodHookParam param) {
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
                var pkgInfo = getManagerPkgInfo(null);
                if (ctx != null && info != null && pkgInfo != null) {
                    var packageName = pkgInfo.applicationInfo.packageName;
                    if (!info.applicationInfo.packageName.equals(packageName)) return;
                    if (originalContext == null) {
                        info.applicationInfo.packageName = packageName + ".origin";
                        var originalPkgInfo = ActivityThread.currentActivityThread().getPackageInfoNoCheck(info.applicationInfo, HiddenApiBridge.Resources_getCompatibilityInfo(ctx.getResources()));
                        XposedHelpers.setObjectField(originalPkgInfo, "mPackageName", packageName);
                        originalContext = (Context) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ContextImpl", null),
                                "createAppContext", ActivityThread.currentActivityThread(), originalPkgInfo);
                        info.applicationInfo.packageName = packageName;
                    }
                    param.args[ctxIdx] = originalContext;
                } else {
                    Hookers.logE("Failed to reload provider", new RuntimeException());
                }
            }
        });

        XposedHelpers.findAndHookMethod(ActivityThread.class, "deliverNewIntents", activityClientRecordClass, List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[1] == null) return;
                for (var intent : (List<?>) param.args[1]) {
                    checkIntent(managerService, (Intent) intent);
                }
            }
        });

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
                    if (staticFactory != null) {
                        sProviderInstance = staticFactory.invoke(null, webViewDelegateConstructor.newInstance());
                    }
                    XposedHelpers.setStaticObjectField(WebViewFactory.class, "sProviderInstance", sProviderInstance);
                    Hookers.logD("Loaded provider: " + sProviderInstance);
                    return sProviderInstance;
                } catch (Exception e) {
                    Hookers.logE("error instantiating provider", e);
                    throw new AndroidRuntimeException(e);
                }
            }
        });
        var stateHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    var record = param.args[0];
                    if (record instanceof IBinder) {
                        record = ((ArrayMap<?, ?>) XposedHelpers.getObjectField(param.thisObject, "mActivities")).get(record);
                        if (record == null) return;
                    }
                    XposedHelpers.callMethod(param.thisObject, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? "callActivityOnSaveInstanceState" : "callCallActivityOnSaveInstanceState", record);
                    var state = (Bundle) XposedHelpers.getObjectField(record, "state");
                    var persistentState = (PersistableBundle) XposedHelpers.getObjectField(record, "persistentState");
                    var aInfo = (ActivityInfo) XposedHelpers.getObjectField(record, "activityInfo");
                    states.compute(aInfo.name, (k, v) -> state);
                    persistentStates.compute(aInfo.name, (k, v) -> persistentState);
                    Hookers.logD("saving state of " + aInfo.name);
                } catch (Throwable e) {
                    Hookers.logE("save state", e);
                }
            }
        };
        XposedBridge.hookAllMethods(ActivityThread.class, "performStopActivityInner", stateHooker);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1)
            XposedHelpers.findAndHookMethod(ActivityThread.class, "performDestroyActivity", IBinder.class, boolean.class, int.class, boolean.class, stateHooker);
    }

    private static void checkIntent(ILSPManagerService managerService, Intent intent) {
        if (managerService == null) return;
        if (Process.myUid() != BuildConfig.MANAGER_INJECTED_UID) return;
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
        List<IBinder> binder = new ArrayList<>(1);
        try (var managerParcelFd = serviceClient.requestInjectedManagerBinder(binder)) {
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
