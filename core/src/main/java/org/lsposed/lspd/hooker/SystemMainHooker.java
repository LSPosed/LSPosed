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

import android.os.Build;

import org.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import org.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

// system_server initialization
public class SystemMainHooker extends XC_MethodHook {

    public static volatile ClassLoader systemServerCL;

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        Hookers.logD("ActivityThread#systemMain() starts");
        try {
            // get system_server classLoader
            systemServerCL = Thread.currentThread().getContextClassLoader();
            // deopt methods in SYSTEMSERVERCLASSPATH
            PrebuiltMethodsDeopter.deoptSystemServerMethods(systemServerCL);
            var sbsHooker = new StartBootstrapServicesHooker();
            Object[] paramTypesAndCallback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                    new Object[]{"com.android.server.utils.TimingsTraceAndSlog", sbsHooker} :
                    new Object[]{sbsHooker};
            XposedHelpers.findAndHookMethod("com.android.server.SystemServer",
                    systemServerCL, "startBootstrapServices", paramTypesAndCallback);
        } catch (Throwable t) {
            Hookers.logE("error when hooking systemMain", t);
        }
    }

}
