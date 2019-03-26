package com.elderdrivers.riru.edxp.whale.entry.hooker;

import android.os.Build;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale.entry.Router;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.edxp.util.ClassLoaderUtils.replaceParentClassLoader;
import static com.elderdrivers.riru.edxp.util.Utils.logD;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class StartBootstrapServicesHooker implements KeepMembers {
    public static String className = "com.android.server.SystemServer";
    public static String methodName = "startBootstrapServices";
    public static String methodSig = "()V";

    public static void hook(Object systemServer) {

        if (XposedBridge.disableHooks) {
            backup(systemServer);
            return;
        }

        logD("SystemServer#startBootstrapServices() starts");

        try {
            XposedInit.loadedPackagesInProcess.add("android");

            replaceParentClassLoader(SystemMainHooker.systemServerCL);

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = "android";
            lpparam.processName = "android"; // it's actually system_server, but other functions return this as well
            lpparam.classLoader = SystemMainHooker.systemServerCL;
            lpparam.appInfo = null;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            // Huawei
            try {
                findAndHookMethod("com.android.server.pm.HwPackageManagerService", SystemMainHooker.systemServerCL, "isOdexMode", XC_MethodReplacement.returnConstant(false));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }

            try {
                String className = "com.android.server.pm." + (Build.VERSION.SDK_INT >= 23 ? "PackageDexOptimizer" : "PackageManagerService");
                findAndHookMethod(className, SystemMainHooker.systemServerCL, "dexEntryExists", String.class, XC_MethodReplacement.returnConstant(true));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }
        } catch (Throwable t) {
            Router.logE("error when hooking startBootstrapServices", t);
        } finally {
            backup(systemServer);
        }
    }

    public static void backup(Object systemServer) {

    }
}
