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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package org.lsposed.lspd.core;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.android.internal.os.ZygoteInit;

import org.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import org.lsposed.lspd.hooker.AttachHooker;
import org.lsposed.lspd.hooker.CrashDumpHooker;
import org.lsposed.lspd.hooker.HandleSystemServerProcessHooker;
import org.lsposed.lspd.hooker.LoadedApkCtorHooker;
import org.lsposed.lspd.hooker.LoadedApkCreateCLHooker;
import org.lsposed.lspd.hooker.OpenDexFileHooker;
import org.lsposed.lspd.impl.LSPosedContext;
import org.lsposed.lspd.impl.LSPosedHelper;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.util.Utils;

import java.util.List;

import dalvik.system.DexFile;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;

public class Startup {
    private static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        LSPosedHelper.hookMethod(CrashDumpHooker.class, Thread.class, "dispatchUncaughtException", Throwable.class);
        if (isSystem) {
            LSPosedHelper.hookAllMethods(HandleSystemServerProcessHooker.class, ZygoteInit.class, "handleSystemServerProcess");
        } else {
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openDexFile");
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFile");
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFiles");
        }
        LSPosedHelper.hookConstructor(LoadedApkCtorHooker.class, LoadedApk.class,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class);
        LSPosedHelper.hookMethod(LoadedApkCreateCLHooker.class, LoadedApk.class, "createOrUpdateClassLoaderLocked", List.class);
        LSPosedHelper.hookAllMethods(AttachHooker.class, ActivityThread.class, "attach");
    }

    public static void bootstrapXposed() {
        // Initialize the Xposed framework
        try {
            startBootstrapHook(XposedInit.startsSystemServer);
            XposedInit.loadLegacyModules();
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
        }
    }

    public static void initXposed(boolean isSystem, String processName, String appDir, ILSPApplicationService service) {
        // init logger
        ApplicationServiceClient.Init(service, processName);
        XposedBridge.initXResources();
        XposedInit.startsSystemServer = isSystem;
        LSPosedContext.isSystemServer = isSystem;
        LSPosedContext.appDir = appDir;
        LSPosedContext.processName = processName;
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
    }
}
