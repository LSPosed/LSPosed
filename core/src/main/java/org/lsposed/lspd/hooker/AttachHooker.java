package org.lsposed.lspd.hooker;

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;

import android.app.ActivityThread;

import org.lsposed.lspd.impl.LSPosedContext;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedInit;

public class AttachHooker extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        serviceClient.getModulesList().forEach(module -> {
            if (LSPosedContext.loadModule((ActivityThread) param.thisObject, module)) {
                XposedInit.getLoadedModules().add(module.packageName);
            }
        });
    }
}
