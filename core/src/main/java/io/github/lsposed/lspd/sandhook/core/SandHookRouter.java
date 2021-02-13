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

package io.github.lsposed.lspd.sandhook.core;

import io.github.lsposed.lspd.config.LSPdConfigGlobal;
import io.github.lsposed.lspd.proxy.BaseRouter;
import io.github.lsposed.lspd.sandhook.config.SandHookProvider;
import io.github.lsposed.lspd.sandhook.entry.AppBootstrapHookInfo;
import io.github.lsposed.lspd.sandhook.entry.SysBootstrapHookInfo;
import io.github.lsposed.lspd.sandhook.entry.SysInnerHookInfo;
import io.github.lsposed.lspd.sandhook.hooker.SystemMainHooker;
import io.github.lsposed.lspd.util.Utils;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import de.robv.android.xposed.XposedBridge;

public class SandHookRouter extends BaseRouter {

    public SandHookRouter() {
    }

    private static boolean useSandHook = false;

    public void startBootstrapHook(boolean isSystem) {
        if (useSandHook) {
            Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
            ClassLoader classLoader = XposedBridge.BOOTCLASSLOADER;
            if (isSystem) {
                XposedCompat.addHookers(classLoader, SysBootstrapHookInfo.hookItems);
            } else {
                XposedCompat.addHookers(classLoader, AppBootstrapHookInfo.hookItems);
            }
        } else {
            super.startBootstrapHook(isSystem);
        }
    }

    public void startSystemServerHook() {
        if (useSandHook) {
            XposedCompat.addHookers(SystemMainHooker.systemServerCL, SysInnerHookInfo.hookItems);
        } else {
            super.startSystemServerHook();
        }
    }

    public void onEnterChildProcess() {
        SandHookXposedBridge.onForkPost();
        //enable compile in child process
        //SandHook.enableCompiler(!XposedInit.startsSystemServer);
    }

    public void injectConfig() {
        LSPdConfigGlobal.sHookProvider = new SandHookProvider();
    }

}
