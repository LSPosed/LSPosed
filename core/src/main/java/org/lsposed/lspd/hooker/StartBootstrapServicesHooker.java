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

import org.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static org.lsposed.lspd.util.Utils.logD;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class StartBootstrapServicesHooker extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        logD("SystemServer#startBootstrapServices() starts");

        try {
            XposedInit.loadedPackagesInProcess.add("android");

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = "android";
            lpparam.processName = "android"; // it's actually system_server, but other functions return this as well
            lpparam.classLoader = SystemMainHooker.systemServerCL;
            lpparam.appInfo = null;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            // Huawei
            try {
                findAndHookMethod("com.android.server.pm.HwPackageManagerService",
                        SystemMainHooker.systemServerCL, "isOdexMode",
                        XC_MethodReplacement.returnConstant(false));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }

            try {
                String className = "com.android.server.pm.PackageDexOptimizer";
                findAndHookMethod(className, SystemMainHooker.systemServerCL,
                        "dexEntryExists", String.class,
                        XC_MethodReplacement.returnConstant(true));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }
        } catch (Throwable t) {
            Hookers.logE("error when hooking startBootstrapServices", t);
        }
    }
}
