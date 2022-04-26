package org.lsposed.lspd.hooker;

import android.os.Build;

import org.lsposed.lspd.nativebridge.HookBridge;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class OpenDexFileHooker extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        ClassLoader classLoader = null;
        for (var arg : param.args) {
            if (arg instanceof ClassLoader) {
                classLoader = (ClassLoader) arg;
            }
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && classLoader == null) {
            classLoader = XposedHelpers.class.getClassLoader();
        }
        while (classLoader != null) {
            if (classLoader == XposedHelpers.class.getClassLoader()) {
                HookBridge.setTrusted(param.getResult());
            } else {
                classLoader = classLoader.getParent();
            }
        }
    }
}
