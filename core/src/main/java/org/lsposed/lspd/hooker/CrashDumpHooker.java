package org.lsposed.lspd.hooker;

import android.util.Log;

import org.lsposed.lspd.impl.LSPosedBridge;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public class CrashDumpHooker implements XposedInterface.Hooker {

    @BeforeInvocation
    public static void beforeHookedMethod(XposedInterface.BeforeHookCallback callback) {
        try {
            var e = (Throwable) callback.getArgs()[0];
            LSPosedBridge.log("Crash unexpectedly: " + Log.getStackTraceString(e));
        } catch (Throwable ignored) {
        }
    }
}
