package org.lsposed.lspd.hooker;

import android.os.Build;

import org.lsposed.lspd.impl.LSPosedBridge;
import org.lsposed.lspd.nativebridge.HookBridge;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public class OpenDexFileHooker implements XposedInterface.Hooker {

    @AfterInvocation
    public static void afterHookedMethod(XposedInterface.AfterHookCallback callback) {
        ClassLoader classLoader = null;
        for (var arg : callback.getArgs()) {
            if (arg instanceof ClassLoader) {
                classLoader = (ClassLoader) arg;
            }
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && classLoader == null) {
            classLoader = LSPosedBridge.class.getClassLoader();
        }
        while (classLoader != null) {
            if (classLoader == LSPosedBridge.class.getClassLoader()) {
                HookBridge.setTrusted(callback.getResult());
                return;
            } else {
                classLoader = classLoader.getParent();
            }
        }
    }
}
