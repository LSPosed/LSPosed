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

package io.github.lsposed.lspd._hooker.impl;

import android.app.AndroidAppHelper;
import android.app.LoadedApk;
import android.content.res.XResources;
import android.util.Log;

import io.github.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;

// when a package is loaded for an existing process, trigger the callbacks as well
// ed: remove resources related hooking
public class LoadedApkCstr extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Hookers.logD("LoadedApk#<init> starts");

        try {
            LoadedApk loadedApk = (LoadedApk) param.thisObject;
            String packageName = loadedApk.getPackageName();
            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            Hookers.logD("LoadedApk#<init> ends: " + mAppDir);

            XResources.setPackageNameForResDir(packageName, loadedApk.getResDir());

            if (packageName.equals("android")) {
                Hookers.logD("LoadedApk#<init> is android, skip: " + mAppDir);
                return;
            }

            // mIncludeCode checking should go ahead of loadedPackagesInProcess added checking
            if (!XposedHelpers.getBooleanField(loadedApk, "mIncludeCode")) {
                Hookers.logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            if (!XposedInit.loadedPackagesInProcess.add(packageName)) {
                Hookers.logD("LoadedApk#<init> has been loaded before, skip: " + mAppDir);
                return;
            }

            // OnePlus magic...
            if (Log.getStackTraceString(new Throwable()).
                    contains("android.app.ActivityThread$ApplicationThread.schedulePreload")) {
                Hookers.logD("LoadedApk#<init> maybe oneplus's custom opt, skip");
                return;
            }

            LoadedApkGetCL hook = new LoadedApkGetCL(loadedApk, packageName,
                    AndroidAppHelper.currentProcessName(), false);
            hook.setUnhook(XposedHelpers.findAndHookMethod(
                    LoadedApk.class, "getClassLoader", hook));

        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk.<init>", t);
        }
    }
}
