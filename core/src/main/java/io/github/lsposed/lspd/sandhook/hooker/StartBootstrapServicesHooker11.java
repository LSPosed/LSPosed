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

package io.github.lsposed.lspd.sandhook.hooker;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd.hooker.StartBootstrapServicesHooker;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.HookReflectClass;
import com.swift.sandhook.annotation.Param;
import com.swift.sandhook.annotation.SkipParamCheck;
import com.swift.sandhook.annotation.ThisObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
@HookReflectClass("com.android.server.SystemServer")
public class StartBootstrapServicesHooker11 implements KeepMembers {
    public static String className = "com.android.server.SystemServer";
    public static String methodName = "startBootstrapServices";
    public static String methodSig = "(Lcom/android/server/utils/TimingsTraceAndSlog;)V";

    @HookMethodBackup("startBootstrapServices")
    @SkipParamCheck
    static Method backup;

    @HookMethod("startBootstrapServices")
    public static void hook(@ThisObject Object systemServer, @Param("com.android.server.utils.TimingsTraceAndSlog") Object traceAndSlog) throws Throwable {
        final XC_MethodHook methodHook = new StartBootstrapServicesHooker();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = systemServer;
        param.args = new Object[]{traceAndSlog};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(systemServer, traceAndSlog);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object systemServer, Object traceAndSlog) throws Throwable {
        SandHook.callOriginByBackup(backup, systemServer, traceAndSlog);
    }
}
