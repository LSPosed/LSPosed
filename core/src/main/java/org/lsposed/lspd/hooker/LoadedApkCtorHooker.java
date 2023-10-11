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

import android.app.LoadedApk;
import android.content.res.XResources;
import android.util.Log;

import org.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

// when a package is loaded for an existing process, trigger the callbacks as well
@XposedHooker
public class LoadedApkCtorHooker implements XposedInterface.Hooker {

    @AfterInvocation
    public static void afterHookedMethod(XposedInterface.AfterHookCallback callback) {
        Hookers.logD("LoadedApk#<init> starts");

        try {
            LoadedApk loadedApk = (LoadedApk) callback.getThisObject();
            assert loadedApk != null;
            String packageName = loadedApk.getPackageName();
            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            Hookers.logD("LoadedApk#<init> ends: " + mAppDir);

            if (!XposedInit.disableResources) {
                XResources.setPackageNameForResDir(packageName, loadedApk.getResDir());
            }

            if (packageName.equals("android")) {
                if (XposedInit.startsSystemServer) {
                    Hookers.logD("LoadedApk#<init> is android, skip: " + mAppDir);
                    return;
                } else {
                    packageName = "system";
                }
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

            LoadedApkCreateCLHooker.addLoadedApk(loadedApk);
        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk.<init>", t);
        }
    }
}
