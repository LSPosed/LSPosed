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

import android.app.ActivityThread;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd._hooker.impl.SystemMain;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;


@ApiSensitive(Level.LOW)
// system_server initialization
// ed: only support sdk >= 21 for now
@HookClass(ActivityThread.class)
public class SystemMainHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "systemMain";
    public static String methodSig = "()Landroid/app/ActivityThread;";

    public static ClassLoader systemServerCL;

    @HookMethodBackup("systemMain")
    static Method backup;

    @HookMethod("systemMain")
    public static ActivityThread hook() throws Throwable {
        final XC_MethodHook methodHook = new SystemMain();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = null;
        param.args = new Object[]{};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            param.setResult(backup());
        }
        methodHook.callAfterHookedMethod(param);
        return (ActivityThread) param.getResult();
    }

    public static ActivityThread backup() throws Throwable {
        return (ActivityThread) SandHook.callOriginByBackup(backup, null);
    }
}