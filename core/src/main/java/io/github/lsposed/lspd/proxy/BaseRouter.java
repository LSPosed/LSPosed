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

package io.github.lsposed.lspd.proxy;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.text.TextUtils;

import io.github.lsposed.lspd.hooker.HandleBindAppHooker;
import io.github.lsposed.lspd.hooker.LoadedApkCstrHooker;
import io.github.lsposed.lspd.hooker.StartBootstrapServicesHooker;
import io.github.lsposed.lspd.hooker.SystemMainHooker;
import io.github.lsposed.lspd.util.Utils;
import io.github.lsposed.lspd.util.Versions;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

public abstract class BaseRouter implements Router {

    protected volatile AtomicBoolean bootstrapHooked = new AtomicBoolean(false);

    public void initResourcesHook() {
        XposedBridge.initXResources();
    }

    public void prepare(boolean isSystem) {
        // this flag is needed when loadModules
        XposedInit.startsSystemServer = isSystem;
    }

    public void installBootstrapHooks(boolean isSystem, String appDataDir) {
        // Initialize the Xposed framework
        try {
            if (!bootstrapHooked.compareAndSet(false, true)) {
                return;
            }
            startBootstrapHook(isSystem, appDataDir);
            XposedInit.initForZygote(isSystem);
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
            XposedBridge.disableHooks = true;
        }
    }

    public void loadModulesSafely(boolean callInitZygote) {
        try {
            XposedInit.loadModules(callInitZygote);
        } catch (Exception exception) {
            Utils.logE("error loading module list", exception);
        }
    }

    public String parsePackageName(String appDataDir) {
        if (TextUtils.isEmpty(appDataDir)) {
            return "";
        }
        int lastIndex = appDataDir.lastIndexOf("/");
        if (lastIndex < 1) {
            return "";
        }
        return appDataDir.substring(lastIndex + 1);
    }


    @ApiSensitive(Level.LOW)
    public void startBootstrapHook(boolean isSystem, String appDataDir) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = BaseRouter.class.getClassLoader();
        if (isSystem) {
            XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                    "systemMain", new SystemMainHooker());
        }
        XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                "handleBindApplication",
                "android.app.ActivityThread$AppBindData",
                new HandleBindAppHooker(appDataDir));
        XposedHelpers.findAndHookConstructor("android.app.LoadedApk", classLoader,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class,
                new LoadedApkCstrHooker());
    }

    public void startSystemServerHook() {
        StartBootstrapServicesHooker sbsHooker = new StartBootstrapServicesHooker();
        Object[] paramTypesAndCallback = Versions.hasR() ?
                new Object[]{"com.android.server.utils.TimingsTraceAndSlog", sbsHooker} :
                new Object[]{sbsHooker};
        XposedHelpers.findAndHookMethod("com.android.server.SystemServer",
                SystemMainHooker.systemServerCL,
                "startBootstrapServices", paramTypesAndCallback);
    }
}
