package com.elderdrivers.riru.xposed.entry.hooker;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.LoadedApk;

import com.elderdrivers.riru.common.KeepMembers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.xposed.util.ClassLoaderUtils.replaceParentClassLoader;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedInit.INSTALLER_PACKAGE_NAME;
import static de.robv.android.xposed.XposedInit.loadedPackagesInProcess;
import static de.robv.android.xposed.XposedInit.logD;
import static de.robv.android.xposed.XposedInit.logE;


public class InstrumentationHooker {

    public static class CallAppOnCreate implements KeepMembers {

        public static String className = "android.app.Instrumentation";
        public static String methodName = "callApplicationOnCreate";
        public static String methodSig = "(Landroid/app/Application;)V";

        public static void hook(Object thiz, Application application) {
            try {
                logD("Instrumentation#callApplicationOnCreate starts");
                LoadedApk loadedApk = (LoadedApk) getObjectField(application, "mLoadedApk");
                String reportedPackageName = application.getPackageName();
                loadedPackagesInProcess.add(reportedPackageName);

                replaceParentClassLoader(loadedApk.getClassLoader());

                XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
                lpparam.packageName = reportedPackageName;
                lpparam.processName = AndroidAppHelper.currentProcessName();
                lpparam.classLoader = loadedApk.getClassLoader();
                lpparam.appInfo = application.getApplicationInfo();
                lpparam.isFirstApplication = true;
                XC_LoadPackage.callAll(lpparam);

                if (reportedPackageName.equals(INSTALLER_PACKAGE_NAME)) {
                    XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader);
                }
            } catch (Throwable throwable) {
                logE("error when hooking Instru#callAppOnCreate", throwable);
            }
            backup(thiz, application);
        }

        public static void backup(Object thiz, Application application) {

        }

    }
}
